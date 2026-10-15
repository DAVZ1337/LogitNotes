package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.example.model.Note
import com.example.model.NoteAttachment
import com.example.security.CryptoManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class VaultFileManager(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private val prefs = context.getSharedPreferences("vault_storage_prefs", Context.MODE_PRIVATE)

    fun getVaultLocationType(): String {
        return prefs.getString("vault_storage_type", "DOCUMENTS") ?: "DOCUMENTS"
    }

    fun getVaultTreeUri(): Uri? {
        val uriStr = prefs.getString("vault_tree_uri", null) ?: return null
        return try { Uri.parse(uriStr) } catch (_: Exception) { null }
    }

    fun getVaultStoragePath(): String {
        val type = getVaultLocationType()
        if (type == "SAF_TREE") {
            return prefs.getString("vault_friendly_path", null)
                ?: prefs.getString("vault_tree_uri", "Carpeta en Explorador de Archivos")!!
        }
        return vaultDir.absolutePath
    }

    fun getVaultLocationDescription(): String {
        val type = getVaultLocationType()
        val path = getVaultStoragePath()
        return when (type) {
            "SAF_TREE" -> "Carpeta de Explorador ($path)"
            "DOCUMENTS" -> "Carpeta Documentos ($path)"
            "DOWNLOADS" -> "Carpeta Descargas ($path)"
            "CUSTOM" -> "Ruta Personalizada ($path)"
            else -> "Almacenamiento Interno Protegido ($path)"
        }
    }

    fun setVaultTreeUri(treeUri: Uri, friendlyName: String? = null) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}

        val doc = DocumentFile.fromTreeUri(context, treeUri)
        val name = friendlyName ?: doc?.name ?: "Carpeta seleccionada"
        val friendlyPath = if (treeUri.path?.contains("primary:") == true) {
            val sub = treeUri.path?.substringAfter("primary:")?.substringBefore("/document") ?: ""
            "/storage/emulated/0/$sub"
        } else {
            "Explorador de Archivos ($name)"
        }

        prefs.edit()
            .putString("vault_storage_type", "SAF_TREE")
            .putString("vault_tree_uri", treeUri.toString())
            .putString("vault_friendly_path", friendlyPath)
            .apply()
    }

    fun setVaultLocation(type: String, customPath: String? = null) {
        val targetPath = when (type) {
            "DOCUMENTS" -> {
                val base = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "LogitNotes")
                val dir = try {
                    if (base.exists() || base.mkdirs()) base else (context.getExternalFilesDir("Documents/LogitNotes") ?: File(context.filesDir, "LogitNotes"))
                } catch (e: Exception) {
                    context.getExternalFilesDir("Documents/LogitNotes") ?: File(context.filesDir, "LogitNotes")
                }
                dir.apply { if (!exists()) mkdirs() }.absolutePath
            }
            "DOWNLOADS" -> {
                val base = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LogitNotes")
                val dir = try {
                    if (base.exists() || base.mkdirs()) base else (context.getExternalFilesDir("Download/LogitNotes") ?: File(context.filesDir, "LogitNotes"))
                } catch (e: Exception) {
                    context.getExternalFilesDir("Download/LogitNotes") ?: File(context.filesDir, "LogitNotes")
                }
                dir.apply { if (!exists()) mkdirs() }.absolutePath
            }
            "CUSTOM" -> {
                val path = customPath?.trim()?.ifBlank { null }
                    ?: (context.getExternalFilesDir("LogitNotes") ?: File(context.filesDir, "LogitNotes")).absolutePath
                val dir = File(path).apply {
                    try { if (!exists()) mkdirs() } catch (_: Exception) {}
                }
                dir.absolutePath
            }
            else -> {
                // "SANDBOX"
                val dir = File(context.filesDir, "LogitNotes").apply { if (!exists()) mkdirs() }
                dir.absolutePath
            }
        }

        prefs.edit()
            .putString("vault_storage_type", type)
            .putString("vault_storage_path", targetPath)
            .remove("vault_tree_uri")
            .apply()
    }

    val vaultDir: File
        get() {
            val defaultPath = try {
                val docDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "LogitNotes")
                if (docDir.exists() || docDir.mkdirs()) docDir.absolutePath else (context.getExternalFilesDir("Documents/LogitNotes") ?: File(context.filesDir, "LogitNotes")).absolutePath
            } catch (e: Exception) {
                File(context.filesDir, "LogitNotes").absolutePath
            }

            val savedPath = prefs.getString("vault_storage_path", defaultPath) ?: defaultPath
            val dir = File(savedPath)
            try {
                if (!dir.exists()) dir.mkdirs()
            } catch (e: Exception) {
                val fallback = File(context.filesDir, "LogitNotes").apply { if (!exists()) mkdirs() }
                return fallback
            }
            return dir
        }

    private val notesDir: File
        get() = vaultDir

    private val attachmentsDir: File
        get() = File(vaultDir, ".attachments").apply {
            if (!exists()) mkdirs()
        }

    private val exportDir: File
        get() = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }

    /**
     * Writes a human-readable Markdown (.md) file organized inside the vault folder.
     * Supports both direct File writing and Android SAF DocumentTree URI.
     */
    fun writeNoteMarkdownFile(
        id: String,
        title: String,
        content: String,
        folder: String,
        tags: List<String>,
        createdAt: Long,
        updatedAt: Long
    ): File? {
        return try {
            val targetFolder = if (folder.isNotBlank() && folder != "General") {
                File(vaultDir, sanitizeFolderName(folder)).apply { if (!exists()) mkdirs() }
            } else {
                vaultDir
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val createdStr = dateFormat.format(Date(createdAt))
            val updatedStr = dateFormat.format(Date(updatedAt))
            val tagsStr = if (tags.isNotEmpty()) tags.joinToString(", ", "[", "]") { "\"$it\"" } else "[]"

            val sanitizedTitle = sanitizeFileName(title.ifBlank { "nota_${id.take(6)}" })
            val fileName = "$sanitizedTitle.md"
            val newFile = File(targetFolder, fileName)

            // Remove old local file if title or folder changed
            val oldRelativePath = prefs.getString("note_md_path_$id", null)
            if (oldRelativePath != null) {
                val oldFile = File(vaultDir, oldRelativePath)
                if (oldFile.exists() && oldFile.absolutePath != newFile.absolutePath) {
                    oldFile.delete()
                }
            }

            val markdownText = buildString {
                appendLine("---")
                appendLine("title: \"${title.replace("\"", "\\\"")}\"")
                appendLine("folder: \"$folder\"")
                appendLine("tags: $tagsStr")
                appendLine("created: \"$createdStr\"")
                appendLine("updated: \"$updatedStr\"")
                appendLine("---")
                appendLine()
                if (!content.trimStart().startsWith("# ")) {
                    appendLine("# $title")
                    appendLine()
                }
                appendLine(content)
            }

            // 1. Write to local file
            newFile.writeText(markdownText, Charsets.UTF_8)
            val relativePath = newFile.relativeTo(vaultDir).path
            prefs.edit().putString("note_md_path_$id", relativePath).apply()

            // 2. If user selected a custom SAF tree folder via File Explorer, sync into it as well!
            val treeUri = getVaultTreeUri()
            if (treeUri != null) {
                try {
                    val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                    if (rootDoc != null && rootDoc.canWrite()) {
                        val targetDir = if (folder.isNotBlank() && folder != "General") {
                            rootDoc.findFile(sanitizeFolderName(folder))
                                ?: rootDoc.createDirectory(sanitizeFolderName(folder))
                                ?: rootDoc
                        } else {
                            rootDoc
                        }
                        val existingDoc = targetDir.findFile(fileName)
                        val noteDoc = existingDoc ?: targetDir.createFile("text/markdown", fileName)
                        if (noteDoc != null) {
                            context.contentResolver.openOutputStream(noteDoc.uri, "wt")?.use { os ->
                                os.write(markdownText.toByteArray(Charsets.UTF_8))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            newFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes the Markdown file corresponding to the note across all folders and SAF storage
     */
    fun deleteNoteMarkdownFile(id: String, folder: String? = null, title: String? = null) {
        try {
            // 1. Delete by saved relative path
            val relativePath = prefs.getString("note_md_path_$id", null)
            if (relativePath != null) {
                val file = File(vaultDir, relativePath)
                if (file.exists()) {
                    file.delete()
                }
                prefs.edit().remove("note_md_path_$id").apply()
            }

            // 2. Check in specific folder directory if given
            if (folder != null) {
                val folderDir = File(vaultDir, sanitizeFolderName(folder))
                if (folderDir.exists()) {
                    if (title != null) {
                        val titleFile = File(folderDir, "${sanitizeFileName(title)}.md")
                        if (titleFile.exists()) titleFile.delete()
                    }
                    val idFile = File(folderDir, "nota_${id.take(6)}.md")
                    if (idFile.exists()) idFile.delete()
                }
            }

            // 3. Check in root vaultDir
            if (title != null) {
                val rootTitle = File(vaultDir, "${sanitizeFileName(title)}.md")
                if (rootTitle.exists()) rootTitle.delete()
            }
            val rootId = File(vaultDir, "nota_${id.take(6)}.md")
            if (rootId.exists()) rootId.delete()

            // 4. Scan all subdirectories in vaultDir in case it was in another folder or imported
            vaultDir.listFiles()?.forEach { sub ->
                if (sub.isDirectory && sub.name != ".encrypted" && sub.name != ".attachments") {
                    if (title != null) {
                        val titleFile = File(sub, "${sanitizeFileName(title)}.md")
                        if (titleFile.exists()) titleFile.delete()
                    }
                    val idFile = File(sub, "nota_${id.take(6)}.md")
                    if (idFile.exists()) idFile.delete()
                }
            }

            // 5. Delete from external SAF Tree if configured
            val treeUri = getVaultTreeUri()
            if (treeUri != null) {
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                val fileName = relativePath?.substringAfterLast('/')
                    ?: (title?.let { "${sanitizeFileName(it)}.md" } ?: "nota_${id.take(6)}.md")
                if (rootDoc != null && rootDoc.canWrite()) {
                    rootDoc.findFile(fileName)?.delete()
                    if (title != null) rootDoc.findFile("${sanitizeFileName(title)}.md")?.delete()
                    rootDoc.findFile("nota_${id.take(6)}.md")?.delete()

                    if (folder != null) {
                        val subDoc = rootDoc.findFile(sanitizeFolderName(folder))
                        if (subDoc != null && subDoc.isDirectory) {
                            subDoc.findFile(fileName)?.delete()
                            if (title != null) subDoc.findFile("${sanitizeFileName(title)}.md")?.delete()
                            subDoc.findFile("nota_${id.take(6)}.md")?.delete()
                        }
                    }

                    // Check all subdirectories
                    rootDoc.listFiles().forEach { sub ->
                        if (sub.isDirectory) {
                            sub.findFile(fileName)?.delete()
                            if (title != null) sub.findFile("${sanitizeFileName(title)}.md")?.delete()
                            sub.findFile("nota_${id.take(6)}.md")?.delete()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Renames a folder in the storage directory and updates all note references
     */
    fun renameFolder(oldFolder: String, newFolder: String) {
        try {
            val oldSanitized = sanitizeFolderName(oldFolder)
            val newSanitized = sanitizeFolderName(newFolder)
            if (oldSanitized == newSanitized) return

            val oldDir = File(vaultDir, oldSanitized)
            val newDir = File(vaultDir, newSanitized)

            if (oldDir.exists()) {
                if (!newDir.exists()) {
                    oldDir.renameTo(newDir)
                } else {
                    oldDir.listFiles()?.forEach { file ->
                        val dest = File(newDir, file.name)
                        file.renameTo(dest)
                    }
                    oldDir.delete()
                }
            }

            // SAF Tree folder rename
            val treeUri = getVaultTreeUri()
            if (treeUri != null) {
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                val oldTreeDoc = rootDoc?.findFile(oldSanitized)
                if (oldTreeDoc != null && oldTreeDoc.isDirectory) {
                    oldTreeDoc.renameTo(newSanitized)
                }
            }

            // Update cached relative paths in SharedPreferences
            val allKeys = prefs.all.keys.filter { it.startsWith("note_md_path_") }
            val editor = prefs.edit()
            for (key in allKeys) {
                val path = prefs.getString(key, null)
                if (path != null && (path.startsWith("$oldSanitized/") || path.startsWith("$oldSanitized\\"))) {
                    val updatedPath = path.replaceFirst(oldSanitized, newSanitized)
                    editor.putString(key, updatedPath)
                }
            }
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves an encrypted file representation of the note into the local vault folder for security
     */
    fun writeEncryptedNoteFile(id: String, folder: String, encryptedPayload: String) {
        try {
            val encDir = File(vaultDir, ".encrypted").apply { if (!exists()) mkdirs() }
            val noteFile = File(encDir, "$id.vault")
            noteFile.writeText(encryptedPayload, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteEncryptedNoteFile(id: String, folder: String) {
        try {
            val encDir = File(vaultDir, ".encrypted")
            val noteFile = File(encDir, "$id.vault")
            if (noteFile.exists()) {
                noteFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves an encrypted attachment into the local vault attachments folder
     */
    fun saveAttachment(attachmentId: String, inputUri: Uri, originalFileName: String): NoteAttachment? {
        return try {
            val inputStream = context.contentResolver.openInputStream(inputUri) ?: return null
            val rawBytes = inputStream.use { it.readBytes() }
            val encryptedBytes = cryptoManager.encryptBytes(rawBytes)

            val destFile = File(attachmentsDir, "$attachmentId.bin")
            destFile.writeBytes(encryptedBytes)

            val mimeType = context.contentResolver.getType(inputUri) ?: "application/octet-stream"

            NoteAttachment(
                id = attachmentId,
                noteId = "",
                fileName = originalFileName,
                mimeType = mimeType,
                sizeBytes = rawBytes.size.toLong(),
                localRelativePath = ".attachments/$attachmentId.bin"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves byte array directly as encrypted attachment
     */
    fun saveAttachmentBytes(attachmentId: String, rawBytes: ByteArray, fileName: String, mimeType: String): NoteAttachment? {
        return try {
            val encryptedBytes = cryptoManager.encryptBytes(rawBytes)
            val destFile = File(attachmentsDir, "$attachmentId.bin")
            destFile.writeBytes(encryptedBytes)

            NoteAttachment(
                id = attachmentId,
                noteId = "",
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = rawBytes.size.toLong(),
                localRelativePath = ".attachments/$attachmentId.bin"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decrypts and reads an attachment into memory
     */
    fun getDecryptedAttachmentBytes(attachment: NoteAttachment): ByteArray? {
        return try {
            val file = File(vaultDir, attachment.localRelativePath)
            if (!file.exists()) return null
            val encryptedBytes = file.readBytes()
            cryptoManager.decryptBytes(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseMarkdownImport(rawMarkdown: String): Triple<String, String, List<String>> {
        var title = "Nota Importada"
        var content = rawMarkdown.trim()
        val tags = mutableListOf<String>()

        if (rawMarkdown.startsWith("---")) {
            val endIdx = rawMarkdown.indexOf("---", 3)
            if (endIdx != -1) {
                val frontmatter = rawMarkdown.substring(3, endIdx)
                content = rawMarkdown.substring(endIdx + 3).trim()

                for (line in frontmatter.lines()) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("title:")) {
                        title = trimmed.removePrefix("title:").trim().removeSurrounding("\"")
                    } else if (trimmed.startsWith("tags:")) {
                        val tagsLine = trimmed.removePrefix("tags:").trim()
                            .removeSurrounding("[", "]")
                        tags.addAll(tagsLine.split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() })
                    }
                }
            }
        }

        if (title == "Nota Importada") {
            val firstLine = content.lines().firstOrNull { it.isNotBlank() }
            if (firstLine != null && firstLine.startsWith("# ")) {
                title = firstLine.removePrefix("# ").trim()
            }
        }

        return Triple(title, content, tags)
    }

    /**
     * Exports a single note as a standard plaintext Markdown (.md) file with metadata
     */
    fun exportNoteAsMarkdownFile(note: Note): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val createdStr = dateFormat.format(Date(note.createdAt))
        val updatedStr = dateFormat.format(Date(note.updatedAt))
        val tagsStr = if (note.tags.isNotEmpty()) note.tags.joinToString(", ", "[", "]") else "[]"

        val markdownContent = buildString {
            appendLine("---")
            appendLine("title: \"${note.title.replace("\"", "\\\"")}\"")
            appendLine("folder: ${note.folder}")
            appendLine("tags: $tagsStr")
            appendLine("created: $createdStr")
            appendLine("updated: $updatedStr")
            appendLine("---")
            appendLine()
            appendLine("# ${note.title}")
            appendLine()
            appendLine(note.content)
        }

        val sanitizedTitle = note.title.take(30)
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .ifBlank { "nota_${note.id.take(6)}" }

        val exportFile = File(exportDir, "$sanitizedTitle.md")
        exportFile.writeText(markdownContent, Charsets.UTF_8)
        return exportFile
    }

    /**
     * Exports the entire Vault as a ZIP bundle
     */
    fun exportEntireVaultAsZip(notes: List<Note>, attachments: List<NoteAttachment>): File {
        val zipFile = File(exportDir, "LogitNotes_Vault_${System.currentTimeMillis()}.zip")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for (note in notes) {
                val folderPrefix = if (note.folder.isNotBlank() && note.folder != "General") {
                    "${sanitizeFolderName(note.folder)}/"
                } else ""

                val safeTitle = sanitizeFileName(note.title.ifBlank { "nota_${note.id.take(6)}" })
                val entryPath = "$folderPrefix$safeTitle.md"

                val noteMd = buildString {
                    appendLine("---")
                    appendLine("title: \"${note.title.replace("\"", "\\\"")}\"")
                    appendLine("folder: \"${note.folder}\"")
                    appendLine("tags: [${note.tags.joinToString(", ") { "\"$it\"" }}]")
                    appendLine("created: \"${dateFormat.format(Date(note.createdAt))}\"")
                    appendLine("updated: \"${dateFormat.format(Date(note.updatedAt))}\"")
                    appendLine("---")
                    appendLine()
                    appendLine("# ${note.title}")
                    appendLine()
                    appendLine(note.content)
                }

                zos.putNextEntry(ZipEntry(entryPath))
                zos.write(noteMd.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            for (att in attachments) {
                val bytes = getDecryptedAttachmentBytes(att)
                if (bytes != null) {
                    val entryPath = "attachments/${sanitizeFileName(att.fileName)}"
                    zos.putNextEntry(ZipEntry(entryPath))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
        }

        return zipFile
    }

    fun calculateVaultSizeBytes(): Long {
        return getFolderSize(vaultDir)
    }

    private fun getFolderSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getFolderSize(file) else file.length()
        }
        return size
    }

    private fun sanitizeFolderName(folder: String): String {
        return folder.trim().replace(Regex("[^a-zA-Z0-9_ -]"), "_").ifBlank { "General" }
    }

    fun sanitizeFileName(name: String): String {
        return name.trim().take(50).replace(Regex("[^a-zA-Z0-9_ -]"), "_").ifBlank { "nota" }
    }
}

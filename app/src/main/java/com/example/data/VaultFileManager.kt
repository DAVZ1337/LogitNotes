package com.example.data

import android.content.Context
import android.net.Uri
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
        return prefs.getString("vault_storage_type", "SANDBOX") ?: "SANDBOX"
    }

    fun getVaultLocationDescription(): String {
        return when (getVaultLocationType()) {
            "DOCUMENTS" -> "Almacenamiento en Documentos (Accesible)"
            "CUSTOM" -> "Carpeta personalizada del usuario"
            else -> "Almacenamiento Interno Seguro (Sandboxed)"
        }
    }

    fun setVaultLocation(type: String, customPath: String? = null) {
        val targetPath = when (type) {
            "DOCUMENTS" -> {
                val docDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "ObsidianVault").apply { if (!exists()) mkdirs() }
                docDir.absolutePath
            }
            "CUSTOM" -> customPath ?: File(context.filesDir, "obsidian_vault").absolutePath
            else -> File(context.filesDir, "obsidian_vault").apply { if (!exists()) mkdirs() }.absolutePath
        }
        prefs.edit()
            .putString("vault_storage_type", type)
            .putString("vault_storage_path", targetPath)
            .apply()
    }

    val vaultDir: File
        get() = File(prefs.getString("vault_storage_path", File(context.filesDir, "obsidian_vault").absolutePath)
            ?: File(context.filesDir, "obsidian_vault").absolutePath).apply {
            if (!exists()) mkdirs()
        }

    private val notesDir: File
        get() = File(vaultDir, "notes").apply {
            if (!exists()) mkdirs()
        }

    private val attachmentsDir: File
        get() = File(vaultDir, "attachments").apply {
            if (!exists()) mkdirs()
        }

    private val exportDir: File
        get() = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }

    /**
     * Saves an encrypted file representation of the note into the local vault folder
     */
    fun writeEncryptedNoteFile(id: String, folder: String, encryptedPayload: String) {
        try {
            val folderDir = File(notesDir, sanitizeFolderName(folder)).apply {
                if (!exists()) mkdirs()
            }
            val noteFile = File(folderDir, "$id.vault")
            noteFile.writeText(encryptedPayload, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteEncryptedNoteFile(id: String, folder: String) {
        try {
            val folderDir = File(notesDir, sanitizeFolderName(folder))
            val noteFile = File(folderDir, "$id.vault")
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
                localRelativePath = "attachments/$attachmentId.bin"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves byte array directly (e.g. from camera/drawing or WebDAV sync)
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
                localRelativePath = "attachments/$attachmentId.bin"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decrypts and reads an attachment into memory (or temp file for viewing)
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

    /**
     * Exports a single note as a standard plaintext Markdown (.md) file with Obsidian metadata
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
     * Exports the entire Vault as an Obsidian-ready ZIP bundle
     */
    fun exportEntireVaultAsZip(notes: List<Note>, attachments: List<NoteAttachment>): File {
        val zipFile = File(exportDir, "VaultNotes_Export_${System.currentTimeMillis()}.zip")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. Export all notes in their folder hierarchy
            for (note in notes) {
                val folderName = sanitizeFolderName(note.folder)
                val sanitizedTitle = note.title.take(30)
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    .ifBlank { "nota_${note.id.take(6)}" }
                val entryPath = "$folderName/$sanitizedTitle.md"

                val tagsStr = if (note.tags.isNotEmpty()) note.tags.joinToString(", ", "[", "]") else "[]"
                val mdContent = buildString {
                    appendLine("---")
                    appendLine("title: \"${note.title.replace("\"", "\\\"")}\"")
                    appendLine("folder: ${note.folder}")
                    appendLine("tags: $tagsStr")
                    appendLine("created: ${dateFormat.format(Date(note.createdAt))}")
                    appendLine("updated: ${dateFormat.format(Date(note.updatedAt))}")
                    appendLine("---")
                    appendLine()
                    appendLine("# ${note.title}")
                    appendLine()
                    appendLine(note.content)
                }

                zos.putNextEntry(ZipEntry(entryPath))
                zos.write(mdContent.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            // 2. Export decrypted attachments into attachments/ folder
            for (att in attachments) {
                val decrypted = getDecryptedAttachmentBytes(att)
                if (decrypted != null) {
                    val entryPath = "attachments/${att.fileName}"
                    zos.putNextEntry(ZipEntry(entryPath))
                    zos.write(decrypted)
                    zos.closeEntry()
                }
            }
        }

        return zipFile
    }

    /**
     * Imports a Markdown string and parses YAML frontmatter & title
     */
    fun parseMarkdownImport(rawMarkdown: String): Triple<String, String, List<String>> {
        var title = "Nota Importada"
        var content = rawMarkdown
        val tags = mutableListOf<String>()

        if (rawMarkdown.startsWith("---")) {
            val endIdx = rawMarkdown.indexOf("---", 3)
            if (endIdx != -1) {
                val frontmatter = rawMarkdown.substring(3, endIdx)
                content = rawMarkdown.substring(endIdx + 3).trim()
                
                // parse frontmatter lines
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

        // Check if content starts with # Title
        if (title == "Nota Importada") {
            val firstLine = content.lines().firstOrNull { it.isNotBlank() }
            if (firstLine != null && firstLine.startsWith("# ")) {
                title = firstLine.removePrefix("# ").trim()
            }
        }

        return Triple(title, content, tags)
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
}

package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.AttachmentEntity
import com.example.data.local.NoteEntity
import com.example.data.local.VaultDao
import com.example.model.Note
import com.example.model.NoteAttachment
import com.example.model.VaultStats
import com.example.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class VaultRepository(
    private val context: Context,
    private val dao: VaultDao,
    val cryptoManager: CryptoManager,
    val fileManager: VaultFileManager
) {
    /**
     * Returns flow of decrypted active notes. If locked, returns empty list.
     */
    fun getDecryptedNotesFlow(): Flow<List<Note>> {
        return dao.getAllActiveNotesFlow().map { entities ->
            if (!cryptoManager.isUnlocked) {
                emptyList()
            } else {
                val attachmentsByNote = try {
                    dao.getAllAttachments().groupBy { it.noteId }
                } catch (e: Exception) {
                    emptyMap()
                }

                entities.mapNotNull { entity ->
                    try {
                        val title = cryptoManager.decryptString(entity.encryptedTitle)
                        val content = cryptoManager.decryptString(entity.encryptedContent)
                        val tags = if (entity.tagsCsv.isNotBlank()) {
                            entity.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        } else emptyList()

                        val count = attachmentsByNote[entity.id]?.size ?: 0

                        Note(
                            id = entity.id,
                            title = title,
                            content = content,
                            folder = entity.folder,
                            tags = tags,
                            createdAt = entity.createdAt,
                            updatedAt = entity.updatedAt,
                            isPinned = entity.isPinned,
                            isArchived = entity.isArchived,
                            attachmentsCount = count
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getDecryptedNoteById(id: String): Note? = withContext(Dispatchers.IO) {
        if (!cryptoManager.isUnlocked) return@withContext null
        val entity = dao.getNoteById(id) ?: return@withContext null
        try {
            val title = cryptoManager.decryptString(entity.encryptedTitle)
            val content = cryptoManager.decryptString(entity.encryptedContent)
            val tags = if (entity.tagsCsv.isNotBlank()) {
                entity.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else emptyList()
            val attachments = dao.getAttachmentsForNote(entity.id)

            Note(
                id = entity.id,
                title = title,
                content = content,
                folder = entity.folder,
                tags = tags,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                isPinned = entity.isPinned,
                isArchived = entity.isArchived,
                attachmentsCount = attachments.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveNote(
        id: String?,
        title: String,
        content: String,
        folder: String,
        tags: List<String>,
        isPinned: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val noteId = id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val encTitle = cryptoManager.encryptString(title.ifBlank { "Sin título" })
        val encContent = cryptoManager.encryptString(content)
        val tagsCsv = tags.joinToString(",")

        val existing = if (id != null) dao.getNoteById(id) else null
        val entity = NoteEntity(
            id = noteId,
            encryptedTitle = encTitle,
            encryptedContent = encContent,
            folder = folder.ifBlank { "General" },
            tagsCsv = tagsCsv,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            isPinned = isPinned,
            isArchived = false,
            isSyncedWithWebDav = false
        )

        dao.insertOrUpdateNote(entity)

        // Save note directly as Markdown (.md) in the selected vault directory
        fileManager.writeNoteMarkdownFile(
            id = noteId,
            title = title,
            content = content,
            folder = entity.folder,
            tags = tags,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )

        // Save local backup file in encrypted vault format (.vault ciphertext file)
        fileManager.writeEncryptedNoteFile(noteId, entity.folder, "$encTitle\n$encContent")

        noteId
    }

    suspend fun togglePinNote(noteId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        val entity = dao.getNoteById(noteId) ?: return@withContext
        dao.insertOrUpdateNote(entity.copy(isPinned = !isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        val entity = dao.getNoteById(noteId)
        if (entity != null) {
            fileManager.deleteNoteMarkdownFile(noteId)
            fileManager.deleteEncryptedNoteFile(noteId, entity.folder)
        }
        dao.deleteNoteById(noteId)
        dao.deleteAllAttachmentsForNote(noteId)
    }

    suspend fun syncAllNotesToMarkdown() = withContext(Dispatchers.IO) {
        val allEntities = dao.getAllNotesSnapshot()
        for (entity in allEntities) {
            val title = try { cryptoManager.decryptString(entity.encryptedTitle) } catch (e: Exception) { "Nota" }
            val content = try { cryptoManager.decryptString(entity.encryptedContent) } catch (e: Exception) { "" }
            val tags = if (entity.tagsCsv.isNotBlank()) entity.tagsCsv.split(",") else emptyList()
            fileManager.writeNoteMarkdownFile(
                id = entity.id,
                title = title,
                content = content,
                folder = entity.folder,
                tags = tags,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }

    // Attachments
    fun getAttachmentsForNoteFlow(noteId: String): Flow<List<NoteAttachment>> {
        return dao.getAttachmentsForNoteFlow(noteId).map { list ->
            list.map {
                NoteAttachment(
                    id = it.id,
                    noteId = it.noteId,
                    fileName = it.fileName,
                    mimeType = it.mimeType,
                    sizeBytes = it.sizeBytes,
                    localRelativePath = it.localRelativePath,
                    createdAt = it.createdAt
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun addAttachment(noteId: String, uri: Uri, originalFileName: String): NoteAttachment? = withContext(Dispatchers.IO) {
        val attId = UUID.randomUUID().toString()
        val attachment = fileManager.saveAttachment(attId, uri, originalFileName) ?: return@withContext null
        val entity = AttachmentEntity(
            id = attId,
            noteId = noteId,
            fileName = attachment.fileName,
            mimeType = attachment.mimeType,
            sizeBytes = attachment.sizeBytes,
            localRelativePath = attachment.localRelativePath,
            createdAt = attachment.createdAt
        )
        dao.insertAttachment(entity)
        attachment.copy(noteId = noteId)
    }

    suspend fun addAttachmentFromBytes(noteId: String, bytes: ByteArray, fileName: String, mimeType: String): NoteAttachment? = withContext(Dispatchers.IO) {
        val attId = UUID.randomUUID().toString()
        val attachment = fileManager.saveAttachmentBytes(attId, bytes, fileName, mimeType) ?: return@withContext null
        val entity = AttachmentEntity(
            id = attId,
            noteId = noteId,
            fileName = attachment.fileName,
            mimeType = attachment.mimeType,
            sizeBytes = attachment.sizeBytes,
            localRelativePath = attachment.localRelativePath,
            createdAt = attachment.createdAt
        )
        dao.insertAttachment(entity)
        attachment.copy(noteId = noteId)
    }

    suspend fun deleteAttachment(attachmentId: String) = withContext(Dispatchers.IO) {
        dao.deleteAttachment(attachmentId)
    }

    fun getDecryptedAttachmentBytes(attachment: NoteAttachment): ByteArray? {
        return fileManager.getDecryptedAttachmentBytes(attachment)
    }

    // Vault Stats
    suspend fun getVaultStats(): VaultStats = withContext(Dispatchers.IO) {
        val notes = dao.getAllNotesSnapshot()
        val attachments = dao.getAllAttachments()
        val folders = notes.map { it.folder }.distinct()
        val tags = notes.flatMap { it.tagsCsv.split(",") }.filter { it.isNotBlank() }.distinct()
        val sizeBytes = fileManager.calculateVaultSizeBytes()

        VaultStats(
            totalNotes = notes.size,
            totalFolders = folders.size,
            totalTags = tags.size,
            totalAttachments = attachments.size,
            vaultSizeBytes = sizeBytes,
            storageLocationDesc = fileManager.getVaultLocationDescription()
        )
    }

    // Markdown Exports
    fun exportNoteToMarkdownFile(note: Note): File {
        return fileManager.exportNoteAsMarkdownFile(note)
    }

    suspend fun exportVaultToZip(): File = withContext(Dispatchers.IO) {
        val notesSnapshot = dao.getAllNotesSnapshot().mapNotNull { entity ->
            try {
                val title = cryptoManager.decryptString(entity.encryptedTitle)
                val content = cryptoManager.decryptString(entity.encryptedContent)
                val tags = if (entity.tagsCsv.isNotBlank()) entity.tagsCsv.split(",") else emptyList()
                Note(
                    id = entity.id,
                    title = title,
                    content = content,
                    folder = entity.folder,
                    tags = tags,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    isPinned = entity.isPinned,
                    isArchived = entity.isArchived
                )
            } catch (e: Exception) {
                null
            }
        }
        val attachments = dao.getAllAttachments().map {
            NoteAttachment(
                id = it.id,
                noteId = it.noteId,
                fileName = it.fileName,
                mimeType = it.mimeType,
                sizeBytes = it.sizeBytes,
                localRelativePath = it.localRelativePath,
                createdAt = it.createdAt
            )
        }
        fileManager.exportEntireVaultAsZip(notesSnapshot, attachments)
    }

    suspend fun importMarkdownNote(rawMarkdown: String, defaultFolder: String = "Importadas"): String {
        val (parsedTitle, parsedContent, parsedTags) = fileManager.parseMarkdownImport(rawMarkdown)
        return saveNote(
            id = null,
            title = parsedTitle,
            content = parsedContent,
            folder = defaultFolder,
            tags = parsedTags
        )
    }
}

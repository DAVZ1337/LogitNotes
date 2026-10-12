package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["folder"]),
        Index(value = ["isPinned", "updatedAt"]),
        Index(value = ["isArchived"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val encryptedTitle: String,
    val encryptedContent: String,
    val folder: String = "General",
    val tagsCsv: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isSyncedWithWebDav: Boolean = false,
    val remoteVersion: Long = 0
)

@Entity(
    tableName = "attachments",
    indices = [
        Index(value = ["noteId"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localRelativePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

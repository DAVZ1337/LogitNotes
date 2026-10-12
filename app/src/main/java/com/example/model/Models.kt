package com.example.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val folder: String = "General",
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val attachmentsCount: Int = 0
) {
    val previewSnippet: String
        get() {
            val clean = content.replace(Regex("#+\\s*"), "")
                .replace(Regex("\\[.*?\\]\\(.*?\\)"), "")
                .replace(Regex("[*_~`]"), "")
                .trim()
            return if (clean.length > 120) clean.take(120) + "..." else clean
        }
}

data class NoteAttachment(
    val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localRelativePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class VaultStats(
    val totalNotes: Int = 0,
    val totalFolders: Int = 0,
    val totalTags: Int = 0,
    val totalAttachments: Int = 0,
    val vaultSizeBytes: Long = 0,
    val storageLocationDesc: String = "Almacenamiento Local Sandboxed"
)

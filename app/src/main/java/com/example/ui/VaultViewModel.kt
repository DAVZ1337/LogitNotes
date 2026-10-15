package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VaultFileManager
import com.example.data.VaultRepository
import com.example.data.local.VaultDatabase
import com.example.model.Note
import com.example.model.NoteAttachment
import com.example.model.VaultStats
import com.example.security.BiometricHelper
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VaultDatabase.getDatabase(application)
    private val cryptoManager = CryptoManager(application)
    val fileManager = VaultFileManager(application, cryptoManager)

    val repository = VaultRepository(
        context = application,
        dao = database.vaultDao(),
        cryptoManager = cryptoManager,
        fileManager = fileManager
    )

    // Auth / Lock State
    private val _isVaultInitialized = MutableStateFlow(cryptoManager.isVaultInitialized)
    val isVaultInitialized: StateFlow<Boolean> = _isVaultInitialized.asStateFlow()

    private val _isUnlocked = MutableStateFlow(cryptoManager.isUnlocked)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _unlockError = MutableStateFlow<String?>(null)
    val unlockError: StateFlow<String?> = _unlockError.asStateFlow()

    val isBiometricAvailable: Boolean = BiometricHelper.isBiometricAvailable(application)
    val isBiometricEnabled: Boolean get() = cryptoManager.isBiometricEnabled

    // Navigation / Screen
    sealed interface Screen {
        object VaultList : Screen
        data class NoteDetail(val noteId: String?) : Screen
        object Settings : Screen
    }

    private val _currentScreen = MutableStateFlow<Screen>(Screen.VaultList)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Filter & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    // Notes List
    val allNotesFlow = repository.getDecryptedNotesFlow()
    val filteredNotes = combine(
        allNotesFlow,
        _searchQuery,
        _selectedFolder,
        _selectedTag
    ) { notes, query, folder, tag ->
        notes.filter { note ->
            val matchesFolder = folder == null || note.folder.equals(folder, ignoreCase = true)
            val matchesTag = tag == null || note.tags.any { it.equals(tag, ignoreCase = true) }
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    note.tags.any { it.contains(query, ignoreCase = true) }
            matchesFolder && matchesTag && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vault Owner Name (from CryptoManager / user setup)
    private val appPrefs = application.getSharedPreferences("vault_user_profile", Context.MODE_PRIVATE)
    private val _vaultOwnerName = MutableStateFlow(cryptoManager.vaultName)
    val vaultOwnerName: StateFlow<String> = _vaultOwnerName.asStateFlow()

    // Dark Mode Theme State
    private val _isDarkMode = MutableStateFlow(appPrefs.getBoolean("dark_mode_enabled", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        appPrefs.edit().putBoolean("dark_mode_enabled", newMode).apply()
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        appPrefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
    }

    // Explicit folders created by user
    private val _customFolders = MutableStateFlow(
        appPrefs.getString("custom_folders", "")?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    )
    val customFolders: StateFlow<List<String>> = _customFolders.asStateFlow()

    // Combined folders
    val folders = combine(allNotesFlow, _customFolders) { notes, custom ->
        (custom + notes.map { it.folder }).distinct().filter { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags = allNotesFlow.combine(_selectedTag) { notes, _ ->
        notes.flatMap { it.tags }.distinct().filter { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Note Editor State
    private val _editingNoteId = MutableStateFlow<String?>(null)
    val editingNoteId: StateFlow<String?> = _editingNoteId.asStateFlow()

    private val _noteTitle = MutableStateFlow("")
    val noteTitle: StateFlow<String> = _noteTitle.asStateFlow()

    private val _noteContent = MutableStateFlow("")
    val noteContent: StateFlow<String> = _noteContent.asStateFlow()

    private val _noteFolder = MutableStateFlow("General")
    val noteFolder: StateFlow<String> = _noteFolder.asStateFlow()

    private val _noteTags = MutableStateFlow<List<String>>(emptyList())
    val noteTags: StateFlow<List<String>> = _noteTags.asStateFlow()

    private val _noteIsPinned = MutableStateFlow(false)
    val noteIsPinned: StateFlow<Boolean> = _noteIsPinned.asStateFlow()

    private val _noteAttachments = MutableStateFlow<List<NoteAttachment>>(emptyList())
    val noteAttachments: StateFlow<List<NoteAttachment>> = _noteAttachments.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    // Vault Stats
    private val _vaultStats = MutableStateFlow(VaultStats())
    val vaultStats: StateFlow<VaultStats> = _vaultStats.asStateFlow()

    private val _vaultStoragePath = MutableStateFlow(fileManager.getVaultStoragePath())
    val vaultStoragePath: StateFlow<String> = _vaultStoragePath.asStateFlow()

    private val _vaultStorageType = MutableStateFlow(fileManager.getVaultLocationType())
    val vaultStorageType: StateFlow<String> = _vaultStorageType.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _lastSavedTime = MutableStateFlow<Long?>(null)
    val lastSavedTime: StateFlow<Long?> = _lastSavedTime.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        refreshVaultStats()
    }

    // Auth Actions
    fun setupVault(
        name: String,
        password: String,
        enableBiometric: Boolean,
        storageType: String = "DOCUMENTS",
        customStoragePath: String? = null,
        treeUri: Uri? = null
    ) {
        if (password.length < 4) {
            _unlockError.value = "La contraseña maestra debe tener al menos 4 caracteres"
            return
        }
        val cleanName = name.trim().ifBlank { "Mi Bóveda" }
        if (treeUri != null) {
            fileManager.setVaultTreeUri(treeUri)
        } else {
            fileManager.setVaultLocation(storageType, customStoragePath)
        }
        _vaultStoragePath.value = fileManager.getVaultStoragePath()
        _vaultStorageType.value = fileManager.getVaultLocationType()
        val success = cryptoManager.setupVault(cleanName, password, enableBiometric)
        if (success) {
            _vaultOwnerName.value = cleanName
            _isVaultInitialized.value = true
            _isUnlocked.value = true
            _unlockError.value = null
            refreshVaultStats()
        } else {
            _unlockError.value = "Error al inicializar la bóveda"
        }
    }

    fun setVaultTreeUri(uri: Uri, friendlyName: String? = null) {
        fileManager.setVaultTreeUri(uri, friendlyName)
        _vaultStoragePath.value = fileManager.getVaultStoragePath()
        _vaultStorageType.value = fileManager.getVaultLocationType()
        viewModelScope.launch {
            _isSaving.value = true
            repository.syncAllNotesToMarkdown()
            _isSaving.value = false
            refreshVaultStats()
            _statusMessage.value = "Carpeta configurada: ${_vaultStoragePath.value}"
        }
    }

    fun unlockWithPassword(password: String) {
        val success = cryptoManager.unlockWithPassword(password)
        if (success) {
            _isUnlocked.value = true
            _unlockError.value = null
            refreshVaultStats()
        } else {
            _unlockError.value = "Contraseña maestra incorrecta"
        }
    }

    fun unlockWithBiometric() {
        val success = cryptoManager.unlockWithBiometric()
        if (success) {
            _isUnlocked.value = true
            _unlockError.value = null
            refreshVaultStats()
        } else {
            _unlockError.value = "Error en autenticación biométrica"
        }
    }

    fun lockVault() {
        cryptoManager.lockVault()
        repository.clearDecryptedCache()
        _isUnlocked.value = false
        _currentScreen.value = Screen.VaultList
        _editingNoteId.value = null
    }

    fun clearUnlockError() {
        _unlockError.value = null
    }

    // Navigation
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.NoteDetail) {
            loadNote(screen.noteId)
        } else if (screen is Screen.Settings) {
            refreshVaultStats()
        }
    }

    // Filters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateVaultOwnerName(name: String) {
        val clean = name.trim().ifBlank { "David" }
        _vaultOwnerName.value = clean
        appPrefs.edit().putString("vault_owner_name", clean).apply()
    }

    fun createFolder(folderName: String) {
        val clean = folderName.trim().ifBlank { "Nueva Carpeta" }
        if (!_customFolders.value.contains(clean)) {
            val updated = _customFolders.value + clean
            _customFolders.value = updated
            appPrefs.edit().putString("custom_folders", updated.joinToString(",")).apply()
        }
    }

    fun renameFolder(oldFolder: String, newFolder: String) {
        val cleanOld = oldFolder.trim()
        val cleanNew = newFolder.trim()
        if (cleanOld.isBlank() || cleanNew.isBlank() || cleanOld.equals(cleanNew, ignoreCase = true)) return

        viewModelScope.launch {
            _isSaving.value = true
            repository.renameFolder(cleanOld, cleanNew)

            val currentCustom = _customFolders.value.filterNot { it.equals(cleanOld, ignoreCase = true) }.toMutableList()
            currentCustom.add(cleanNew)
            val distinctUpdated = currentCustom.distinct()
            _customFolders.value = distinctUpdated
            appPrefs.edit().putString("custom_folders", distinctUpdated.joinToString(",")).apply()

            if (_selectedFolder.value?.equals(cleanOld, ignoreCase = true) == true) {
                _selectedFolder.value = cleanNew
            }
            if (_noteFolder.value.equals(cleanOld, ignoreCase = true)) {
                _noteFolder.value = cleanNew
            }
            _isSaving.value = false
            refreshVaultStats()
            _statusMessage.value = "Carpeta renombrada a $cleanNew"
        }
    }

    fun deleteFolder(folderName: String) {
        val clean = folderName.trim()
        viewModelScope.launch {
            repository.renameFolder(clean, "General")
            val currentCustom = _customFolders.value.filterNot { it.equals(clean, ignoreCase = true) }
            _customFolders.value = currentCustom
            appPrefs.edit().putString("custom_folders", currentCustom.joinToString(",")).apply()
            if (_selectedFolder.value.equals(clean, ignoreCase = true)) {
                _selectedFolder.value = null
            }
            refreshVaultStats()
            _statusMessage.value = "Carpeta eliminada (notas movidas a General)"
        }
    }

    fun selectFolder(folder: String?) {
        _selectedFolder.value = folder
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    private var isDeletingNote = false

    // Note Editing
    private fun loadNote(noteId: String?) {
        isDeletingNote = false
        _editingNoteId.value = noteId
        _isPreviewMode.value = false
        if (noteId == null) {
            // New Note
            _noteTitle.value = ""
            _noteContent.value = ""
            _noteFolder.value = _selectedFolder.value ?: "General"
            _noteTags.value = _selectedTag.value?.let { listOf(it) } ?: emptyList()
            _noteIsPinned.value = false
            _noteAttachments.value = emptyList()
        } else {
            viewModelScope.launch {
                val note = repository.getDecryptedNoteById(noteId)
                if (note != null) {
                    _noteTitle.value = note.title
                    _noteContent.value = note.content
                    _noteFolder.value = note.folder
                    _noteTags.value = note.tags
                    _noteIsPinned.value = note.isPinned
                }
                repository.getAttachmentsForNoteFlow(noteId).collect { list ->
                    _noteAttachments.value = list
                }
            }
        }
    }

    fun updateNoteTitle(title: String) {
        _noteTitle.value = title
    }

    fun updateNoteContent(content: String) {
        _noteContent.value = content
    }

    fun updateNoteFolder(folder: String) {
        _noteFolder.value = folder
    }

    fun addTag(tag: String) {
        val clean = tag.trim().removePrefix("#")
        if (clean.isNotBlank() && !_noteTags.value.contains(clean)) {
            _noteTags.value = _noteTags.value + clean
        }
    }

    fun removeTag(tag: String) {
        _noteTags.value = _noteTags.value.filter { it != tag }
    }

    fun togglePinned() {
        _noteIsPinned.value = !_noteIsPinned.value
    }

    fun togglePreviewMode() {
        _isPreviewMode.value = !_isPreviewMode.value
    }

    private var saveJob: kotlinx.coroutines.Job? = null

    fun saveCurrentNote() {
        if (!_isUnlocked.value || isDeletingNote) return
        val title = _noteTitle.value.trim()
        val content = _noteContent.value
        // If both title and content are blank, don't persist an empty note
        if (title.isBlank() && content.isBlank()) return
        val finalTitle = title.ifBlank { "Nota sin título" }

        val currentId = _editingNoteId.value
        val currentFolder = _noteFolder.value
        val currentTags = _noteTags.value
        val isPinned = _noteIsPinned.value

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _isSaving.value = true
            val savedId = repository.saveNote(
                id = currentId,
                title = finalTitle,
                content = content,
                folder = currentFolder,
                tags = currentTags,
                isPinned = isPinned
            )
            if (!isDeletingNote && isActive) {
                _editingNoteId.value = savedId
                _lastSavedTime.value = System.currentTimeMillis()
            }
            _isSaving.value = false
            refreshVaultStats()
        }
    }

    fun setVaultStorageLocation(type: String, customPath: String? = null) {
        fileManager.setVaultLocation(type, customPath)
        _vaultStoragePath.value = fileManager.getVaultStoragePath()
        _vaultStorageType.value = fileManager.getVaultLocationType()
        viewModelScope.launch {
            _isSaving.value = true
            repository.syncAllNotesToMarkdown()
            _isSaving.value = false
            refreshVaultStats()
            _statusMessage.value = "Carpeta actualizada: ${_vaultStoragePath.value}"
        }
    }

    fun syncAllNotesToMarkdown() {
        viewModelScope.launch {
            _isSaving.value = true
            repository.syncAllNotesToMarkdown()
            _isSaving.value = false
            _statusMessage.value = "Notas sincronizadas en formato .md"
        }
    }

    fun deleteCurrentNote() {
        isDeletingNote = true
        saveJob?.cancel()
        val id = _editingNoteId.value
        _editingNoteId.value = null
        _noteTitle.value = ""
        _noteContent.value = ""
        _currentScreen.value = Screen.VaultList
        if (id != null) {
            viewModelScope.launch {
                repository.deleteNote(id)
                refreshVaultStats()
                _statusMessage.value = "Nota eliminada"
                isDeletingNote = false
            }
        } else {
            isDeletingNote = false
        }
    }

    fun deleteNote(noteId: String) {
        if (_editingNoteId.value == noteId) {
            isDeletingNote = true
            saveJob?.cancel()
            _editingNoteId.value = null
            _noteTitle.value = ""
            _noteContent.value = ""
            _currentScreen.value = Screen.VaultList
        }
        viewModelScope.launch {
            repository.deleteNote(noteId)
            refreshVaultStats()
            _statusMessage.value = "Nota eliminada"
            isDeletingNote = false
        }
    }

    fun toggleChecklistLine(lineIndex: Int, isChecked: Boolean) {
        val lines = _noteContent.value.lines().toMutableList()
        if (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            val newLine = if (isChecked) {
                line.replaceFirst("- [ ] ", "- [x] ")
                    .replaceFirst("* [ ] ", "* [x] ")
            } else {
                line.replaceFirst("- [x] ", "- [ ] ")
                    .replaceFirst("- [X] ", "- [ ] ")
                    .replaceFirst("* [x] ", "* [ ] ")
            }
            lines[lineIndex] = newLine
            _noteContent.value = lines.joinToString("\n")
            saveCurrentNote()
        }
    }

    // Attachments
    fun addAttachmentFromUri(uri: Uri, fileName: String) {
        viewModelScope.launch {
            val noteId = _editingNoteId.value ?: run {
                val newId = repository.saveNote(
                    id = null,
                    title = _noteTitle.value.ifBlank { "Nota con adjunto" },
                    content = _noteContent.value,
                    folder = _noteFolder.value,
                    tags = _noteTags.value
                )
                _editingNoteId.value = newId
                newId
            }
            val attachment = repository.addAttachment(noteId, uri, fileName)
            if (attachment != null) {
                _noteAttachments.value = _noteAttachments.value + attachment
                refreshVaultStats()
            }
        }
    }

    fun addAttachmentFromBytes(bytes: ByteArray, fileName: String, mimeType: String) {
        viewModelScope.launch {
            val noteId = _editingNoteId.value ?: run {
                val newId = repository.saveNote(
                    id = null,
                    title = _noteTitle.value.ifBlank { "Nota con adjunto" },
                    content = _noteContent.value,
                    folder = _noteFolder.value,
                    tags = _noteTags.value
                )
                _editingNoteId.value = newId
                newId
            }
            val attachment = repository.addAttachmentFromBytes(noteId, bytes, fileName, mimeType)
            if (attachment != null) {
                _noteAttachments.value = _noteAttachments.value + attachment
                refreshVaultStats()
            }
        }
    }

    fun deleteAttachment(id: String) {
        viewModelScope.launch {
            repository.deleteAttachment(id)
            _noteAttachments.value = _noteAttachments.value.filter { it.id != id }
            refreshVaultStats()
        }
    }

    // Markdown Export & Sharing
    fun exportCurrentNoteAsMarkdown(context: Context) {
        viewModelScope.launch {
            val currentNote = Note(
                id = _editingNoteId.value ?: "temp",
                title = _noteTitle.value.ifBlank { "Nota" },
                content = _noteContent.value,
                folder = _noteFolder.value,
                tags = _noteTags.value,
                isPinned = _noteIsPinned.value
            )
            val exportedFile = repository.exportNoteToMarkdownFile(currentNote)
            shareFile(context, exportedFile, "text/markdown", "Exportar Nota Markdown")
        }
    }

    fun exportFullVaultZip(context: Context) {
        viewModelScope.launch {
            _isExporting.value = true
            _statusMessage.value = "Generando archivo ZIP de la bóveda..."
            try {
                val zipFile = repository.exportVaultToZip()
                _statusMessage.value = "Bóveda exportada exitosamente"
                shareFile(context, zipFile, "application/zip", "Exportar Bóveda LogitNotes (.zip)")
            } catch (e: Exception) {
                _statusMessage.value = "Error al exportar: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun importMarkdownFile(rawMarkdown: String) {
        viewModelScope.launch {
            try {
                repository.importMarkdownNote(rawMarkdown)
                refreshVaultStats()
                _statusMessage.value = "Nota importada con éxito"
            } catch (e: Exception) {
                _statusMessage.value = "Error al importar nota: ${e.message}"
            }
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            _statusMessage.value = "Error al compartir archivo: ${e.message}"
        }
    }

    fun setBiometricSetting(enabled: Boolean) {
        cryptoManager.setBiometricEnabled(enabled)
    }

    fun refreshVaultStats() {
        viewModelScope.launch {
            _vaultStats.value = repository.getVaultStats()
        }
    }
}

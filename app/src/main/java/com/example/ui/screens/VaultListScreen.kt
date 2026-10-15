package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Note
import com.example.ui.VaultViewModel
import com.example.ui.theme.BorderDashedWarm
import com.example.ui.theme.BorderSubtleWarm
import com.example.ui.theme.CanvasBgWarm
import com.example.ui.theme.EmeraldSecurity
import com.example.ui.theme.FolderAmber
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceCardDashed
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VaultListScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val notes by viewModel.filteredNotes.collectAsState()
    val allNotes by viewModel.allNotesFlow.collectAsState(initial = emptyList())
    val folders by viewModel.folders.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val vaultOwnerName by viewModel.vaultOwnerName.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }

    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf("") }
    var renameFolderInput by remember { mutableStateOf("") }

    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    var showEditOwnerDialog by remember { mutableStateOf(false) }
    var newOwnerNameInput by remember { mutableStateOf(vaultOwnerName) }

    val scope = rememberCoroutineScope()

    // Handle back button when inside a folder or when searching: return to root
    BackHandler(enabled = selectedFolder != null || isSearchActive || searchQuery.isNotEmpty()) {
        if (selectedFolder != null) {
            viewModel.selectFolder(null)
        } else if (searchQuery.isNotEmpty()) {
            viewModel.setSearchQuery("")
        } else if (isSearchActive) {
            isSearchActive = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(VaultViewModel.Screen.NoteDetail(null)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("create_note_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Nota", modifier = Modifier.size(26.dp))
            }
        }
    ) { innerPadding ->
        if (selectedFolder == null) {
            // ==========================================
            // ROOT BÓVEDA SCREEN (Matches Screenshot)
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 28.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header: "L O G I T N O T E S" + "David" and Top Controls
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    newOwnerNameInput = vaultOwnerName
                                    showEditOwnerDialog = true
                                }
                        ) {
                            Text(
                                text = "L O G I T N O T E S",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 3.sp,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = vaultOwnerName,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 38.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Minimalist Lupita (Search Toggle)
                            IconButton(
                                onClick = {
                                    isSearchActive = !isSearchActive
                                    if (!isSearchActive) viewModel.setSearchQuery("")
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("search_notes_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar notas",
                                    tint = if (isSearchActive || searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Minimalist Dark Mode Toggle (Preserves layout, toggles light/dark)
                            IconButton(
                                onClick = { viewModel.toggleDarkMode() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("dark_mode_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isDarkMode) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                                    tint = if (isDarkMode) FolderAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Settings Button
                            IconButton(
                                onClick = { viewModel.navigateTo(VaultViewModel.Screen.Settings) },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(36.dp)
                                    .testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Ajustes",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // "🔒 Bloquear" Pill Button
                            Surface(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { viewModel.lockVault() }
                                    .testTag("lock_vault_button"),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🔒", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Bloquear",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Minimalist Search Bar on Principal Screen
                if (isSearchActive || searchQuery.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Buscar en toda la bóveda...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    IconButton(onClick = { isSearchActive = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("main_notes_search_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                // If searching, display search results directly
                if (searchQuery.isNotEmpty()) {
                    val searchResults = allNotes.filter { note ->
                        note.title.contains(searchQuery, ignoreCase = true) ||
                                note.content.contains(searchQuery, ignoreCase = true) ||
                                note.tags.any { it.contains(searchQuery, ignoreCase = true) } ||
                                note.folder.contains(searchQuery, ignoreCase = true)
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Resultados (${searchResults.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            TextButton(onClick = {
                                viewModel.setSearchQuery("")
                                isSearchActive = false
                            }) {
                                Text("Cerrar búsqueda", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }

                    if (searchResults.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "🔍", fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No se encontraron notas para \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(searchResults, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                onClick = { viewModel.navigateTo(VaultViewModel.Screen.NoteDetail(note.id)) },
                                onTogglePin = {
                                    scope.launch {
                                        viewModel.repository.togglePinNote(note.id, note.isPinned)
                                    }
                                },
                                onDelete = { noteToDelete = note }
                            )
                        }
                    }
                } else {
                    // 2-Column Grid of Folders (dvid, + Carpeta)
                    item {
                        val folderList = folders
                        val rowCount = (folderList.size + 1 + 1) / 2

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (r in 0 until rowCount) {
                            val idx1 = r * 2
                            val idx2 = r * 2 + 1

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // First column item
                                if (idx1 < folderList.size) {
                                    val fName = folderList[idx1]
                                    val noteCount = allNotes.count { it.folder.equals(fName, ignoreCase = true) }
                                    FolderCard(
                                        folderName = fName,
                                        noteCount = noteCount,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.selectFolder(fName) },
                                        onRename = {
                                            folderToRename = fName
                                            renameFolderInput = fName
                                            showRenameFolderDialog = true
                                        }
                                    )
                                } else if (idx1 == folderList.size) {
                                    AddFolderCard(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            newFolderNameInput = ""
                                            showNewFolderDialog = true
                                        }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                // Second column item
                                if (idx2 < folderList.size) {
                                    val fName = folderList[idx2]
                                    val noteCount = allNotes.count { it.folder.equals(fName, ignoreCase = true) }
                                    FolderCard(
                                        folderName = fName,
                                        noteCount = noteCount,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.selectFolder(fName) },
                                        onRename = {
                                            folderToRename = fName
                                            renameFolderInput = fName
                                            showRenameFolderDialog = true
                                        }
                                    )
                                } else if (idx2 == folderList.size) {
                                    AddFolderCard(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            newFolderNameInput = ""
                                            showNewFolderDialog = true
                                        }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }


                // Recent notes overview (if any notes exist)
                if (allNotes.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Todas las notas (${allNotes.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    items(allNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onClick = { viewModel.navigateTo(VaultViewModel.Screen.NoteDetail(note.id)) },
                            onTogglePin = {
                                scope.launch {
                                    viewModel.repository.togglePinNote(note.id, note.isPinned)
                                }
                            },
                            onDelete = { noteToDelete = note }
                        )
                    }
                }
            }
        }
    } else {
            // ==========================================
            // INSIDE FOLDER VIEW
            // ==========================================
            val currentFolder = selectedFolder!!
            val folderNotes = notes.filter { it.folder.equals(currentFolder, ignoreCase = true) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with back navigation to root
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.selectFolder(null) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver a Bóveda",
                                tint = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "B Ó V E D A",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 2.sp,
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentFolder,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        folderToRename = currentFolder
                                        renameFolderInput = currentFolder
                                        showRenameFolderDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Renombrar carpeta",
                                        tint = TextSecondaryMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Bar inside folder
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Buscar notas en $currentFolder...", color = TextSecondaryMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryMuted)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // Notes List
                if (folderNotes.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📁", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No hay notas en $currentFolder",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pulsa el botón + para escribir tu primera nota",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryMuted
                            )
                        }
                    }
                } else {
                    items(folderNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onClick = { viewModel.navigateTo(VaultViewModel.Screen.NoteDetail(note.id)) },
                            onTogglePin = {
                                scope.launch {
                                    viewModel.repository.togglePinNote(note.id, note.isPinned)
                                }
                            },
                            onDelete = { noteToDelete = note }
                        )
                    }
                }
            }
        }
    }

    // Dialog to Add New Folder
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = {
                Text(
                    text = "Nueva Carpeta",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
                )
            },
            text = {
                OutlinedTextField(
                    value = newFolderNameInput,
                    onValueChange = { newFolderNameInput = it },
                    placeholder = { Text("Nombre de la carpeta (ej. dvid, proyectos)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimaryDark
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderNameInput.isNotBlank()) {
                            viewModel.createFolder(newFolderNameInput)
                        }
                        showNewFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryDark)
                ) {
                    Text("Crear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancelar", color = TextSecondaryMuted)
                }
            }
        )
    }

    // Dialog to Rename Any Folder (including imported ones)
    if (showRenameFolderDialog) {
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            title = {
                Text(
                    text = "Renombrar Carpeta",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Introduce el nuevo nombre para la carpeta \"$folderToRename\":",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameFolderInput,
                        onValueChange = { renameFolderInput = it },
                        placeholder = { Text("Nombre de la carpeta") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_folder_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextPrimaryDark,
                            unfocusedBorderColor = BorderSubtleWarm
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = renameFolderInput.trim()
                        if (clean.isNotBlank()) {
                            viewModel.renameFolder(folderToRename, clean)
                        }
                        showRenameFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryDark),
                    modifier = Modifier.testTag("confirm_rename_folder_btn")
                ) {
                    Text("Renombrar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFolderDialog = false }) {
                    Text("Cancelar", color = TextSecondaryMuted)
                }
            }
        )
    }

    // Dialog to Delete Note (from list or inside any folder)
    if (noteToDelete != null) {
        val targetNote = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = {
                Text(
                    text = "Eliminar nota",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "¿Seguro que deseas eliminar permanentemente la nota \"${targetNote.title.ifBlank { "Sin título" }}\"? Se borrará de la carpeta \"${targetNote.folder}\" y de los archivos del dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToDelete = targetNote.id
                        noteToDelete = null
                        viewModel.deleteNote(idToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                    modifier = Modifier.testTag("confirm_delete_note_btn")
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancelar", color = TextSecondaryMuted)
                }
            }
        )
    }

    // Dialog to Edit Vault Owner Name
    if (showEditOwnerDialog) {
        AlertDialog(
            onDismissRequest = { showEditOwnerDialog = false },
            title = {
                Text(
                    text = "Nombre de la Bóveda",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
                )
            },
            text = {
                OutlinedTextField(
                    value = newOwnerNameInput,
                    onValueChange = { newOwnerNameInput = it },
                    placeholder = { Text("Tu nombre o nombre de bóveda") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimaryDark
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newOwnerNameInput.isNotBlank()) {
                            viewModel.updateVaultOwnerName(newOwnerNameInput)
                        }
                        showEditOwnerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryDark)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditOwnerDialog = false }) {
                    Text("Cancelar", color = TextSecondaryMuted)
                }
            }
        )
    }
}

/**
 * Solid White Folder Card with Yellow 📁 Icon, Title, Rename Button, and Note Count
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folderName: String,
    noteCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRename: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onRename
            )
            .testTag("folder_card_$folderName"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📁",
                    fontSize = 30.sp
                )
                IconButton(
                    onClick = onRename,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("rename_folder_${folderName}_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Renombrar carpeta",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$noteCount notas",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

/**
 * Dashed / Subtle Outline Card for "+ Carpeta" (Matches Screenshot)
 */
@Composable
fun AddFolderCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("add_folder_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ Carpeta",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Clean, warm minimalist Note Card
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteCardItem(
    note: Note,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(note.updatedAt))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("note_card_${note.id.take(6)}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (note.isPinned) FolderAmber else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row: Folder badge, attachments count, date, pin & delete buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = note.folder,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (note.attachmentsCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${note.attachmentsCount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("pin_note_${note.id.take(6)}")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Desfijar" else "Fijar",
                            tint = if (note.isPinned) FolderAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_note_${note.id.take(6)}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar nota",
                            tint = RoseError.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Clickable body of the note
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp, top = 2.dp)
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (note.previewSnippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.previewSnippet,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        note.tags.take(4).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

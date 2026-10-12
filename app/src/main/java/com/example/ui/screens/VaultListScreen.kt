package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
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
    val notes by viewModel.filteredNotes.collectAsState()
    val allNotes by viewModel.allNotesFlow.collectAsState(initial = emptyList())
    val folders by viewModel.folders.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val vaultOwnerName by viewModel.vaultOwnerName.collectAsState()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }

    var showEditOwnerDialog by remember { mutableStateOf(false) }
    var newOwnerNameInput by remember { mutableStateOf(vaultOwnerName) }

    val scope = rememberCoroutineScope()

    // Handle back button when inside a folder: return to root folder grid
    BackHandler(enabled = selectedFolder != null) {
        viewModel.selectFolder(null)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasBgWarm,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(VaultViewModel.Screen.NoteDetail(null)) },
                containerColor = TextPrimaryDark,
                contentColor = Color.White,
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header: "B Ó V E D A" + "David" and "🔒 Bloquear"
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
                                    color = TextSecondaryMuted
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = vaultOwnerName,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 42.sp,
                                    color = TextPrimaryDark
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Settings Button
                            IconButton(
                                onClick = { viewModel.navigateTo(VaultViewModel.Screen.Settings) },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(38.dp)
                                    .testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Ajustes",
                                    tint = TextSecondaryMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // "🔒 Bloquear" Pill Button (exact match to screenshot)
                            Surface(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { viewModel.lockVault() }
                                    .testTag("lock_vault_button"),
                                shape = CircleShape,
                                color = SurfaceCardWhite,
                                border = BorderStroke(1.dp, BorderSubtleWarm)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🔒", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Bloquear",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = TextPrimaryDark
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

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
                                        onClick = { viewModel.selectFolder(fName) }
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
                                        onClick = { viewModel.selectFolder(fName) }
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
                                color = TextPrimaryDark
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
                            }
                        )
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
                        Column {
                            Text(
                                text = "B Ó V E D A",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 2.sp,
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                            )
                            Text(
                                text = currentFolder,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                            )
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
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCardWhite,
                            unfocusedContainerColor = SurfaceCardWhite,
                            focusedBorderColor = TextPrimaryDark,
                            unfocusedBorderColor = BorderSubtleWarm
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
                            }
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
 * Solid White Folder Card with Yellow 📁 Icon, Title, and Note Count (Matches Screenshot)
 */
@Composable
fun FolderCard(
    folderName: String,
    noteCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("folder_card_$folderName"),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCardWhite,
        border = BorderStroke(1.dp, BorderSubtleWarm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "📁",
                fontSize = 32.sp
            )

            Column {
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimaryDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$noteCount notas",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = TextSecondaryMuted
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
        color = CanvasBgWarm.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, BorderDashedWarm)
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
                    color = TextSecondaryMuted
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
    onTogglePin: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(note.updatedAt))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("note_card_${note.id.take(6)}"),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardWhite,
        border = BorderStroke(
            1.dp,
            if (note.isPinned) FolderAmber else BorderSubtleWarm
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CanvasBgWarm
                    ) {
                        Text(
                            text = note.folder,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TextSecondaryMuted
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (note.attachmentsCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = TextSecondaryMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${note.attachmentsCount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = TextSecondaryMuted
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextSecondaryMuted
                    )
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Desfijar" else "Fijar",
                            tint = if (note.isPinned) FolderAmber else TextSecondaryMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (note.previewSnippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.previewSnippet,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = TextSecondaryMuted,
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
                                fontSize = 11.sp
                            ),
                            color = TextSecondaryMuted,
                            modifier = Modifier
                                .background(CanvasBgWarm, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

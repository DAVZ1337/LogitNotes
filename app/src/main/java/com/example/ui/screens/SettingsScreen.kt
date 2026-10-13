package com.example.ui.screens

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VaultViewModel
import com.example.ui.theme.BorderSubtleWarm
import com.example.ui.theme.CanvasBgWarm
import com.example.ui.theme.FolderAmber
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vaultStats by viewModel.vaultStats.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val vaultStoragePath by viewModel.vaultStoragePath.collectAsState()
    val vaultStorageType by viewModel.vaultStorageType.collectAsState()

    var biometricEnabled by remember { mutableStateOf(viewModel.isBiometricEnabled) }
    var showChangePathDialog by remember { mutableStateOf(false) }
    var tempStorageOption by remember { mutableStateOf(vaultStorageType) }
    var tempCustomPath by remember { mutableStateOf(vaultStoragePath) }

    // Import Markdown file launcher
    val importMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val rawMarkdown = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (rawMarkdown.isNotBlank()) {
                    viewModel.importMarkdownFile(rawMarkdown)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BackHandler {
        viewModel.navigateTo(VaultViewModel.Screen.VaultList)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasBgWarm,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes de la Bóveda",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = TextPrimaryDark
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(VaultViewModel.Screen.VaultList) },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CanvasBgWarm
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Almacenamiento 100% Local y Privacidad
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCardWhite,
                border = BorderStroke(1.dp, BorderSubtleWarm)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Almacenamiento 100% Local",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tus notas se guardan de forma exclusiva en el almacenamiento interno de tu dispositivo con cifrado AES-256-GCM. Sin servidores, sin telemetría y sin dependencias externas.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = TextSecondaryMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CanvasBgWarm
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🛡️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cifrado Militar AES-256-GCM",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Clave derivada por PBKDF2 (100.000 iteraciones)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryMuted
                                )
                            }
                        }
                    }
                }
            }

            // 2. Carpeta y Almacenamiento de Notas (.md)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCardWhite,
                border = BorderStroke(1.dp, BorderSubtleWarm)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Carpeta de Notas (.md)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Cada nota se guarda automáticamente como un archivo .md estándar dentro de esta carpeta, organizada en subcarpetas:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CanvasBgWarm,
                        border = BorderStroke(1.dp, BorderSubtleWarm)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Ruta activa:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondaryMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = vaultStoragePath,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = TextPrimaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                tempStorageOption = vaultStorageType
                                tempCustomPath = vaultStoragePath
                                showChangePathDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderSubtleWarm)
                        ) {
                            Text("Cambiar Carpeta", color = TextPrimaryDark)
                        }

                        Button(
                            onClick = { viewModel.syncAllNotesToMarkdown() },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextPrimaryDark,
                                contentColor = Color.White
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("Sincronizar .md", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Seguridad & Biometría
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCardWhite,
                border = BorderStroke(1.dp, BorderSubtleWarm)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Seguridad y Bloqueo",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewModel.isBiometricAvailable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Desbloqueo Biométrico",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Abrir con huella dactilar o reconocimiento facial",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryMuted
                                )
                            }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = {
                                    biometricEnabled = it
                                    viewModel.setBiometricSetting(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimaryDark,
                                    checkedTrackColor = FolderAmber
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = BorderSubtleWarm
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.lockVault() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                        border = BorderStroke(1.dp, BorderSubtleWarm)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bloquear Bóveda Inmediatamente")
                    }
                }
            }

            // 3. Exportar & Importar Markdown
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCardWhite,
                border = BorderStroke(1.dp, BorderSubtleWarm)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Copia de Seguridad y Markdown",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Exporta tus notas en archivos estándar .md organizados en carpetas con adjuntos, 100% interoperables y portables.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.exportFullVaultZip(context) },
                        enabled = !isExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_zip_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextPrimaryDark,
                            contentColor = Color.White
                        )
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar Bóveda Completa (.zip)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            importMarkdownLauncher.launch(arrayOf("text/*", "text/plain", "application/octet-stream"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtleWarm),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar Nota Markdown (.md)")
                    }

                    AnimatedVisibility(visible = statusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryMuted
                        )
                    }
                }
            }

            // 4. Especificaciones Técnicas
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCardWhite,
                border = BorderStroke(1.dp, BorderSubtleWarm)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Detalles Técnicos",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SpecRow("Aplicación", "LogitNotes")
                    SpecRow("Cifrado", "AES-256-GCM Autenticado")
                    SpecRow("Almacenamiento", "100% Local en Dispositivo")
                    SpecRow("Notas almacenadas", "${vaultStats.totalNotes}")
                    SpecRow("Carpetas creadas", "${vaultStats.totalFolders}")
                    SpecRow("Archivos adjuntos", "${vaultStats.totalAttachments}")
                    SpecRow("Tamaño en disco", Formatter.formatFileSize(context, vaultStats.vaultSizeBytes))
                    SpecRow("Ubicación de guardado", vaultStats.storageLocationDesc)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showChangePathDialog) {
        AlertDialog(
            onDismissRequest = { showChangePathDialog = false },
            title = {
                Text(
                    text = "Seleccionar Carpeta de Notas",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Elige la ubicación donde se crearán los archivos .md:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )

                    // Documentos
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempStorageOption = "DOCUMENTS" }
                    ) {
                        RadioButton(
                            selected = tempStorageOption == "DOCUMENTS",
                            onClick = { tempStorageOption = "DOCUMENTS" }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Documentos (Documents/LogitNotes)")
                    }

                    // Descargas
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempStorageOption = "DOWNLOADS" }
                    ) {
                        RadioButton(
                            selected = tempStorageOption == "DOWNLOADS",
                            onClick = { tempStorageOption = "DOWNLOADS" }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Descargas (Download/LogitNotes)")
                    }

                    // Personalizada
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempStorageOption = "CUSTOM" }
                    ) {
                        RadioButton(
                            selected = tempStorageOption == "CUSTOM",
                            onClick = { tempStorageOption = "CUSTOM" }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ruta Personalizada")
                    }

                    if (tempStorageOption == "CUSTOM") {
                        OutlinedTextField(
                            value = tempCustomPath,
                            onValueChange = { tempCustomPath = it },
                            label = { Text("Ruta completa de la carpeta") },
                            placeholder = { Text("/storage/emulated/0/MisNotas") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Interno
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempStorageOption = "SANDBOX" }
                    ) {
                        RadioButton(
                            selected = tempStorageOption == "SANDBOX",
                            onClick = { tempStorageOption = "SANDBOX" }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Almacenamiento Interno Protegido")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setVaultStorageLocation(
                            tempStorageOption,
                            if (tempStorageOption == "CUSTOM") tempCustomPath else null
                        )
                        showChangePathDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryDark)
                ) {
                    Text("Guardar y Mover")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePathDialog = false }) {
                    Text("Cancelar", color = TextSecondaryMuted)
                }
            }
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TextPrimaryDark
        )
    }
}

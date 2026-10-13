package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricHelper
import com.example.ui.VaultViewModel
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.BorderSubtleWarm
import com.example.ui.theme.CanvasBgWarm
import com.example.ui.theme.FolderAmber
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceCardWhite
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted

@Composable
fun LockScreen(
    viewModel: VaultViewModel,
    activity: FragmentActivity?
) {
    val isInitialized by viewModel.isVaultInitialized.collectAsState()
    val unlockError by viewModel.unlockError.collectAsState()
    val vaultOwnerName by viewModel.vaultOwnerName.collectAsState()

    var vaultNameInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var enableBiometrics by remember { mutableStateOf(viewModel.isBiometricAvailable) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Auto-prompt biometrics if initialized and enabled
    LaunchedEffect(isInitialized, viewModel.isBiometricEnabled) {
        if (isInitialized && viewModel.isBiometricAvailable && viewModel.isBiometricEnabled && activity != null) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Desbloquear LogitNotes",
                subtitle = "Usa tu huella dactilar o reconocimiento facial",
                onSuccess = { viewModel.unlockWithBiometric() },
                onError = { /* fallback to password */ }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBgWarm)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // LogitNotes Minimalist App Icon Logo
            Surface(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = CanvasBgWarm,
                border = BorderStroke(1.dp, BorderSubtleWarm),
                shadowElevation = 2.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.eco_minimal_gem_icon_1791895721804),
                    contentDescription = "LogitNotes Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "L O G I T N O T E S",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 4.sp,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryMuted
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isInitialized) vaultOwnerName else "Crear Nueva Bóveda",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 32.sp,
                    color = TextPrimaryDark
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isInitialized)
                    "Bóveda 100% local y cifrada. Identifícate con huella dactilar, rostro o contraseña."
                else
                    "Configura el nombre, almacenamiento local y tu contraseña personal (sin contraseñas por defecto).",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = TextSecondaryMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = SurfaceCardWhite,
                border = BorderStroke(1.dp, BorderSubtleWarm)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isInitialized) {
                        // UNLOCK SCREEN (Fingerprint / Face / Password)
                        if (viewModel.isBiometricAvailable && viewModel.isBiometricEnabled && activity != null) {
                            Button(
                                onClick = {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        title = "Desbloquear Bóveda",
                                        subtitle = "Usa tu huella dactilar o reconocimiento facial",
                                        onSuccess = { viewModel.unlockWithBiometric() },
                                        onError = { /* fallback to password */ }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("biometric_unlock_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TextPrimaryDark,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Desbloquear con Huella / Cara",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderSubtleWarm))
                                Text(
                                    text = "  o con contraseña  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryMuted
                                )
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderSubtleWarm))
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Password Input
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                viewModel.clearUnlockError()
                            },
                            label = { Text("Contraseña Maestra", color = TextSecondaryMuted) },
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null, tint = TextSecondaryMuted)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Mostrar contraseña",
                                        tint = TextSecondaryMuted
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (password.isNotEmpty()) {
                                        viewModel.unlockWithPassword(password)
                                    }
                                }
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TextPrimaryDark,
                                unfocusedBorderColor = BorderSubtleWarm
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.unlockWithPassword(password) },
                            enabled = password.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("unlock_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isBiometricEnabled) CanvasBgWarm else TextPrimaryDark,
                                contentColor = if (viewModel.isBiometricEnabled) TextPrimaryDark else Color.White
                            )
                        ) {
                            Text(
                                text = "Abrir Bóveda",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        // ==========================================
                        // ONBOARDING: CREACIÓN DE LA BÓVEDA
                        // ==========================================

                        // 1. Nombre de la Bóveda
                        OutlinedTextField(
                            value = vaultNameInput,
                            onValueChange = { vaultNameInput = it },
                            label = { Text("Nombre de la Bóveda", color = TextSecondaryMuted) },
                            placeholder = { Text("Ej. David, Mi Bóveda Secreta...") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = TextSecondaryMuted)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_vault_name_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TextPrimaryDark,
                                unfocusedBorderColor = BorderSubtleWarm
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Almacenamiento Interno Privado (Único modo seguro)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = CanvasBgWarm,
                            border = BorderStroke(1.dp, BorderSubtleWarm)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TextPrimaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Almacenamiento Interno Seguro",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "Bóveda privada aislada dentro de la app (Máxima protección)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Contraseña Maestra
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                viewModel.clearUnlockError()
                            },
                            label = { Text("Contraseña Maestra (Mín. 4 caracteres)", color = TextSecondaryMuted) },
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null, tint = TextSecondaryMuted)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Mostrar contraseña",
                                        tint = TextSecondaryMuted
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_password_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TextPrimaryDark,
                                unfocusedBorderColor = BorderSubtleWarm
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4. Confirmar Contraseña
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                viewModel.clearUnlockError()
                            },
                            label = { Text("Confirmar Contraseña Maestra", color = TextSecondaryMuted) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondaryMuted)
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_password_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TextPrimaryDark,
                                unfocusedBorderColor = BorderSubtleWarm
                            )
                        )

                        // Password status feedback
                        if (password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (password.length < 4) {
                                    Text(
                                        text = "⚠️ Mínimo 4 caracteres",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FolderAmber
                                    )
                                } else {
                                    Text(
                                        text = "✓ Longitud válida",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryMuted
                                    )
                                }

                                if (confirmPassword.isNotEmpty()) {
                                    if (password == confirmPassword) {
                                        Text(
                                            text = "✓ Contraseñas coinciden",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = com.example.ui.theme.EmeraldSecurity
                                        )
                                    } else {
                                        Text(
                                            text = "✕ No coinciden",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RoseError
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 Elige tu propia contraseña personal. No hay claves por defecto.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondaryMuted,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 5. Switch Biometría (Huella o Rostro)
                        if (viewModel.isBiometricAvailable) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CanvasBgWarm)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = TextPrimaryDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Huella Dactilar o Reconocimiento Facial",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = "Desbloqueo biométrico rápido y seguro",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondaryMuted
                                        )
                                    }
                                }
                                Switch(
                                    checked = enableBiometrics,
                                    onCheckedChange = { enableBiometrics = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TextPrimaryDark,
                                        checkedTrackColor = FolderAmber
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Botón Crear Bóveda
                        Button(
                            onClick = {
                                if (password == confirmPassword && password.length >= 4) {
                                    val name = vaultNameInput.ifBlank { "Mi Bóveda" }
                                    viewModel.setupVault(
                                        name = name,
                                        password = password,
                                        enableBiometric = enableBiometrics,
                                        storageType = "SANDBOX"
                                    )
                                }
                            },
                            enabled = password.isNotBlank() && password == confirmPassword && password.length >= 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("create_vault_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextPrimaryDark,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Crear Bóveda Cifrada",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    // Error Message
                    AnimatedVisibility(visible = unlockError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = unlockError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = RoseError),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Cifrado Militar AES-256-GCM y PBKDF2 (100k iteraciones)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryMuted
            )
        }
    }
}

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WarmLightColorScheme = lightColorScheme(
    primary = TextPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = FolderAmberLight,
    onPrimaryContainer = AccentGold,
    secondary = EmeraldSecurity,
    onSecondary = Color.White,
    tertiary = FolderAmber,
    background = CanvasBgWarm,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardWhite,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCardDashed,
    onSurfaceVariant = TextSecondaryMuted,
    outline = BorderSubtleWarm,
    outlineVariant = BorderDashedWarm,
    error = RoseError
)

private val WarmDarkColorScheme = darkColorScheme(
    primary = TextPrimaryLight,
    onPrimary = CanvasBgDark,
    primaryContainer = Color(0xFF2C2518),
    onPrimaryContainer = FolderAmber,
    secondary = EmeraldSecurity,
    onSecondary = Color.White,
    tertiary = FolderAmber,
    background = CanvasBgDark,
    onBackground = TextPrimaryLight,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFF2A2825),
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderDark,
    outlineVariant = Color(0xFF403C36),
    error = RoseError
)

@Composable
fun VaultNotesTheme(
    darkTheme: Boolean = false, // Warm minimal ivory light mode as shown in screenshot
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) WarmDarkColorScheme else WarmLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) = VaultNotesTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

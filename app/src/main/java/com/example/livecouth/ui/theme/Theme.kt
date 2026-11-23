package com.example.livecouth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Aqua,
    onPrimary = Night,
    primaryContainer = OceanBlueDark,
    onPrimaryContainer = Color(0xFFE7FFFA),
    secondary = Coral,
    onSecondary = Night,
    secondaryContainer = Color(0xFF553D38),
    onSecondaryContainer = Color(0xFFFFE5DE),
    tertiary = Sand,
    background = Night,
    onBackground = Color(0xFFE7F1EF),
    surface = NightSurface,
    onSurface = Color(0xFFE7F1EF),
    surfaceVariant = Color(0xFF1A464C),
    onSurfaceVariant = Color(0xFFBBD0CD),
    outline = Color(0xFF78948F),
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = Seafoam,
    onPrimaryContainer = OceanBlueDark,
    secondary = Coral,
    onSecondary = Ink,
    secondaryContainer = Sand,
    onSecondaryContainer = Ink,
    tertiary = OceanBlueDark,
    onTertiary = Color.White,
    tertiaryContainer = PaleBlue,
    onTertiaryContainer = Ink,
    background = Color(0xFFF7FBFA),
    onBackground = Ink,
    surface = Color(0xFFFFFFFF),
    onSurface = Ink,
    surfaceVariant = PaleBlue,
    onSurfaceVariant = Slate,
    outline = Color(0xFFB5CAC6),
)

@Composable
fun LivecouthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
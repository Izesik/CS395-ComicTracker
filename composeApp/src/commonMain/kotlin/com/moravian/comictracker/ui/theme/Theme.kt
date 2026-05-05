package com.moravian.comictracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Amber = Color(0xFFFFB300)
val AmberDim = Color(0xFF3D2E00)
val AmberSubtle = Color(0xFF1F1800)

val ColorBackground = Color(0xFF0F0F0F)
val ColorSurface = Color(0xFF1A1A1A)
val ColorSurfaceVariant = Color(0xFF252525)
val ColorSurfaceElevated = Color(0xFF2A2A2A)

val ColorNavBar = Color(0xFF141414)

val ColorOnBackground = Color(0xFFEEEEEE)
val ColorOnSurface = Color(0xFFEEEEEE)
val ColorOnSurfaceMuted = Color(0xFF777777)

private val DarkColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1A0F00),
    primaryContainer = AmberDim,
    onPrimaryContainer = Color(0xFFFFDF99),
    secondary = Color(0xFFBBAA88),
    onSecondary = Color(0xFF1A1500),
    secondaryContainer = Color(0xFF2A2010),
    onSecondaryContainer = Color(0xFFE0CFA0),
    background = ColorBackground,
    onBackground = ColorOnBackground,
    surface = ColorSurface,
    onSurface = ColorOnSurface,
    surfaceVariant = ColorSurfaceVariant,
    onSurfaceVariant = ColorOnSurfaceMuted,
    outline = Color(0xFF3A3A3A),
    error = Color(0xFFCF6679),
    onError = Color(0xFF1A0010),
)

@Composable
fun ComicTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

package com.sumocarrito.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF10B981); val Amber = Color(0xFFF59E0B); val Red = Color(0xFFEF4444)

val LocalHighContrast = staticCompositionLocalOf { false }

data class StatusPalette(val green: Color, val amber: Color, val red: Color)

@Composable
fun statusPalette(): StatusPalette =
    if (LocalHighContrast.current) StatusPalette(Color(0xFF00FF66), Color(0xFFFFEA00), Color(0xFFFF4D4D))
    else StatusPalette(Green, Amber, Red)

private val Light = lightColorScheme(primary = Color(0xFF0F9D58), background = Color.White, surface = Color.White,
    onPrimary = Color.White, onBackground = Color(0xFF111111), onSurface = Color(0xFF111111), error = Red)
private val Dark = darkColorScheme(primary = Green, background = Color(0xFF121212), surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black, onBackground = Color.White, onSurface = Color.White, error = Red)
/** Negro puro + blanco + amarillo: contraste >= 15:1 (WCAG AAA) para supermercados con mala luz o sol. */
private val HighContrast = darkColorScheme(primary = Color(0xFFFFEA00), onPrimary = Color.Black,
    background = Color.Black, onBackground = Color.White, surface = Color.Black, onSurface = Color.White,
    surfaceVariant = Color(0xFF111111), onSurfaceVariant = Color.White, outline = Color.White,
    secondaryContainer = Color(0xFF333300), onSecondaryContainer = Color.White,
    surfaceContainerLow = Color.Black, surfaceContainerHigh = Color(0xFF111111), surfaceContainerHighest = Color(0xFF111111),
    error = Color(0xFFFF4D4D))

@Composable
fun SumoTheme(highContrast: Boolean = false, content: @Composable () -> Unit) {
    val scheme = when { highContrast -> HighContrast; isSystemInDarkTheme() -> Dark; else -> Light }
    CompositionLocalProvider(LocalHighContrast provides highContrast) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

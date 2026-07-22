package com.corporacionronceros.fieldsync.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = FieldSyncBlue,
    onPrimary = Color.White,
    primaryContainer = FieldSyncBlueContainer,
    onPrimaryContainer = FieldSyncOnBlueContainer,
    secondary = FieldSyncBlueDark,
    onSecondary = Color.White,
    background = FieldSyncBackground,
    onBackground = FieldSyncOnSurface,
    surface = FieldSyncSurface,
    onSurface = FieldSyncOnSurface,
    surfaceVariant = FieldSyncSurfaceVariant,
    onSurfaceVariant = FieldSyncOnSurfaceVariant,
    outline = FieldSyncOutline,
    error = FieldSyncError,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FA6FF),
    onPrimary = Color(0xFF00204D),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = FieldSyncBlueContainer,
    secondary = Color(0xFF93B4FF),
    onSecondary = Color(0xFF00204D),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111A2E),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

/** Tema de marca de FieldSync: cobalto + esquinas suaves, coherente con el resto del ecosistema. */
@Composable
fun FieldSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = FieldSyncShapes,
        content = content
    )
}

package com.example.uade.rememberapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema oscuro del diseño. Los "surfaceContainer*" son los que usan por defecto Card,
 * chips y barras de Material3, por eso se pisan con el color de las cards.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    background = Background,
    onBackground = OnSurface,
    surface = Background,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLowest = Background,
    surfaceContainerLow = CardSurface,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = SurfaceVariant,
    surfaceContainerHighest = SurfaceVariant,
    outline = Outline,
    outlineVariant = Outline,
)

/**
 * Por ahora la app es siempre oscura y sin color dinámico: con dynamic color, Android 12+
 * reemplaza la paleta del diseño por la del fondo de pantalla del usuario.
 *
 * TODO: esquema claro cuando esté diseñado; ahí volver a usar isSystemInDarkTheme().
 */
@Composable
fun RememberAppTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}

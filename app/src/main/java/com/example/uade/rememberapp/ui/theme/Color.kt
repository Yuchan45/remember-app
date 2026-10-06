package com.example.uade.rememberapp.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta oscura del diseño de Figma ("Hey! Remember App").
// TODO: los valores están estimados de una captura; reemplazarlos por los hex exactos del Figma.

// Fondo de la app: degradé de arriba (BackgroundTop) hacia abajo (Background).
val Background = Color(0xFF0B141B)
val BackgroundTop = Color(0xFF13232E)

// Cards, chips y bordes.
val CardSurface = Color(0xFF1E2C36)
val SurfaceVariant = Color(0xFF2A3944)
val Outline = Color(0xFF3B4B56)

// Celeste de acento: títulos de sección, links ("Al llegar a Casa").
val Primary = Color(0xFF9CD3F7)
val OnPrimary = Color(0xFF0B141B)
val PrimaryContainer = Color(0xFF1F5B80)
val OnPrimaryContainer = Color(0xFFD6ECFB)

val OnSurface = Color(0xFFE6EDF2)
val OnSurfaceVariant = Color(0xFF9AA8B2)

// Colores propios que no tienen lugar en el ColorScheme de Material.

/** El "!" de un aviso cercano o vencido. */
val DueSoonAmber = Color(0xFFF2C94C)

/** Barra de navegación inferior flotante y su destino seleccionado. */
val NavBarContainer = Color(0xFFB9E3FF)
val NavBarSelected = Color(0xFF0F1B23)
val NavBarContent = Color(0xFF0F1B23)
val NavBarOnSelected = Color(0xFFFFFFFF)

/** Velo oscuro sobre la foto de fondo de una card, para que el texto se lea. */
val PhotoScrim = Color(0xB3000000)

// Fondo de la foto de un recordatorio (placeholder hasta cargar la foto real).
val PhotoBackground = Color(0xFF6B4A3A)

// Menú "Crear": color de fondo del ícono de cada tipo de recordatorio, y el del ícono encima.
// TODO: los valores están estimados de una captura; reemplazarlos por los del Figma.
val CreateNoteColor = Color(0xFFA8D8F8)
val CreateChecklistColor = Color(0xFFA78BFA)
val CreateAudioColor = Color(0xFF6FD08C)
val CreateLongTextColor = Color(0xFFF4A28C)
val CreateScheduledMessageColor = Color(0xFF4DE0E0)
val CreateIconContent = Color(0xFF0F1B23)

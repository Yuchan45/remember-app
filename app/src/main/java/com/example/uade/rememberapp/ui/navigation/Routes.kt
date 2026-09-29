package com.example.uade.rememberapp.ui.navigation

/**
 * Rutas de navegación. Son strings simples (no rutas type-safe) para no sumar el plugin de
 * kotlinx-serialization. Una ruta con argumentos se escribe como `"reminders/{id}"`.
 */
object Routes {
    const val REMINDERS_LIST = "reminders"
    const val AUDIOS = "audios"
    const val PLACES_LIST = "places"
    const val SETTINGS = "settings"
}

package com.example.uade.rememberapp.ui.navigation

/**
 * Rutas de navegación. Son strings simples (no rutas type-safe) para no sumar el plugin de
 * kotlinx-serialization. Una ruta con argumentos se escribe como `"reminders/{id}"`.
 *
 * Los destinos principales (Inicio, Archivo, Lugares, Ajustes) no tienen ruta propia: son
 * páginas de [MainTabs], que vive en [MAIN]. Acá van las pantallas que se abren encima,
 * como el detalle de un recordatorio.
 */
object Routes {
    const val MAIN = "main"
}

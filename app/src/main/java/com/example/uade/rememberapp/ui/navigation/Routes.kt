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

    /** Nombre del argumento con el id del recordatorio; el ViewModel lo lee con este nombre. */
    const val ARG_REMINDER_ID = "id"

    /** Detalle de un recordatorio. Para navegar, usar [reminderDetail]. */
    const val REMINDER_DETAIL = "reminders/{$ARG_REMINDER_ID}"

    /** La ruta del detalle con el id ya puesto, ej. "reminders/7". */
    fun reminderDetail(id: Long): String = "reminders/$id"

    /**
     * Nota nueva ("+" → "Nota"): la misma pantalla que el detalle, pero vacía y sin id.
     * No es "reminders/new" porque chocaría con "reminders/{id}" (el id es un número).
     */
    const val NEW_NOTE = "notes/new"
}

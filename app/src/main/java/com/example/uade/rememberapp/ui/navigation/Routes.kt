package com.example.uade.rememberapp.ui.navigation

import android.net.Uri
import com.example.uade.rememberapp.ui.reminders.model.ReminderDetailSection

/**
 * Rutas de navegación. Son strings simples (no rutas type-safe) para no sumar el plugin de
 * kotlinx-serialization. Una ruta con argumentos se escribe como `"reminders/{id}"`.
 *
 * Los destinos principales (Inicio, Historial, Lugares, Ajustes) no tienen ruta propia: son
 * páginas de [MainTabs], que vive en [MAIN]. Acá van las pantallas que se abren encima,
 * como el detalle de un recordatorio.
 */
object Routes {
    const val MAIN = "main"

    /** Nombre del argumento con el id del recordatorio; el ViewModel lo lee con este nombre. */
    const val ARG_REMINDER_ID = "id"

    /** Argumento opcional de [NEW_NOTE]: si es true, la nota arranca con una lista vacía. */
    const val ARG_WITH_LIST = "withList"

    /** Argumento opcional de [NEW_NOTE]: el título ya escrito (ej. en la nota rápida). */
    const val ARG_TITLE = "title"

    /**
     * Argumento opcional del detalle: el nombre de una ReminderDetailSection cuyo modal se abre
     * apenas carga (ej. tocar la hora en la card abre "Establecer recordatorio").
     */
    const val ARG_OPEN_SECTION = "open"

    /** Detalle de un recordatorio. Para navegar, usar [reminderDetail]. */
    const val REMINDER_DETAIL = "reminders/{$ARG_REMINDER_ID}?$ARG_OPEN_SECTION={$ARG_OPEN_SECTION}"

    /**
     * La ruta del detalle con el id ya puesto, ej. "reminders/7"; con [open], además abre ese
     * modal: "reminders/7?open=Time".
     */
    fun reminderDetail(id: Long, open: ReminderDetailSection? = null): String =
        "reminders/$id" + open?.let { "?$ARG_OPEN_SECTION=${it.name}" }.orEmpty()

    /**
     * Nota nueva ("+" → "Nota"): la misma pantalla que el detalle, pero vacía y sin id.
     * No es "reminders/new" porque chocaría con "reminders/{id}" (el id es un número).
     *
     * Los argumentos van como query (`?a=b`) para que sean opcionales: "notes/new" sola también
     * coincide. Para navegar, usar [newNote].
     */
    const val NEW_NOTE = "notes/new?$ARG_WITH_LIST={$ARG_WITH_LIST}&$ARG_TITLE={$ARG_TITLE}"

    /**
     * La ruta de una nota nueva. Con [withList], abre con la lista lista para escribir el primer
     * ítem; [title] se codifica porque puede tener espacios, "&" o "?".
     */
    fun newNote(withList: Boolean = false, title: String? = null): String =
        "notes/new?$ARG_WITH_LIST=$withList" + title?.takeIf { it.isNotBlank() }
            ?.let { "&$ARG_TITLE=${Uri.encode(it)}" }.orEmpty()
}

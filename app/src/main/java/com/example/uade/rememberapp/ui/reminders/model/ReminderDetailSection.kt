package com.example.uade.rememberapp.ui.reminders.model

/**
 * Una parte del recordatorio que se puede tocar directo en la card de la Home (el chip de la
 * hora, el del lugar o las etiquetas). Tocarla abre el detalle con el modal de esa parte ya
 * abierto, para editarla sin pasos de más.
 *
 * Viaja en la ruta del detalle por nombre (ver Routes.reminderDetail), así que renombrar un valor
 * rompe los links que ya estén armados; agregar valores nuevos no.
 */
enum class ReminderDetailSection {
    /** "Establecer recordatorio": los avisos por fecha y hora. */
    Time,

    /** "Ubicación": el aviso al llegar o salir de un lugar. */
    Place,

    /** "Etiquetas". */
    Tags,
}

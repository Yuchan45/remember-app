package com.example.uade.rememberapp.domain.model

/**
 * Una foto del recordatorio, tal como se eligió (sin recortar). [path] es el archivo dentro de la
 * app (no la URI de la galería: esa se puede dejar de poder leer).
 *
 * [isCover] marca la que se eligió de portada; el recorte de esa foto es [Reminder.photoPath].
 */
data class ReminderPhoto(
    val path: String,
    val isCover: Boolean = false,
)

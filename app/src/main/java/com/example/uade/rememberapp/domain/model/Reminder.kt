package com.example.uade.rememberapp.domain.model

import java.time.Instant

/**
 * Un pendiente capturado por el usuario. Puede ser una nota o una lista; fuera de los ítems
 * de la lista, los dos tipos comparten todo: título, descripción, foto, avisos y etiquetas.
 *
 * Los avisos pueden ser varios y combinarse: a ciertas horas ([alarms]) y/o al llegar o salir
 * de ciertos lugares ([places]). Sin ninguno, el recordatorio no avisa.
 *
 * Fotos: [photos] son las elegidas (hasta [MAX_PHOTOS]) y [photoPath] es la portada ya recortada,
 * la que se muestra en la card y en el detalle.
 *
 * Tiene que tener al menos uno de [title], [description] o [photoPath].
 */
data class Reminder(
    val id: Long = 0,
    val type: ReminderType = ReminderType.Note,
    val title: String? = null,
    val description: String? = null,
    /** La portada recortada (de la foto de [photos] con isCover), o null si no tiene. */
    val photoPath: String? = null,
    val photos: List<ReminderPhoto> = emptyList(),
    /** Solo se usa en [ReminderType.Checklist]; en una nota queda vacía. */
    val items: List<ChecklistItem> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val alarms: List<Alarm> = emptyList(),
    val places: List<PlaceAlert> = emptyList(),
    val importance: Importance = Importance.Default,
    /** Cuánto muestra la notificación con el teléfono bloqueado. */
    val visibility: Visibility = Visibility.Default,
    /** La acción rápida de la notificación, si tiene (ej. llamar a un número). */
    val action: NotificationAction? = null,
    val isDone: Boolean = false,
    /** Dónde está: en la lista principal, archivado o en la papelera. */
    val status: ReminderStatus = ReminderStatus.Active,
    val createdAt: Instant,
) {
    init {
        require(!title.isNullOrBlank() || !description.isNullOrBlank() || photoPath != null) {
            "Un recordatorio necesita título, descripción o foto"
        }
    }
}

/** Cuántas fotos puede tener un recordatorio. */
const val MAX_PHOTOS = 15

/** Qué tipo de recordatorio es. Cambia el formulario de creación y cómo se dibuja el cuerpo. */
enum class ReminderType {
    Note,
    Checklist,
}

/**
 * Estado del recordatorio respecto de la lista principal. Archivar y eliminar lo sacan de la
 * Home y lo mandan a la sección Archivo, desde donde se puede recuperar.
 */
enum class ReminderStatus {
    /** En la lista principal (Home). */
    Active,

    /** Guardado aparte: ya no molesta en la Home pero no se pierde. */
    Archived,

    /** En la papelera. TODO: definir si se borra definitivamente después de un tiempo. */
    Deleted,
}

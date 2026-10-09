package com.example.uade.rememberapp.domain.model

import java.time.Instant

/**
 * Un pendiente capturado por el usuario. Puede ser una nota o una lista; fuera de los ítems
 * de la lista, los dos tipos comparten todo: título, descripción, foto, aviso y etiquetas.
 *
 * Tiene que tener al menos uno de [title], [description] o [photoPath].
 */
data class Reminder(
    val id: Long = 0,
    val type: ReminderType = ReminderType.Note,
    val title: String? = null,
    val description: String? = null,
    val photoPath: String? = null,
    val audioPath: String? = null,
    /** Solo se usa en [ReminderType.Checklist]; en una nota queda vacía. */
    val items: List<ChecklistItem> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val trigger: Trigger = Trigger.None,
    val isDone: Boolean = false,
    /** Dónde está: en la lista principal, archivado o en la papelera. */
    val status: ReminderStatus = ReminderStatus.Active,
    val aiStatus: AiProcessingStatus = AiProcessingStatus.None,
    val createdAt: Instant,
) {
    init {
        require(!title.isNullOrBlank() || !description.isNullOrBlank() || photoPath != null || audioPath != null) {
            "Un recordatorio necesita título, descripción, foto o audio"
        }
    }
}

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

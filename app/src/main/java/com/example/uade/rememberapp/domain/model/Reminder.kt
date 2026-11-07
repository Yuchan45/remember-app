package com.example.uade.rememberapp.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Un pendiente capturado por el usuario: una nota con texto y, opcionalmente, una lista de
 * ítems ([items]). Además tiene título, foto, avisos y etiquetas.
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
    val audioPath: String? = null,
    /** La lista de la nota; vacía si no tiene. */
    val items: List<ChecklistItem> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val alarms: List<Alarm> = emptyList(),
    val places: List<PlaceAlert> = emptyList(),
    val importance: Importance = Importance.Default,
    /** La acción rápida de la notificación, si tiene (ej. llamar a un número). */
    val action: NotificationAction? = null,
    val isDone: Boolean = false,
    /** Dónde está: en la lista principal, archivado o en la papelera. */
    val status: ReminderStatus = ReminderStatus.Active,
    /**
     * Desde cuándo está hecho, archivado o eliminado; null mientras está activo. Con esto se
     * calcula cuándo se borra solo (ver [ReminderBucket.retention]).
     */
    val statusChangedAt: Instant? = null,
    val aiStatus: AiProcessingStatus = AiProcessingStatus.None,
    val createdAt: Instant,
) {
    init {
        require(!title.isNullOrBlank() || !description.isNullOrBlank() || photoPath != null || audioPath != null || items.isNotEmpty()) {
            "Un recordatorio necesita título, descripción, ítems, foto o audio"
        }
    }

    /**
     * En qué lista se ve. Hecho no es un [ReminderStatus] propio (es [isDone] sobre uno activo),
     * pero para el usuario es otra sección: esto junta las dos cosas en un solo valor.
     */
    val bucket: ReminderBucket
        get() = when (status) {
            ReminderStatus.Active -> if (isDone) ReminderBucket.Done else ReminderBucket.Active
            ReminderStatus.Archived -> ReminderBucket.Archived
            ReminderStatus.Deleted -> ReminderBucket.Deleted
        }

    /** Cuándo se borra solo, o null si no se borra (activo o archivado, o sin fecha de cambio). */
    val expiresAt: Instant?
        get() {
            val retention = bucket.retention ?: return null
            return statusChangedAt?.plus(retention)
        }
}

/** Cuántas fotos puede tener un recordatorio. */
const val MAX_PHOTOS = 15

/**
 * Cómo se guardó: con ítems es [Checklist] y si no, [Note]. El usuario no lo elige (todo se crea
 * como nota y la lista es opcional); se guarda igual porque lo usan la IA y los datos viejos.
 */
enum class ReminderType {
    Note,
    Checklist,
}

/**
 * Estado del recordatorio respecto de la lista principal. Archivar y eliminar lo sacan de la
 * Home y lo mandan al Historial, desde donde se puede recuperar.
 */
enum class ReminderStatus {
    /** En la lista principal (Home). */
    Active,

    /** Guardado aparte: ya no molesta en la Home pero no se pierde. */
    Archived,

    /** En la papelera: se borra solo pasados unos días (ver [ReminderBucket.Deleted]). */
    Deleted,
}

/**
 * La sección donde se ve un recordatorio: la Home ([Active]) o una pestaña del Historial.
 *
 * [retention] es cuánto se guarda antes de borrarse solo (null = para siempre). Los hechos duran
 * poco porque ya cumplieron; los eliminados dan más margen para arrepentirse.
 */
enum class ReminderBucket(val retention: Duration?) {
    Active(null),
    Done(Duration.ofDays(7)),
    Archived(null),
    Deleted(Duration.ofDays(15)),
}

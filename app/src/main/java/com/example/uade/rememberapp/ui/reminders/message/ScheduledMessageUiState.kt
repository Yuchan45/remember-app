package com.example.uade.rememberapp.ui.reminders.message

import androidx.annotation.StringRes
import com.example.uade.rememberapp.domain.model.AlarmRepeat
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderPhoto
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.ui.reminders.detail.AlarmsEditor
import com.example.uade.rememberapp.ui.reminders.detail.PhotosEditor
import com.example.uade.rememberapp.ui.reminders.detail.PlaceEditor
import java.time.Instant

/**
 * Estado inmutable de la pantalla de creación o edición de un mensaje programado.
 */
data class ScheduledMessageUiState(
    val reminderId: Long? = null,
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val originalReminder: Reminder? = null,

    // Título personalizado del recordatorio
    val title: String = "",

    // Canal y destinatario
    val channel: ScheduledMessageChannel = ScheduledMessageChannel.WhatsApp,
    val recipient: String = "",

    // Contenido
    val subject: String = "",
    val messageText: String = "",

    // Fotos e imágenes
    val photos: List<ReminderPhoto> = emptyList(),
    val photoPath: String? = null,
    val photosEditor: PhotosEditor? = null,

    // Avisos por hora (Alarms)
    val alarms: List<Instant> = emptyList(),
    val alarmRepeats: Map<Instant, AlarmRepeat> = emptyMap(),
    val alarmsEditor: AlarmsEditor? = null,

    // Avisos por ubicación (Places)
    val places: List<PlaceAlert> = emptyList(),
    val availablePlaces: List<Place> = emptyList(),
    val placeEditor: PlaceEditor? = null,

    // Comportamiento / Importancia
    val importance: Importance = Importance.High,
    val importanceEditor: Importance? = null,

    // Etiquetas
    val selectedTagIds: Set<Long> = emptySet(),
    val allTags: List<Tag> = emptyList(),
    val isTagsSheetOpen: Boolean = false,
    val tagsDraft: Set<Long> = emptySet(),

    // Finalización
    val isSaved: Boolean = false,
    @get:StringRes val errorMessage: Int? = null,
) {
    /**
     * Fotos apiladas: primero la portada y luego las demás sin repetir la portada.
     */
    val stackPhotos: List<String>
        get() = if (photoPath != null) {
            listOf(photoPath) + photos.filterNot { it.isCover }.map { it.path }
        } else {
            photos.map { it.path }
        }

    /**
     * Valida que se hayan completado los campos requeridos para poder programar.
     */
    val canSave: Boolean
        get() = recipient.isNotBlank() &&
            messageText.isNotBlank() &&
            (alarms.isNotEmpty() || places.isNotEmpty())

    /**
     * Primer lugar asignado, si existe.
     */
    val firstPlace: PlaceAlert?
        get() = places.firstOrNull()

    /**
     * Nombre del primer lugar seleccionado, si existe.
     */
    val placeName: String?
        get() = firstPlace?.let { alert -> availablePlaces.firstOrNull { it.id == alert.placeId }?.name }

    /**
     * Lista de etiquetas asignadas actualmente.
     */
    val tags: List<Tag>
        get() = allTags.filter { it.id in selectedTagIds }

    /**
     * Devuelve el objeto [NotificationAction] configurado.
     */
    fun toNotificationAction(): NotificationAction = when (channel) {
        ScheduledMessageChannel.WhatsApp -> NotificationAction.Message(
            channel = MessageChannel.WhatsApp,
            to = recipient.trim(),
            text = messageText.trim(),
        )
        ScheduledMessageChannel.Sms -> NotificationAction.Message(
            channel = MessageChannel.Sms,
            to = recipient.trim(),
            text = messageText.trim(),
        )
        ScheduledMessageChannel.Email -> NotificationAction.Email(
            to = recipient.trim(),
            subject = subject.trim().takeIf { it.isNotBlank() },
        )
    }
}

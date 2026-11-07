package com.example.uade.rememberapp.ui.reminders.message

import androidx.annotation.StringRes
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import java.time.LocalDate
import java.time.LocalTime

/**
 * Estado inmutable de la pantalla de creación o edición de un mensaje programado.
 */
data class ScheduledMessageUiState(
    val reminderId: Long? = null,
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val originalReminder: Reminder? = null,

    // Canal y destinatario
    val channel: ScheduledMessageChannel = ScheduledMessageChannel.WhatsApp,
    val recipient: String = "",

    // Contenido
    val subject: String = "",
    val messageText: String = "",

    // Programación
    val scheduleType: ScheduleTriggerType = ScheduleTriggerType.Time,

    // Disparador por hora
    val scheduledDate: LocalDate = LocalDate.now(),
    val scheduledTime: LocalTime = LocalTime.now().plusHours(1).withMinute(0),
    val selectedTimeShortcut: TimeShortcut? = null,

    // Disparador por ubicación
    val selectedPlaceId: Long? = null,
    val placeEvent: PlaceEvent = PlaceEvent.Arrive,
    val availablePlaces: List<Place> = emptyList(),

    // Diálogos abiertos
    val isDatePickerOpen: Boolean = false,
    val isTimePickerOpen: Boolean = false,

    // Etiquetas
    val selectedTagIds: Set<Long> = emptySet(),
    val allTags: List<Tag> = emptyList(),

    // Finalización
    val isSaved: Boolean = false,
    @get:StringRes val errorMessage: Int? = null,
) {
    /**
     * Valida que se hayan completado los campos requeridos para poder programar.
     */
    val canSave: Boolean
        get() = recipient.isNotBlank() &&
            messageText.isNotBlank() &&
            (scheduleType != ScheduleTriggerType.Location || selectedPlaceId != null)

    /**
     * Nombre del lugar seleccionado, si existe.
     */
    val selectedPlaceName: String?
        get() = availablePlaces.firstOrNull { it.id == selectedPlaceId }?.name

    /**
     * Devuelve el objeto [NotificationAction] configurado.
     */
    fun toNotificationAction(): NotificationAction = when (channel) {
        ScheduledMessageChannel.WhatsApp -> NotificationAction.Message(
            channel = com.example.uade.rememberapp.domain.model.MessageChannel.WhatsApp,
            to = recipient.trim(),
            text = messageText.trim(),
        )
        ScheduledMessageChannel.Sms -> NotificationAction.Message(
            channel = com.example.uade.rememberapp.domain.model.MessageChannel.Sms,
            to = recipient.trim(),
            text = messageText.trim(),
        )
        ScheduledMessageChannel.Email -> NotificationAction.Email(
            to = recipient.trim(),
            subject = subject.trim().takeIf { it.isNotBlank() },
        )
    }
}

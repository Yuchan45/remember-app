package com.example.uade.rememberapp.ui.reminders.message

import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.ReminderPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ScheduledMessageUiStateTest {

    @Test
    fun canSave_returnsFalse_whenRecipientOrMessageIsBlank() {
        val emptyState = ScheduledMessageUiState(alarms = listOf(Instant.now()))
        assertFalse(emptyState.canSave)

        val recipientOnly = emptyState.copy(recipient = "+5491112345678", messageText = "")
        assertFalse(recipientOnly.canSave)

        val messageOnly = emptyState.copy(recipient = "", messageText = "Hola mundo")
        assertFalse(messageOnly.canSave)
    }

    @Test
    fun canSave_returnsFalse_whenNoAlarmsAndNoPlaces() {
        val noTriggersState = ScheduledMessageUiState(
            recipient = "+5491112345678",
            messageText = "Hola mundo",
            alarms = emptyList(),
            places = emptyList(),
        )
        assertFalse(noTriggersState.canSave)
    }

    @Test
    fun canSave_returnsTrue_whenAlarmsPresent_andRecipientAndMessagePresent() {
        val validState = ScheduledMessageUiState(
            recipient = "+5491112345678",
            messageText = "Mensaje de prueba",
            alarms = listOf(Instant.now().plusSeconds(3600)),
        )
        assertTrue(validState.canSave)
    }

    @Test
    fun canSave_returnsTrue_whenPlacesPresent_andRecipientAndMessagePresent() {
        val validLocationState = ScheduledMessageUiState(
            recipient = "+5491112345678",
            messageText = "Aviso al llegar",
            alarms = emptyList(),
            places = listOf(PlaceAlert(placeId = 42L, event = PlaceAlertEvent.Arrive)),
        )
        assertTrue(validLocationState.canSave)
    }

    @Test
    fun customTitle_isStoredCorrectly() {
        val state = ScheduledMessageUiState(
            title = "Aviso importante de reunión",
            recipient = "juan@empresa.com",
            messageText = "Nos vemos a las 15hs",
        )
        assertEquals("Aviso importante de reunión", state.title)
    }

    @Test
    fun toNotificationAction_createsProperWhatsAppAction() {
        val state = ScheduledMessageUiState(
            channel = ScheduledMessageChannel.WhatsApp,
            recipient = "+54 9 11 1234-5678",
            messageText = "Hola por WhatsApp",
        )
        val action = state.toNotificationAction()

        assertTrue(action is NotificationAction.Message)
        val msgAction = action as NotificationAction.Message
        assertEquals(MessageChannel.WhatsApp, msgAction.channel)
        assertEquals("+54 9 11 1234-5678", msgAction.to)
        assertEquals("Hola por WhatsApp", msgAction.text)
    }

    @Test
    fun toNotificationAction_createsProperSmsAction() {
        val state = ScheduledMessageUiState(
            channel = ScheduledMessageChannel.Sms,
            recipient = "11223344",
            messageText = "Hola por SMS",
        )
        val action = state.toNotificationAction()

        assertTrue(action is NotificationAction.Message)
        val msgAction = action as NotificationAction.Message
        assertEquals(MessageChannel.Sms, msgAction.channel)
        assertEquals("11223344", msgAction.to)
        assertEquals("Hola por SMS", msgAction.text)
    }

    @Test
    fun toNotificationAction_createsProperEmailAction() {
        val state = ScheduledMessageUiState(
            channel = ScheduledMessageChannel.Email,
            recipient = "contacto@ejemplo.com",
            subject = "Reunión urgente",
            messageText = "Cuerpo del correo",
        )
        val action = state.toNotificationAction()

        assertTrue(action is NotificationAction.Email)
        val emailAction = action as NotificationAction.Email
        assertEquals("contacto@ejemplo.com", emailAction.to)
        assertEquals("Reunión urgente", emailAction.subject)
    }

    @Test
    fun placeName_returnsMatchingPlaceName() {
        val places = listOf(
            Place(id = 1L, name = "Casa", latitude = 0.0, longitude = 0.0),
            Place(id = 2L, name = "Oficina", latitude = 0.0, longitude = 0.0),
        )
        val state = ScheduledMessageUiState(
            availablePlaces = places,
            places = listOf(PlaceAlert(placeId = 2L, event = PlaceAlertEvent.Arrive)),
        )
        assertEquals("Oficina", state.placeName)
    }

    @Test
    fun stackPhotos_ordersCoverFirstAndExcludesCoverOriginal() {
        val p1 = ReminderPhoto(path = "path/to/1.jpg", isCover = false)
        val p2 = ReminderPhoto(path = "path/to/2.jpg", isCover = true)
        val p3 = ReminderPhoto(path = "path/to/3.jpg", isCover = false)

        val stateWithCover = ScheduledMessageUiState(
            photoPath = "path/to/cover_cropped.jpg",
            photos = listOf(p1, p2, p3),
        )

        assertEquals(
            listOf("path/to/cover_cropped.jpg", "path/to/1.jpg", "path/to/3.jpg"),
            stateWithCover.stackPhotos,
        )

        val stateWithoutCover = ScheduledMessageUiState(
            photoPath = null,
            photos = listOf(p1, p3),
        )

        assertEquals(
            listOf("path/to/1.jpg", "path/to/3.jpg"),
            stateWithoutCover.stackPhotos,
        )
    }
}

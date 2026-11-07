package com.example.uade.rememberapp.ui.reminders.message

import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduledMessageUiStateTest {

    @Test
    fun canSave_returnsFalse_whenRecipientOrMessageIsBlank() {
        val emptyState = ScheduledMessageUiState()
        assertFalse(emptyState.canSave)

        val recipientOnly = emptyState.copy(recipient = "+5491112345678", messageText = "")
        assertFalse(recipientOnly.canSave)

        val messageOnly = emptyState.copy(recipient = "", messageText = "Hola mundo")
        assertFalse(messageOnly.canSave)
    }

    @Test
    fun canSave_returnsTrue_whenTimeTrigger_andRecipientAndMessagePresent() {
        val validState = ScheduledMessageUiState(
            recipient = "+5491112345678",
            messageText = "Mensaje de prueba",
            scheduleType = ScheduleTriggerType.Time,
        )
        assertTrue(validState.canSave)
    }

    @Test
    fun canSave_withLocationTrigger_requiresPlaceSelection() {
        val locationWithoutPlace = ScheduledMessageUiState(
            recipient = "+5491112345678",
            messageText = "Aviso al llegar",
            scheduleType = ScheduleTriggerType.Location,
            selectedPlaceId = null,
        )
        assertFalse(locationWithoutPlace.canSave)

        val locationWithPlace = locationWithoutPlace.copy(selectedPlaceId = 42L)
        assertTrue(locationWithPlace.canSave)
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
    fun selectedPlaceName_returnsMatchingPlaceName() {
        val places = listOf(
            Place(id = 1L, name = "Casa", latitude = 0.0, longitude = 0.0),
            Place(id = 2L, name = "Oficina", latitude = 0.0, longitude = 0.0),
        )
        val state = ScheduledMessageUiState(
            availablePlaces = places,
            selectedPlaceId = 2L,
        )
        assertEquals("Oficina", state.selectedPlaceName)
    }
}

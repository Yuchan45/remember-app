package com.example.uade.rememberapp.ui.reminders.detail

import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.Reminder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReminderDetailUiStateTest {

    private val now = Instant.parse("2026-10-24T18:00:00Z")

    @Test
    fun hasUnsavedChanges_whenReminderHasAlarms_andPlaceIsAdded_returnsTrue() {
        val reminder = Reminder(
            id = 1L,
            title = "Comprar leche",
            alarms = listOf(Alarm(now.plusSeconds(3600))),
            places = emptyList(),
            createdAt = now,
        )

        val state = ReminderDetailUiState(
            isLoading = false,
            reminder = reminder,
            title = "Comprar leche",
            alarms = listOf(now.plusSeconds(3600)),
            // Se agrega ubicación mediante placesDraft
            placesDraft = listOf(PlaceAlert(placeId = 10L, event = PlaceAlertEvent.Arrive)),
        )

        assertTrue("Debe detectar cambios sin guardar al agregar ubicación a un recordatorio con horario", state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_whenReminderHasPlaces_andAlarmIsAdded_returnsTrue() {
        val reminder = Reminder(
            id = 1L,
            title = "Comprar leche",
            alarms = emptyList(),
            places = listOf(PlaceAlert(placeId = 10L, event = PlaceAlertEvent.Arrive)),
            createdAt = now,
        )

        val state = ReminderDetailUiState(
            isLoading = false,
            reminder = reminder,
            title = "Comprar leche",
            // Se agrega horario
            alarms = listOf(now.plusSeconds(3600)),
        )

        assertTrue("Debe detectar cambios sin guardar al agregar horario a un recordatorio con lugar", state.hasUnsavedChanges)
    }

    @Test
    fun hasUnsavedChanges_whenNoChanges_returnsFalse() {
        val reminder = Reminder(
            id = 1L,
            title = "Comprar leche",
            alarms = listOf(Alarm(now.plusSeconds(3600))),
            places = listOf(PlaceAlert(placeId = 10L, event = PlaceAlertEvent.Arrive)),
            createdAt = now,
        )

        val state = ReminderDetailUiState(
            isLoading = false,
            reminder = reminder,
            title = "Comprar leche",
            alarms = listOf(now.plusSeconds(3600)),
        )

        assertFalse("No debe detectar cambios si los datos coinciden con el recordatorio existente", state.hasUnsavedChanges)
    }
}

package com.example.uade.rememberapp.data.local.mapper

import com.example.uade.rememberapp.data.local.entity.AlarmEntity
import com.example.uade.rememberapp.data.local.entity.PlaceAlertEntity
import com.example.uade.rememberapp.data.local.entity.ReminderWithDetails
import com.example.uade.rememberapp.data.local.entity.TagEntity
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ReminderMapperTest {

    private val createdAt = Instant.parse("2026-09-30T12:00:00Z")

    private val reminder = Reminder(
        id = 7,
        title = "Llamar al plomero",
        status = ReminderStatus.Archived,
        createdAt = createdAt,
    )

    @Test
    fun `ida y vuelta de la fila conserva los datos propios del recordatorio`() {
        assertEquals(reminder, reminder.toEntity().toDomain())
    }

    @Test
    fun `trae del resultado de Room las etiquetas y los avisos, con las horas ordenadas`() {
        val row = ReminderWithDetails(
            reminder = reminder.toEntity(),
            tags = listOf(TagEntity(id = 1, name = "Salud", colorArgb = 0xFF6FCF97)),
            alarms = listOf(
                AlarmEntity(id = 2, reminderId = 7, atMillis = 2_000),
                AlarmEntity(id = 1, reminderId = 7, atMillis = 1_000),
            ),
            places = listOf(PlaceAlertEntity(id = 1, reminderId = 7, placeId = 3, event = "Leave")),
        )

        val domain = row.toDomain()

        assertEquals(listOf(Tag(id = 1, name = "Salud", colorArgb = 0xFF6FCF97)), domain.tags)
        assertEquals(listOf(Alarm(Instant.ofEpochMilli(1_000)), Alarm(Instant.ofEpochMilli(2_000))), domain.alarms)
        assertEquals(listOf(PlaceAlert(placeId = 3, event = PlaceAlertEvent.Leave)), domain.places)
    }

    @Test
    fun `un aviso por lugar va y vuelve igual`() {
        val alert = PlaceAlert(placeId = 3, event = PlaceAlertEvent.Leave)
        assertEquals(alert, alert.toEntity(reminderId = 7).toDomain())
    }

    @Test
    fun `un evento de lugar o un estado desconocidos no rompen la lectura`() {
        val place = PlaceAlertEntity(reminderId = 7, placeId = 3, event = "OTRO").toDomain()
        val domain = reminder.toEntity().copy(status = "Viejo").toDomain()

        assertEquals(PlaceAlertEvent.Arrive, place.event)
        assertEquals(ReminderStatus.Active, domain.status)
    }
}

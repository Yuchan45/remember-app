package com.example.uade.rememberapp.data.local.mapper

import com.example.uade.rememberapp.data.local.entity.ReminderWithTags
import com.example.uade.rememberapp.data.local.entity.TagEntity
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ReminderMapperTest {

    private val createdAt = Instant.parse("2026-09-30T12:00:00Z")

    private fun reminder(trigger: Trigger) = Reminder(
        id = 7,
        title = "Llamar al plomero",
        trigger = trigger,
        status = ReminderStatus.Archived,
        createdAt = createdAt,
    )

    @Test
    fun `ida y vuelta conserva el recordatorio con cada tipo de aviso`() {
        listOf(
            Trigger.None,
            Trigger.AtTime(Instant.parse("2026-10-01T12:00:00Z")),
            Trigger.AtPlace(placeId = 3),
        ).forEach { trigger ->
            val original = reminder(trigger)
            assertEquals(original, original.toEntity().toDomain())
        }
    }

    @Test
    fun `aplana el aviso por hora en sus columnas`() {
        val entity = reminder(Trigger.AtTime(Instant.ofEpochMilli(1_000))).toEntity()

        assertEquals(TriggerAtTime, entity.triggerType)
        assertEquals(1_000L, entity.triggerAtMillis)
        assertEquals(null, entity.triggerPlaceId)
    }

    @Test
    fun `el recordatorio con sus etiquetas de la base trae las etiquetas al dominio`() {
        val salud = TagEntity(id = 1, name = "Salud", colorArgb = 0xFF6FCF97)
        val row = ReminderWithTags(reminder = reminder(Trigger.None).toEntity(), tags = listOf(salud))

        val domain = row.toDomain()

        assertEquals(listOf(Tag(id = 1, name = "Salud", colorArgb = 0xFF6FCF97)), domain.tags)
        assertEquals("Llamar al plomero", domain.title)
    }

    @Test
    fun `un tipo de aviso o un estado desconocidos no rompen la lectura`() {
        val entity = reminder(Trigger.None).toEntity().copy(triggerType = "OTRO", status = "Viejo")

        val domain = entity.toDomain()

        assertEquals(Trigger.None, domain.trigger)
        assertEquals(ReminderStatus.Active, domain.status)
    }
}

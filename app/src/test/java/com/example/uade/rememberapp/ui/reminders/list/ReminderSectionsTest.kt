package com.example.uade.rememberapp.ui.reminders.list

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class ReminderSectionsTest {

    private val zone = ZoneOffset.UTC
    private val now = Instant.parse("2026-09-30T10:00:00Z")

    private fun note(id: Long, at: String? = null, createdAt: String = "2026-09-29T00:00:00Z") = Reminder(
        id = id,
        title = "Nota $id",
        alarms = listOfNotNull(at?.let { Alarm(Instant.parse(it)) }),
        createdAt = Instant.parse(createdAt),
    )

    private fun List<ReminderSection>.ids() = associate { section -> section.key to section.reminders.map { it.id } }

    @Test
    fun `reparte por día del aviso y ordena por hora`() {
        val sections = groupByDate(
            listOf(
                note(1, at = "2026-09-30T18:00:00Z"),
                note(2, at = "2026-09-30T11:00:00Z"),
                note(3, at = "2026-10-01T09:00:00Z"),
                note(4, at = "2026-10-05T09:00:00Z"),
                note(5),
            ),
            now,
            zone,
        )

        assertEquals(
            mapOf(
                ReminderSectionKey.Today to listOf(2L, 1L),
                ReminderSectionKey.Tomorrow to listOf(3L),
                ReminderSectionKey.Upcoming to listOf(4L),
                ReminderSectionKey.NoDate to listOf(5L),
            ),
            sections.ids(),
        )
        // Y en el orden de pantalla.
        assertEquals(ReminderSectionKey.entries.toList(), sections.map { it.key })
    }

    @Test
    fun `con varios avisos la agrupa por el próximo que no pasó`() {
        // Uno ya pasó (ayer) y el próximo es mañana: va a "Mañana", no a "Hoy".
        val reminder = Reminder(
            id = 1,
            title = "Nota",
            alarms = listOf(
                Alarm(Instant.parse("2026-09-29T09:00:00Z")),
                Alarm(Instant.parse("2026-10-01T09:00:00Z")),
            ),
            createdAt = now,
        )

        val sections = groupByDate(listOf(reminder), now, zone)

        assertEquals(mapOf(ReminderSectionKey.Tomorrow to listOf(1L)), sections.ids())
    }

    @Test
    fun `los vencidos de días anteriores quedan en hoy`() {
        val sections = groupByDate(listOf(note(1, at = "2026-09-28T09:00:00Z")), now, zone)

        assertEquals(mapOf(ReminderSectionKey.Today to listOf(1L)), sections.ids())
    }

    @Test
    fun `sin fecha va del más nuevo al más viejo, y no hay secciones vacías`() {
        val sections = groupByDate(
            listOf(
                note(1, createdAt = "2026-09-01T00:00:00Z"),
                note(2, createdAt = "2026-09-20T00:00:00Z"),
            ),
            now,
            zone,
        )

        assertEquals(mapOf(ReminderSectionKey.NoDate to listOf(2L, 1L)), sections.ids())
    }
}

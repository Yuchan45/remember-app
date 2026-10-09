package com.example.uade.rememberapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AlarmTest {

    private val now = Instant.parse("2026-10-08T10:00:00Z")

    private fun at(text: String) = Alarm(Instant.parse(text))

    private fun note(vararg alarms: Alarm) = Reminder(title = "Nota", alarms = alarms.toList(), createdAt = now)

    @Test
    fun `el próximo aviso es el más cercano que todavía no pasó`() {
        val reminder = note(at("2026-10-09T10:00:00Z"), at("2026-10-07T10:00:00Z"), at("2026-10-08T12:00:00Z"))
        assertEquals(at("2026-10-08T12:00:00Z"), reminder.nextAlarm(now))
    }

    @Test
    fun `si ya pasaron todos, devuelve el último (para marcarlo vencido)`() {
        val reminder = note(at("2026-10-06T10:00:00Z"), at("2026-10-07T10:00:00Z"))
        assertEquals(at("2026-10-07T10:00:00Z"), reminder.nextAlarm(now))
    }

    @Test
    fun `sin avisos por hora no hay próximo`() {
        assertNull(note().nextAlarm(now))
    }

    @Test
    fun `está por sonar si falta menos de una hora o ya pasó`() {
        assertTrue(at("2026-10-08T10:30:00Z").isDueSoon(now))
        assertTrue(at("2026-10-08T09:00:00Z").isDueSoon(now))
        assertFalse(at("2026-10-08T12:00:00Z").isDueSoon(now))
    }
}

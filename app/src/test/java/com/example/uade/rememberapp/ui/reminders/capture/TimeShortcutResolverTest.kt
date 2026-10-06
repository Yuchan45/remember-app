package com.example.uade.rememberapp.ui.reminders.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeShortcutResolverTest {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    // Miércoles 30/09/2026.
    private fun at(date: String, time: String): ZonedDateTime =
        LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time)).atZone(zone)

    private fun resolve(
        shortcut: TimeShortcut?,
        now: ZonedDateTime,
        pickedDate: LocalDate? = null,
        pickedTime: LocalTime? = null,
    ) = resolveReminderTime(shortcut, pickedDate, pickedTime, now)

    @Test
    fun `sin atajo elegido no hay hora`() {
        assertNull(resolve(null, at("2026-09-30", "10:00")))
    }

    @Test
    fun `en 1 hora suma una hora y descarta los segundos`() {
        val now = at("2026-09-30", "10:15").withSecond(42)
        assertEquals(at("2026-09-30", "11:15"), resolve(TimeShortcut.InOneHour, now))
    }

    @Test
    fun `hoy 21 es hoy, o mañana si ya pasaron las 21`() {
        assertEquals(at("2026-09-30", "21:00"), resolve(TimeShortcut.Tonight, at("2026-09-30", "18:00")))
        assertEquals(at("2026-10-01", "21:00"), resolve(TimeShortcut.Tonight, at("2026-09-30", "22:00")))
    }

    @Test
    fun `mañana 9 es el día siguiente a las 9`() {
        assertEquals(at("2026-10-01", "09:00"), resolve(TimeShortcut.TomorrowMorning, at("2026-09-30", "23:30")))
    }

    @Test
    fun `sábado 10 es el próximo sábado, o hoy si es sábado antes de las 10`() {
        // Miércoles → sábado 03/10.
        assertEquals(at("2026-10-03", "10:00"), resolve(TimeShortcut.Weekend, at("2026-09-30", "10:00")))
        // Sábado 9:00 → ese mismo sábado.
        assertEquals(at("2026-10-03", "10:00"), resolve(TimeShortcut.Weekend, at("2026-10-03", "09:00")))
        // Sábado 11:00 → el sábado siguiente.
        assertEquals(at("2026-10-10", "10:00"), resolve(TimeShortcut.Weekend, at("2026-10-03", "11:00")))
    }

    @Test
    fun `elegir fecha usa la hora elegida o las 9 si no se eligió`() {
        val now = at("2026-09-30", "10:00")
        val date = LocalDate.parse("2026-10-15")

        assertEquals(
            at("2026-10-15", "18:30"),
            resolve(TimeShortcut.PickDate, now, pickedDate = date, pickedTime = LocalTime.of(18, 30)),
        )
        assertEquals(at("2026-10-15", "09:00"), resolve(TimeShortcut.PickDate, now, pickedDate = date))
        assertNull(resolve(TimeShortcut.PickDate, now))
    }
}

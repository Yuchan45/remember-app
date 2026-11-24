package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.AlarmRepeat
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.nextOccurrence
import com.example.uade.rememberapp.domain.model.rolledForward
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RepeatingAlarmsTest {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    /** 2026-11-06 es viernes. */
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant()

    @Test
    fun `diario pasa al dia siguiente a la misma hora`() {
        val alarm = Alarm(at("2026-11-06T08:00"), repeat = AlarmRepeat.Daily)
        assertEquals(at("2026-11-07T08:00"), alarm.nextOccurrence(at("2026-11-06T08:00"), zone))
    }

    @Test
    fun `de lunes a viernes saltea el fin de semana`() {
        val friday = Alarm(at("2026-11-06T08:00"), repeat = AlarmRepeat.Weekdays)
        assertEquals(at("2026-11-09T08:00"), friday.nextOccurrence(at("2026-11-06T08:00"), zone))
    }

    @Test
    fun `semanal pasa a la semana siguiente`() {
        val alarm = Alarm(at("2026-11-06T08:00"), repeat = AlarmRepeat.Weekly)
        assertEquals(at("2026-11-13T08:00"), alarm.nextOccurrence(at("2026-11-06T08:00"), zone))
    }

    @Test
    fun `sin repeticion no tiene proxima`() {
        assertNull(Alarm(at("2026-11-06T08:00")).nextOccurrence(at("2026-11-06T08:00"), zone))
    }

    @Test
    fun `al correr los vencidos solo cambian los que se repiten`() {
        val now = at("2026-11-10T12:00")
        val alarms = listOf(
            Alarm(at("2026-11-06T08:00"), repeat = AlarmRepeat.Daily),
            Alarm(at("2026-11-06T09:00")),
        )

        val rolled = alarms.rolledForward(now, zone)

        assertEquals(at("2026-11-11T08:00"), rolled[0].at)
        assertEquals(at("2026-11-06T09:00"), rolled[1].at)
    }

    @Test
    fun `al sonar un aviso diario queda guardado y programado el de manana`() = runBlocking {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderScheduler()
        val fired = at("2026-11-06T08:00")
        val id = repository.save(
            Reminder(title = "Pastilla", alarms = listOf(Alarm(fired, repeat = AlarmRepeat.Daily)), createdAt = fired),
        )

        AdvanceRepeatingAlarmUseCase(repository, scheduler, now = { fired }, zone = zone)(id, fired)

        val next = at("2026-11-07T08:00")
        assertEquals(listOf(next), repository.getById(id)?.alarms?.map { it.at })
        assertEquals(AlarmRepeat.Daily, repository.getById(id)?.alarms?.single()?.repeat)
        assertEquals(listOf(next), scheduler.scheduledFor(id))
    }

    @Test
    fun `posponer despues de avanzar conserva la proxima repeticion`() = runBlocking {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderScheduler()
        val fired = at("2026-11-06T08:00")
        val id = repository.save(
            Reminder(title = "Pastilla", alarms = listOf(Alarm(fired, repeat = AlarmRepeat.Daily)), createdAt = fired),
        )

        AdvanceRepeatingAlarmUseCase(repository, scheduler, now = { fired }, zone = zone)(id, fired)
        SnoozeReminderUseCase(repository, scheduler, now = { fired })(id, firedAt = fired)

        val alarms = repository.getById(id)?.alarms.orEmpty()
        assertTrue(alarms.any { it.at == at("2026-11-07T08:00") && it.repeat == AlarmRepeat.Daily })
        assertTrue(alarms.any { it.isSnooze })
    }

    @Test
    fun `hecho desde la notificacion con lista incompleta marca todo y finaliza`() = runBlocking {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderScheduler()
        val id = repository.save(
            Reminder(
                title = "Compras",
                items = listOf(ChecklistItem(text = "Pan"), ChecklistItem(text = "Leche", isChecked = true)),
                createdAt = at("2026-11-06T08:00"),
            ),
        )

        CompleteFromNotificationUseCase(repository, MarkReminderDoneUseCase(repository, scheduler))(id)

        val reminder = repository.getById(id)!!
        assertTrue(reminder.isDone)
        assertTrue(reminder.items.all { it.isChecked })
    }

    @Test
    fun `hecho desde la notificacion en uno que se repite no corta la repeticion`() = runBlocking {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderScheduler()
        val id = repository.save(
            Reminder(
                title = "Pastilla",
                alarms = listOf(Alarm(at("2026-11-07T08:00"), repeat = AlarmRepeat.Daily)),
                createdAt = at("2026-11-06T08:00"),
            ),
        )

        CompleteFromNotificationUseCase(repository, MarkReminderDoneUseCase(repository, scheduler))(id)

        assertFalse(repository.getById(id)!!.isDone)
    }
}

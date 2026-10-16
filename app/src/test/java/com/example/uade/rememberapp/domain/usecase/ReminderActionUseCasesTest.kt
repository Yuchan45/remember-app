package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.Reminder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReminderActionUseCasesTest {

    private val createdAt = Instant.parse("2026-10-03T10:00:00Z")
    private fun at(time: String): Instant = Instant.parse("2026-10-03T${time}:00Z")

    private val repository = FakeReminderRepository()
    private val scheduler = FakeReminderScheduler()

    /** Guarda el recordatorio y programa sus avisos, como lo haría SaveReminderUseCase. */
    private suspend fun saveWithAlarms(vararg alarms: Alarm): Long {
        val id = repository.save(Reminder(title = "Reunión", alarms = alarms.toList(), createdAt = createdAt))
        alarms.forEach { scheduler.schedule(id, it.at) }
        return id
    }

    @Test
    fun `marcar como hecho actualiza el recordatorio y cancela todas sus alarmas`() = runBlocking {
        val id = saveWithAlarms(Alarm(at("15:00")), Alarm(at("16:00")))

        MarkReminderDoneUseCase(repository, scheduler)(id)

        assertTrue(repository.getById(id)!!.isDone)
        assertEquals(emptyList<Instant>(), scheduler.scheduledFor(id))
    }

    @Test
    fun `posponer reemplaza el aviso que sonó y los anteriores, y conserva los siguientes`() = runBlocking {
        val id = saveWithAlarms(Alarm(at("14:00")), Alarm(at("15:00")), Alarm(at("16:00")))
        val snooze = SnoozeReminderUseCase(repository, scheduler, now = { at("15:00") })

        snooze(id, firedAt = at("15:00"), minutes = 15)

        val expected = listOf(Alarm(at("15:15"), isSnooze = true), Alarm(at("16:00")))
        assertEquals(expected, repository.getById(id)!!.alarms)
        assertEquals(listOf(at("15:15"), at("16:00")), scheduler.scheduledFor(id))
    }

    @Test
    fun `al posponer otro aviso se descarta el pospuesto anterior aunque sea posterior`() = runBlocking {
        // Avisos a las 15 y a las 16. El de las 15 se fue posponiendo hasta las 16:05.
        val id = saveWithAlarms(Alarm(at("16:00")), Alarm(at("16:05"), isSnooze = true))
        // Suena el de las 16:00 y se pospone: el de las 16:15 pasa a "llevar la posta".
        val snooze = SnoozeReminderUseCase(repository, scheduler, now = { at("16:00") })

        snooze(id, firedAt = at("16:00"), minutes = 15)

        assertEquals(listOf(Alarm(at("16:15"), isSnooze = true)), repository.getById(id)!!.alarms)
        assertEquals(listOf(at("16:15")), scheduler.scheduledFor(id))
    }

    @Test
    fun `reprogramar en reinicio agenda solo los avisos futuros de recordatorios activos`() = runBlocking {
        val now = at("12:00")
        val idActive = repository.save(
            Reminder(title = "Futuro", alarms = listOf(Alarm(at("10:00")), Alarm(at("15:00"))), createdAt = createdAt),
        )
        val idDone = repository.save(
            Reminder(title = "Hecho", alarms = listOf(Alarm(at("15:00"))), isDone = true, createdAt = createdAt),
        )

        RescheduleRemindersUseCase(repository, scheduler, now = { now })()

        assertEquals(listOf(at("15:00")), scheduler.scheduledFor(idActive))
        assertEquals(emptyList<Instant>(), scheduler.scheduledFor(idDone))
    }
}

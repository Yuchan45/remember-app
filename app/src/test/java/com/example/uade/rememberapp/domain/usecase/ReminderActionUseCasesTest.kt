package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.Trigger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReminderActionUseCasesTest {

    private val now = Instant.parse("2026-10-03T12:00:00Z")
    private val triggerAt = Instant.parse("2026-10-03T15:00:00Z")

    @Test
    fun `marcar como hecho actualiza el recordatorio y cancela la alarma`() = runBlocking {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderScheduler()
        val markDone = MarkReminderDoneUseCase(repository, scheduler)

        val id = repository.save(
            Reminder(
                title = "Estudiar Kotlin",
                trigger = Trigger.AtTime(triggerAt),
                createdAt = now,
            ),
        )
        scheduler.schedule(id, triggerAt)

        markDone(id)

        val updated = repository.getById(id)
        assertNotNull(updated)
        assertTrue(updated!!.isDone)
        assertNull(scheduler.scheduled[id])
    }

    @Test
    fun `posponer reprograma la hora y la alarma`() = runBlocking {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderScheduler()
        val snooze = SnoozeReminderUseCase(repository, scheduler)

        val id = repository.save(
            Reminder(
                title = "Reunión",
                trigger = Trigger.AtTime(triggerAt),
                createdAt = now,
            ),
        )

        snooze(id, minutes = 15)

        val updated = repository.getById(id)
        assertNotNull(updated)
        val trigger = updated!!.trigger as Trigger.AtTime
        // Debe haberse programado la nueva alarma
        assertEquals(trigger.at, scheduler.scheduled[id])
    }

    @Test
    fun `reprogramar en reinicio agenda solo los recordatorios activos y futuros`() = runBlocking {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderScheduler()
        val reschedule = RescheduleRemindersUseCase(repository, scheduler)

        val future = Instant.now().plus(2, ChronoUnit.HOURS)
        val past = Instant.now().minus(2, ChronoUnit.HOURS)

        val idFuture = repository.save(
            Reminder(title = "Futuro", trigger = Trigger.AtTime(future), createdAt = now),
        )
        val idPast = repository.save(
            Reminder(title = "Pasado", trigger = Trigger.AtTime(past), createdAt = now),
        )
        val idDone = repository.save(
            Reminder(title = "Hecho", trigger = Trigger.AtTime(future), isDone = true, createdAt = now),
        )

        reschedule()

        assertEquals(future, scheduler.scheduled[idFuture])
        assertNull(scheduler.scheduled[idPast])
        assertNull(scheduler.scheduled[idDone])
    }
}

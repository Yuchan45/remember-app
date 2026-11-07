package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderBucket
import com.example.uade.rememberapp.domain.model.ReminderStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TrashReminderUseCaseTest {

    private val createdAt = Instant.parse("2026-09-30T12:00:00Z")
    private val now = Instant.parse("2026-10-01T09:00:00Z")
    private val repository = FakeReminderRepository()
    private val scheduler = FakeReminderScheduler()
    private val save = SaveReminderUseCase(repository, scheduler)
    private val trash = TrashReminderUseCase(repository, save, now = { now })

    @Test
    fun `lo pasa a la papelera sin borrarlo y deja de estar entre los activos`() = runBlocking {
        val id = repository.save(Reminder(title = "Llamar al plomero", createdAt = createdAt))
        val observe = ObserveRemindersUseCase(repository)

        trash(id)

        assertEquals(ReminderStatus.Deleted, repository.getById(id)?.status)
        assertEquals(now, repository.getById(id)?.statusChangedAt)
        assertTrue(observe().first().isEmpty())
        assertEquals(listOf(id), observe(ReminderBucket.Deleted).first().map { it.id })
    }

    @Test
    fun `cancela sus alarmas`() = runBlocking {
        val at = Instant.now().plusSeconds(3600)
        val id = save(Reminder(title = "Turno", alarms = listOf(Alarm(at)), createdAt = createdAt))
        assertEquals(listOf(at), scheduler.scheduledFor(id))

        trash(id)

        assertTrue(scheduler.scheduledFor(id).isEmpty())
    }

    @Test
    fun `con un id que no existe no hace nada`() = runBlocking {
        trash(42)

        assertTrue(repository.observeAll().first().isEmpty())
    }
}

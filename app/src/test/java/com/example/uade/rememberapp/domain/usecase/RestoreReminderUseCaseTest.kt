package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderBucket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class RestoreReminderUseCaseTest {

    private val repository = FakeReminderRepository()
    private val scheduler = FakeReminderScheduler()
    private val save = SaveReminderUseCase(repository, scheduler)
    private val restore = RestoreReminderUseCase(repository, save)
    private val observe = ObserveRemindersUseCase(repository)

    @Test
    fun `deshacer la papelera lo devuelve a los activos`() = runBlocking {
        val id = repository.save(Reminder(title = "Llamar al plomero", createdAt = Instant.EPOCH))
        TrashReminderUseCase(repository, save)(id)

        restore(id)

        assertEquals(listOf(id), observe().first().map { it.id })
        assertNull(repository.getById(id)?.statusChangedAt)
    }

    @Test
    fun `los hechos no aparecen en activos sino en hechos, y restaurar los devuelve`() = runBlocking {
        val id = repository.save(Reminder(title = "Completado", isDone = true, createdAt = Instant.EPOCH))

        assertEquals(emptyList<Long>(), observe(ReminderBucket.Active).first().map { it.id })
        assertEquals(listOf(id), observe(ReminderBucket.Done).first().map { it.id })
        assertEquals(emptyList<Long>(), observe(ReminderBucket.Archived).first().map { it.id })

        restore(id)
        assertEquals(listOf(id), observe(ReminderBucket.Active).first().map { it.id })
    }

    @Test
    fun `restaurar un archivado vuelve a programar sus alarmas`() = runBlocking {
        val at = Instant.now().plusSeconds(3600)
        val id = save(Reminder(title = "Turno", alarms = listOf(Alarm(at)), createdAt = Instant.EPOCH))
        ArchiveReminderUseCase(repository, save)(id)
        assertEquals(emptyList<Instant>(), scheduler.scheduledFor(id))

        restore(id)

        assertEquals(listOf(at), scheduler.scheduledFor(id))
    }
}

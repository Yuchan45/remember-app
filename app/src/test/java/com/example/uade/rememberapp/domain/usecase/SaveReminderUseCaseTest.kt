package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SaveReminderUseCaseTest {

    private val createdAt = Instant.parse("2026-09-30T12:00:00Z")
    private val at18 = Instant.parse("2026-09-30T18:00:00Z")
    private val at20 = Instant.parse("2026-09-30T20:00:00Z")

    private val repository = FakeReminderRepository()
    private val scheduler = FakeReminderScheduler()
    private val save = SaveReminderUseCase(repository, scheduler)

    @Test
    fun `guardar uno nuevo le asigna id y lo hace visible en observar`() = runBlocking {
        val observe = ObserveRemindersUseCase(repository)

        val id = save(Reminder(title = "Llamar al plomero", createdAt = createdAt))

        assertEquals(1L, id)
        assertEquals(listOf("Llamar al plomero"), observe().first().map { it.title })
    }

    @Test
    fun `guardar un recordatorio con varios avisos programa una alarma por cada uno`() = runBlocking {
        val id = save(
            Reminder(title = "Dentista", alarms = listOf(Alarm(at18), Alarm(at20)), createdAt = createdAt),
        )

        assertEquals(listOf(at18, at20), scheduler.scheduledFor(id))
    }

    @Test
    fun `al editar cancela los avisos que se quitaron`() = runBlocking {
        val id = save(
            Reminder(title = "Dentista", alarms = listOf(Alarm(at18), Alarm(at20)), createdAt = createdAt),
        )

        save(repository.getById(id)!!.copy(alarms = listOf(Alarm(at20))))

        assertEquals(listOf(at20), scheduler.scheduledFor(id))
    }

    @Test
    fun `guardar un recordatorio completado no programa alarmas y cancela las que tenía`() = runBlocking {
        val id = save(Reminder(title = "Comprar pan", alarms = listOf(Alarm(at18)), createdAt = createdAt))

        save(repository.getById(id)!!.copy(isDone = true))

        assertEquals(emptyList<Instant>(), scheduler.scheduledFor(id))
    }

    @Test
    fun `observar filtra por estado`() = runBlocking {
        save(Reminder(title = "Activa", createdAt = createdAt))
        save(Reminder(title = "Archivada", status = ReminderStatus.Archived, createdAt = createdAt))

        val observe = ObserveRemindersUseCase(repository)

        assertEquals(listOf("Activa"), observe().first().map { it.title })
        assertEquals(listOf("Archivada"), observe(ReminderStatus.Archived).first().map { it.title })
    }
}

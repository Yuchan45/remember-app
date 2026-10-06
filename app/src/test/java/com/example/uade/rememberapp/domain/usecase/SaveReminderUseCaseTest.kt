package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SaveReminderUseCaseTest {

    private val createdAt = Instant.parse("2026-09-30T12:00:00Z")

    @Test
    fun `guardar uno nuevo le asigna id y lo hace visible en observar`() = runBlocking {
        val repository = FakeReminderRepository()
        val save = SaveReminderUseCase(repository)
        val observe = ObserveRemindersUseCase(repository)

        val id = save(Reminder(title = "Llamar al plomero", createdAt = createdAt))

        assertEquals(1L, id)
        assertEquals(listOf("Llamar al plomero"), observe().first().map { it.title })
    }

    @Test
    fun `observar filtra por estado`() = runBlocking {
        val repository = FakeReminderRepository()
        val save = SaveReminderUseCase(repository)
        save(Reminder(title = "Activa", createdAt = createdAt))
        save(Reminder(title = "Archivada", status = ReminderStatus.Archived, createdAt = createdAt))

        val observe = ObserveRemindersUseCase(repository)

        assertEquals(listOf("Activa"), observe().first().map { it.title })
        assertEquals(listOf("Archivada"), observe(ReminderStatus.Archived).first().map { it.title })
    }
}

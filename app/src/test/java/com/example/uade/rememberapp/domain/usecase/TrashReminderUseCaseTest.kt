package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TrashReminderUseCaseTest {

    private val createdAt = Instant.parse("2026-09-30T12:00:00Z")

    @Test
    fun `lo pasa a la papelera sin borrarlo y deja de estar entre los activos`() = runBlocking {
        val repository = FakeReminderRepository()
        val id = repository.save(Reminder(title = "Llamar al plomero", createdAt = createdAt))
        val observe = ObserveRemindersUseCase(repository)

        TrashReminderUseCase(repository)(id)

        assertEquals(ReminderStatus.Deleted, repository.getById(id)?.status)
        assertTrue(observe().first().isEmpty())
        assertEquals(listOf(id), observe(ReminderStatus.Deleted).first().map { it.id })
    }

    @Test
    fun `con un id que no existe no hace nada`() = runBlocking {
        val repository = FakeReminderRepository()

        TrashReminderUseCase(repository)(42)

        assertTrue(repository.observeAll().first().isEmpty())
    }
}

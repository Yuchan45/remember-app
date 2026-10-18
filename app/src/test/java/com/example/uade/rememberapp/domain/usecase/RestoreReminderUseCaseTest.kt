package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RestoreReminderUseCaseTest {

    @Test
    fun `deshacer la papelera lo devuelve a los activos`() = runBlocking {
        val repository = FakeReminderRepository()
        val id = repository.save(Reminder(title = "Llamar al plomero", createdAt = Instant.EPOCH))
        TrashReminderUseCase(repository)(id)

        RestoreReminderUseCase(repository)(id)

        assertEquals(listOf(id), ObserveRemindersUseCase(repository)().first().map { it.id })
    }

    @Test
    fun `los recordatorios hechos no aparecen en activos pero si en archivados`() = runBlocking {
        val repository = FakeReminderRepository()
        val id = repository.save(Reminder(title = "Completado", isDone = true, createdAt = Instant.EPOCH))

        assertEquals(emptyList<Long>(), ObserveRemindersUseCase(repository)(com.example.uade.rememberapp.domain.model.ReminderStatus.Active).first().map { it.id })
        assertEquals(listOf(id), ObserveRemindersUseCase(repository)(com.example.uade.rememberapp.domain.model.ReminderStatus.Archived).first().map { it.id })

        RestoreReminderUseCase(repository)(id)
        assertEquals(listOf(id), ObserveRemindersUseCase(repository)(com.example.uade.rememberapp.domain.model.ReminderStatus.Active).first().map { it.id })
    }
}

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
}

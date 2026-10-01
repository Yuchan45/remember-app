package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

/** Repositorio en memoria para tests: guarda en un mapa y asigna ids incrementales. */
private class FakeReminderRepository : ReminderRepository {
    private val reminders = MutableStateFlow<Map<Long, Reminder>>(emptyMap())
    private var nextId = 1L

    override fun observeAll(): Flow<List<Reminder>> = reminders.map { it.values.toList() }

    override fun observePendingForPlace(placeId: Long): Flow<List<Reminder>> =
        throw UnsupportedOperationException()

    override suspend fun getById(id: Long): Reminder? = reminders.value[id]

    override suspend fun save(reminder: Reminder): Long {
        val id = if (reminder.id != 0L) reminder.id else nextId++
        reminders.update { it + (id to reminder.copy(id = id)) }
        return id
    }

    override suspend fun delete(id: Long) {
        reminders.update { it - id }
    }
}

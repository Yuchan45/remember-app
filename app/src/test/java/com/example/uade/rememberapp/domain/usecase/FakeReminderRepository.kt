package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Repositorio en memoria para tests: guarda en un mapa y asigna ids incrementales. */
internal class FakeReminderRepository : ReminderRepository {
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

package com.example.uade.rememberapp.data.repository

import com.example.uade.rememberapp.data.local.dao.ReminderDao
import com.example.uade.rememberapp.data.local.mapper.toDomain
import com.example.uade.rememberapp.data.local.mapper.toEntity
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [ReminderRepository] guardado con Room. Solo traduce entre el dominio y las filas (con el
 * mapper); las reglas de negocio (ej. programar la alarma al guardar) van en los casos de uso.
 */
class ReminderRepositoryImpl(
    private val dao: ReminderDao,
) : ReminderRepository {

    override fun observeAll(): Flow<List<Reminder>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observePendingForPlace(placeId: Long): Flow<List<Reminder>> =
        dao.observePendingForPlace(placeId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: Long): Reminder? = dao.getById(id)?.toDomain()

    override suspend fun save(reminder: Reminder): Long {
        val newId = dao.upsert(reminder.toEntity())
        // @Upsert devuelve -1 cuando actualiza una fila existente: el id es el que ya tenía.
        return if (reminder.id != 0L) reminder.id else newId
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

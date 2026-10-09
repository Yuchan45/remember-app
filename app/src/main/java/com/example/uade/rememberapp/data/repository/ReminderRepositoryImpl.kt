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
 *
 * Las etiquetas y los avisos de cada recordatorio se leen y se guardan junto con él: el dominio
 * ve un `Reminder` con sus listas y no sabe que en la base son varias tablas más.
 */
class ReminderRepositoryImpl(
    private val dao: ReminderDao,
) : ReminderRepository {

    override fun observeAll(): Flow<List<Reminder>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observePendingForPlace(placeId: Long): Flow<List<Reminder>> =
        dao.observePendingForPlace(placeId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: Long): Reminder? = dao.getById(id)?.toDomain()

    override suspend fun save(reminder: Reminder): Long =
        dao.saveWithDetails(
            reminder = reminder.toEntity(),
            tagIds = reminder.tags.map { it.id },
            // Sin repetidos: dos avisos a la misma hora son uno solo.
            alarmsAt = reminder.alarms.map { it.at.toEpochMilli() }.distinct(),
            places = reminder.places.map { it.toEntity() },
        )

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

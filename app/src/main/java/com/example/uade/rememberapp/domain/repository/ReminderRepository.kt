package com.example.uade.rememberapp.domain.repository

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderBucket
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeAll(): Flow<List<Reminder>>

    /**
     * Los de una sección (Home o una pestaña del Historial), en vivo. El filtro lo hace la base:
     * así no se cargan todos los recordatorios con sus fotos y avisos para tirar la mayoría.
     * Los más recientes primero (por fecha de cambio de estado, o de creación si no tiene).
     */
    fun observeByBucket(bucket: ReminderBucket): Flow<List<Reminder>>

    /** Lo mismo que [observeByBucket], una sola vez (para borrar los vencidos). */
    suspend fun getByBucket(bucket: ReminderBucket): List<Reminder>

    fun observePendingForPlace(placeId: Long): Flow<List<Reminder>>
    suspend fun getById(id: Long): Reminder?
    suspend fun save(reminder: Reminder): Long
    suspend fun delete(id: Long)
}

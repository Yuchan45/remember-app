package com.example.uade.rememberapp.domain.repository

import com.example.uade.rememberapp.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeAll(): Flow<List<Reminder>>
    fun observePendingForPlace(placeId: Long): Flow<List<Reminder>>
    suspend fun getById(id: Long): Reminder?
    suspend fun save(reminder: Reminder): Long
    suspend fun delete(id: Long)
}

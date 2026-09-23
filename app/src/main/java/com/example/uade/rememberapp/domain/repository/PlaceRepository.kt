package com.example.uade.rememberapp.domain.repository

import com.example.uade.rememberapp.domain.model.Place
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    fun observeAll(): Flow<List<Place>>
    suspend fun getById(id: Long): Place?
    suspend fun save(place: Place): Long
    suspend fun delete(id: Long)
}

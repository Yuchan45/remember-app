package com.example.uade.rememberapp.data.repository

import com.example.uade.rememberapp.data.local.dao.PlaceDao
import com.example.uade.rememberapp.data.local.mapper.toDomain
import com.example.uade.rememberapp.data.local.mapper.toEntity
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de [PlaceRepository] persistida en Room mediante [PlaceDao].
 */
class PlaceRepositoryImpl(
    private val dao: PlaceDao,
) : PlaceRepository {

    override fun observeAll(): Flow<List<Place>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: Long): Place? =
        dao.getById(id)?.toDomain()

    override suspend fun save(place: Place): Long =
        dao.insert(place.toEntity())

    override suspend fun delete(id: Long) {
        dao.deleteAlertsByPlaceId(id)
        dao.deleteById(id)
    }
}

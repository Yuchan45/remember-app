package com.example.uade.rememberapp.data.repository

import com.example.uade.rememberapp.data.local.dao.PlaceDao
import com.example.uade.rememberapp.data.local.entity.PlaceEntity
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceRepositoryImplTest {

    private class FakePlaceDao : PlaceDao {
        private val places = mutableListOf<PlaceEntity>()
        private val flow = MutableStateFlow<List<PlaceEntity>>(emptyList())
        var deletedAlertsForPlaceId: Long? = null

        private fun emit() {
            flow.value = places.toList()
        }

        override fun observeAll(): Flow<List<PlaceEntity>> = flow

        override suspend fun getById(id: Long): PlaceEntity? =
            places.firstOrNull { it.id == id }

        override suspend fun getAll(): List<PlaceEntity> = places.toList()

        override suspend fun insert(place: PlaceEntity): Long {
            val id = if (place.id == 0L) (places.maxOfOrNull { it.id } ?: 0L) + 1L else place.id
            places.removeAll { it.id == id }
            places.add(place.copy(id = id))
            emit()
            return id
        }

        override suspend fun update(place: PlaceEntity) {
            places.removeAll { it.id == place.id }
            places.add(place)
            emit()
        }

        override suspend fun deleteById(id: Long) {
            places.removeAll { it.id == id }
            emit()
        }

        override suspend fun deleteAlertsByPlaceId(placeId: Long) {
            deletedAlertsForPlaceId = placeId
        }
    }

    @Test
    fun `save guarda lugar y genera id`() = runBlocking {
        val dao = FakePlaceDao()
        val repo = PlaceRepositoryImpl(dao)

        val id = repo.save(Place(name = "Casa", latitude = -34.58, longitude = -58.43, kind = PlaceKind.Home))
        assertEquals(1L, id)

        val retrieved = repo.getById(1L)
        assertEquals("Casa", retrieved?.name)
        assertEquals(PlaceKind.Home, retrieved?.kind)
    }

    @Test
    fun `delete elimina el lugar y limpia sus alertas asociadas`() = runBlocking {
        val dao = FakePlaceDao()
        val repo = PlaceRepositoryImpl(dao)

        val id = repo.save(Place(name = "Trabajo", latitude = -34.60, longitude = -58.38, kind = PlaceKind.Work))
        repo.delete(id)

        assertNull(repo.getById(id))
        assertEquals(id, dao.deletedAlertsForPlaceId)
        assertEquals(emptyList<Place>(), repo.observeAll().first())
    }
}

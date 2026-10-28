package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.AddressSuggestion
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceKind
import com.example.uade.rememberapp.domain.repository.AddressSearchService
import com.example.uade.rememberapp.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaceUseCasesTest {

    private class FakePlaceRepository : PlaceRepository {
        val places = mutableListOf<Place>()
        val flow = MutableStateFlow<List<Place>>(emptyList())
        var deletedId: Long? = null

        private fun emit() {
            flow.value = places.toList()
        }

        override fun observeAll(): Flow<List<Place>> = flow

        override suspend fun getById(id: Long): Place? = places.firstOrNull { it.id == id }

        override suspend fun save(place: Place): Long {
            val id = if (place.id == 0L) (places.maxOfOrNull { it.id } ?: 0L) + 1L else place.id
            val saved = place.copy(id = id)
            places.removeAll { it.id == id }
            places.add(saved)
            emit()
            return id
        }

        override suspend fun delete(id: Long) {
            deletedId = id
            places.removeAll { it.id == id }
            emit()
        }
    }

    private class FakeAddressSearchService : AddressSearchService {
        var searchedQuery: String? = null
        override suspend fun search(query: String): List<AddressSuggestion> {
            searchedQuery = query
            return listOf(
                AddressSuggestion(
                    addressLine = "Lima 757, CABA",
                    latitude = -34.617,
                    longitude = -58.381,
                    featureName = "Lima 757",
                ),
            )
        }

        override suspend fun getCurrentLocation(): AddressSuggestion =
            AddressSuggestion(
                addressLine = "Av. Corrientes 1234, CABA",
                latitude = -34.603,
                longitude = -58.381,
                featureName = "Av. Corrientes",
            )
    }

    private lateinit var placeRepo: FakePlaceRepository
    private lateinit var addressService: FakeAddressSearchService

    @Before
    fun setUp() {
        placeRepo = FakePlaceRepository()
        addressService = FakeAddressSearchService()
    }

    @Test
    fun `guardar lugar genera id y es observable`() = runBlocking {
        val saveUseCase = SavePlaceUseCase(placeRepo)
        val observeUseCase = ObservePlacesUseCase(placeRepo)

        val id = saveUseCase(
            Place(name = "Casa", latitude = -34.588, longitude = -58.430, radiusMeters = 150, kind = PlaceKind.Home),
        )

        assertEquals(1L, id)
        val list = observeUseCase().first()
        assertEquals(1, list.size)
        assertEquals("Casa", list.first().name)
        assertEquals(PlaceKind.Home, list.first().kind)
    }

    @Test
    fun `eliminar lugar lo remueve del repositorio`() = runBlocking {
        val saveUseCase = SavePlaceUseCase(placeRepo)
        val deleteUseCase = DeletePlaceUseCase(placeRepo)
        val observeUseCase = ObservePlacesUseCase(placeRepo)

        val id = saveUseCase(
            Place(name = "Facultad", latitude = -34.617, longitude = -58.381, kind = PlaceKind.Study),
        )
        deleteUseCase(id)

        assertEquals(id, placeRepo.deletedId)
        assertTrue(observeUseCase().first().isEmpty())
    }

    @Test
    fun `buscar direccion invoca el servicio y retorna sugerencias`() = runBlocking {
        val searchUseCase = SearchAddressUseCase(addressService)

        val results = searchUseCase("Lima 757")

        assertEquals("Lima 757", addressService.searchedQuery)
        assertEquals(1, results.size)
        assertEquals("Lima 757, CABA", results.first().addressLine)
        assertEquals(-34.617, results.first().latitude, 0.001)
    }

    @Test
    fun `obtener ubicacion actual retorna las coordenadas esperadas`() = runBlocking {
        val locationUseCase = GetCurrentLocationUseCase(addressService)

        val location = locationUseCase()

        assertEquals(-34.603, location?.latitude ?: 0.0, 0.001)
        assertEquals(-58.381, location?.longitude ?: 0.0, 0.001)
        assertEquals("Av. Corrientes 1234, CABA", location?.addressLine)
    }
}

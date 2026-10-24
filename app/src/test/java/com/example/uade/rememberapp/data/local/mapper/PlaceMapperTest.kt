package com.example.uade.rememberapp.data.local.mapper

import com.example.uade.rememberapp.data.local.entity.PlaceEntity
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceMapperTest {

    private val place = Place(
        id = 3,
        name = "Facultad",
        latitude = -34.617,
        longitude = -58.381,
        radiusMeters = 150,
        kind = PlaceKind.Study,
    )

    @Test
    fun `ida y vuelta de la fila conserva todos los datos del lugar`() {
        assertEquals(place, place.toEntity().toDomain())
    }

    @Test
    fun `tipo desconocido en entity cae en PlaceKind Other por defecto`() {
        val entity = PlaceEntity(
            id = 5,
            name = "Lugar raro",
            latitude = -34.600,
            longitude = -58.380,
            radiusMeters = 200,
            kind = "NON_EXISTING_KIND",
        )
        val domain = entity.toDomain()
        assertEquals(PlaceKind.Other, domain.kind)
    }
}

package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.repository.PlaceRepository

/**
 * Guarda un lugar nuevo o editado y devuelve su ID generado o actualizado.
 */
class SavePlaceUseCase(
    private val places: PlaceRepository,
) {
    suspend operator fun invoke(place: Place): Long = places.save(place)
}

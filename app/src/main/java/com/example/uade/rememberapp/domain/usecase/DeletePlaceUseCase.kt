package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.repository.PlaceRepository

/**
 * Elimina un lugar guardado por su ID.
 */
class DeletePlaceUseCase(
    private val places: PlaceRepository,
) {
    suspend operator fun invoke(id: Long) = places.delete(id)
}

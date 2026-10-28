package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow

/**
 * Emite la lista reactiva de todos los lugares guardados por el usuario.
 */
class ObservePlacesUseCase(
    private val places: PlaceRepository,
) {
    operator fun invoke(): Flow<List<Place>> = places.observeAll()
}

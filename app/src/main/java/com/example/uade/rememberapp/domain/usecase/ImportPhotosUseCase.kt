package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.MAX_PHOTOS
import com.example.uade.rememberapp.domain.repository.PhotoRepository

/**
 * Copia dentro de la app las fotos elegidas en el selector y devuelve sus rutas.
 *
 * Respeta el máximo de [MAX_PHOTOS] por recordatorio: si ya tiene [alreadyCount], solo importa
 * las que entran (las primeras que se eligieron).
 */
class ImportPhotosUseCase(
    private val photos: PhotoRepository,
) {
    suspend operator fun invoke(uris: List<String>, alreadyCount: Int): List<String> =
        uris.take((MAX_PHOTOS - alreadyCount).coerceAtLeast(0)).map { photos.import(it) }
}

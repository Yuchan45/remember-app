package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.CropRect
import com.example.uade.rememberapp.domain.repository.PhotoRepository

/**
 * Recorta la foto elegida de portada y devuelve la ruta del recorte (lo que se guarda en
 * Reminder.photoPath y se muestra en la card y en el detalle).
 *
 * TODO: borrar la portada anterior cuando se guarde el recordatorio (hoy queda el archivo).
 */
class SaveCoverUseCase(
    private val photos: PhotoRepository,
) {
    suspend operator fun invoke(sourcePath: String, crop: CropRect): String =
        photos.saveCover(sourcePath, crop)
}

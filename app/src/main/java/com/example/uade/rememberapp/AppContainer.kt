package com.example.uade.rememberapp

import com.example.uade.rememberapp.data.repository.InMemoryTagRepository
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.repository.TagRepository

/**
 * Inyección de dependencias manual: crea una sola vez los repositorios (y más adelante la base
 * de datos, schedulers y casos de uso) y los expone con el tipo de la interfaz del dominio.
 *
 * Vive en [RememberApp]; los ViewModels lo toman desde su Factory.
 */
class AppContainer {

    // TODO: pasar a Room. Mientras tanto arranca con etiquetas de ejemplo.
    val tagRepository: TagRepository by lazy {
        InMemoryTagRepository(initial = SampleTags)
    }
}

private val SampleTags = listOf(
    Tag(id = 1, name = "Salud", colorArgb = 0xFF6FCF97),
    Tag(id = 5, name = "Ideas", colorArgb = 0xFFF2C94C),
)

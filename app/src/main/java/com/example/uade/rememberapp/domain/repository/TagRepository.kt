package com.example.uade.rememberapp.domain.repository

import com.example.uade.rememberapp.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Etiquetas del usuario (nombre + color). Las comparten todos los recordatorios.
 *
 * [create] y [update] validan el nombre y lanzan [InvalidTagNameException] si no sirve.
 */
interface TagRepository {
    fun observeAll(): Flow<List<Tag>>

    /** Crea la etiqueta y la devuelve con su id. */
    suspend fun create(name: String, colorArgb: Long): Tag

    suspend fun update(tag: Tag)

    suspend fun delete(id: Long)

    /** Vuelve a agregar una etiqueta recién borrada, con el mismo id (para "Deshacer"). */
    suspend fun restore(tag: Tag)

    companion object {
        const val MaxNameLength = 20
    }
}

/** Por qué no se aceptó un nombre de etiqueta. La UI elige el texto a mostrar según [reason]. */
class InvalidTagNameException(val reason: Reason) : IllegalArgumentException(reason.name) {
    enum class Reason {
        Empty,
        TooLong,
        Duplicate,
    }
}

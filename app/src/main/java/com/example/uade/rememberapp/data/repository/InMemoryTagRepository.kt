package com.example.uade.rememberapp.data.repository

import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.repository.InvalidTagNameException
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.repository.TagRepository.Companion.MaxNameLength
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Etiquetas guardadas en memoria: duran mientras la app está abierta.
 *
 * TODO: reemplazar por una implementación con Room; la interfaz no cambia.
 */
class InMemoryTagRepository(
    initial: List<Tag> = emptyList(),
) : TagRepository {

    private val tags = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeAll(): Flow<List<Tag>> = tags.asStateFlow()

    override suspend fun create(name: String, colorArgb: Long): Tag {
        val cleanName = validName(name, excludingId = null)
        val tag = Tag(id = nextId++, name = cleanName, colorArgb = colorArgb)
        tags.update { it + tag }
        return tag
    }

    override suspend fun update(tag: Tag) {
        val cleanName = validName(tag.name, excludingId = tag.id)
        tags.update { list ->
            list.map { if (it.id == tag.id) tag.copy(name = cleanName) else it }
        }
    }

    override suspend fun delete(id: Long) {
        tags.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun restore(tag: Tag) {
        tags.update { list -> if (list.any { it.id == tag.id }) list else list + tag }
    }

    /** Nombre sin espacios de más; falla si queda vacío, es muy largo o ya existe otro igual. */
    private fun validName(name: String, excludingId: Long?): String {
        val clean = name.trim()
        val reason = when {
            clean.isEmpty() -> InvalidTagNameException.Reason.Empty
            clean.length > MaxNameLength -> InvalidTagNameException.Reason.TooLong
            tags.value.any { it.id != excludingId && it.name.equals(clean, ignoreCase = true) } ->
                InvalidTagNameException.Reason.Duplicate
            else -> return clean
        }
        throw InvalidTagNameException(reason)
    }
}

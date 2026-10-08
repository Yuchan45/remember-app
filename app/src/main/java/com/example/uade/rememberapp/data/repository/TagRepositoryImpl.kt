package com.example.uade.rememberapp.data.repository

import com.example.uade.rememberapp.data.local.dao.TagDao
import com.example.uade.rememberapp.data.local.entity.TagEntity
import com.example.uade.rememberapp.data.local.mapper.toDomain
import com.example.uade.rememberapp.data.local.mapper.toEntity
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.repository.validTagName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [TagRepository] guardado con Room. Valida los nombres con [validTagName] (la misma regla que
 * cualquier implementación) antes de escribir.
 */
class TagRepositoryImpl(
    private val dao: TagDao,
) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun create(name: String, colorArgb: Long): Tag {
        val cleanName = validTagName(name, existing())
        val id = dao.insert(TagEntity(name = cleanName, colorArgb = colorArgb))
        return Tag(id = id, name = cleanName, colorArgb = colorArgb)
    }

    override suspend fun update(tag: Tag) {
        val cleanName = validTagName(tag.name, existing(), excludingId = tag.id)
        dao.update(tag.copy(name = cleanName).toEntity())
    }

    /** También la saca de todos los recordatorios (CASCADE en `reminder_tags`). */
    override suspend fun delete(id: Long) = dao.deleteById(id)

    /**
     * Vuelve a crearla con su mismo id ("Deshacer").
     * TODO: no recupera en qué recordatorios estaba asignada; el borrado ya los sacó (CASCADE).
     */
    override suspend fun restore(tag: Tag) {
        dao.insert(tag.toEntity())
    }

    private suspend fun existing(): List<Tag> = dao.getAll().map { it.toDomain() }
}

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

/**
 * Valida un nombre de etiqueta contra las que ya existen ([existing]) y lo devuelve sin espacios
 * de más. Lanza [InvalidTagNameException] si queda vacío, es muy largo o ya hay otra con el
 * mismo nombre (sin importar mayúsculas). [excludingId] es la etiqueta que se está editando,
 * para que no choque consigo misma.
 *
 * Es una regla de negocio: vive en el dominio y la usan todas las implementaciones de
 * [TagRepository].
 */
fun validTagName(name: String, existing: List<Tag>, excludingId: Long? = null): String {
    val clean = name.trim()
    val reason = when {
        clean.isEmpty() -> InvalidTagNameException.Reason.Empty
        clean.length > TagRepository.MaxNameLength -> InvalidTagNameException.Reason.TooLong
        existing.any { it.id != excludingId && it.name.equals(clean, ignoreCase = true) } ->
            InvalidTagNameException.Reason.Duplicate
        else -> return clean
    }
    throw InvalidTagNameException(reason)
}

/** Por qué no se aceptó un nombre de etiqueta. La UI elige el texto a mostrar según [reason]. */
class InvalidTagNameException(val reason: Reason) : IllegalArgumentException(reason.name) {
    enum class Reason {
        Empty,
        TooLong,
        Duplicate,
    }
}

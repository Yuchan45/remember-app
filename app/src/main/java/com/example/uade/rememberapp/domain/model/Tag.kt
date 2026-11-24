package com.example.uade.rememberapp.domain.model

/**
 * Etiqueta creada por el usuario, con nombre y color.
 *
 * El color se guarda como ARGB en un Long (ej. 0xFF6FCF97) y no como Color de Compose,
 * para que el dominio no dependa de la UI. La UI lo convierte con `Color(colorArgb)`.
 */
data class Tag(
    val id: Long = 0,
    val name: String,
    val colorArgb: Long,
) {
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
 * [com.example.uade.rememberapp.domain.repository.TagRepository].
 */
fun validTagName(name: String, existing: List<Tag>, excludingId: Long? = null): String {
    val clean = name.trim()
    val reason = when {
        clean.isEmpty() -> InvalidTagNameException.Reason.Empty
        clean.length > Tag.MaxNameLength -> InvalidTagNameException.Reason.TooLong
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

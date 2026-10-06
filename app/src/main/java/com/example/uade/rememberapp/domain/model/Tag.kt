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
)

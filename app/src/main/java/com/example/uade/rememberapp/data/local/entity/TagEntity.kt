package com.example.uade.rememberapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Fila de la tabla `tags`: una etiqueta del usuario (nombre y color). */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Color en ARGB, como [com.example.uade.rememberapp.domain.model.Tag.colorArgb]. */
    val colorArgb: Long,
)

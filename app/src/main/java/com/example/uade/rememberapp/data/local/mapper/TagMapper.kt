package com.example.uade.rememberapp.data.local.mapper

import com.example.uade.rememberapp.data.local.entity.TagEntity
import com.example.uade.rememberapp.domain.model.Tag

// Traducción entre la etiqueta del dominio y su fila de Room.

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, colorArgb = colorArgb)

fun Tag.toEntity(): TagEntity = TagEntity(id = id, name = name, colorArgb = colorArgb)

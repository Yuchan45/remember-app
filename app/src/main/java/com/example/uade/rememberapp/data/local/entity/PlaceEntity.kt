package com.example.uade.rememberapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `places`: un lugar guardado por el usuario (nombre, coordenadas, radio y tipo).
 */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 150,
    /** Nombre del enum [com.example.uade.rememberapp.domain.model.PlaceKind], ej. "Home", "Work". */
    val kind: String = "Other",
)

package com.example.uade.rememberapp.domain.model

/** Lugar guardado por el usuario. El id es también el id de su geofence. */
data class Place(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 150,
    val kind: PlaceKind = PlaceKind.Other,
)

/** Tipo de lugar que elige el usuario. Define el ícono con que se muestra. */
enum class PlaceKind {
    Home,
    Work,
    Study,
    Parking,
    Other,
}

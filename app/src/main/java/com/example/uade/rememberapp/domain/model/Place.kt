package com.example.uade.rememberapp.domain.model

/** Lugar guardado por el usuario. El id es también el id de su geofence. */
data class Place(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 150,
)

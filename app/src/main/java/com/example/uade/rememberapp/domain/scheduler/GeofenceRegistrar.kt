package com.example.uade.rememberapp.domain.scheduler

/** Registra una geofence por lugar. Lo implementa la capa de datos con Play Services. */
interface GeofenceRegistrar {
    suspend fun ensureRegistered(placeId: Long)
    suspend fun remove(placeId: Long)
}

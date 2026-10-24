package com.example.uade.rememberapp.data.scheduler

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.uade.rememberapp.domain.repository.PlaceRepository
import com.example.uade.rememberapp.domain.scheduler.GeofenceRegistrar
import com.example.uade.rememberapp.platform.receiver.GeofenceReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Implementación de [GeofenceRegistrar] usando la API de Geofencing de Google Play Services.
 */
class GeofenceRegistrarImpl(
    private val context: Context,
    private val placeRepository: PlaceRepository,
) : GeofenceRegistrar {

    private val geofencingClient by lazy { LocationServices.getGeofencingClient(context) }

    override suspend fun ensureRegistered(placeId: Long) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) return

        val place = placeRepository.getById(placeId) ?: return

        val geofence = Geofence.Builder()
            .setRequestId(place.id.toString())
            .setCircularRegion(place.latitude, place.longitude, place.radiusMeters.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(10_000)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(context, GeofenceReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            place.id.toInt(),
            intent,
            flags,
        )

        try {
            geofencingClient.addGeofences(request, pendingIntent)
        } catch (_: SecurityException) {
            // Sin permiso en tiempo de ejecución
        }
    }

    override suspend fun remove(placeId: Long) {
        try {
            geofencingClient.removeGeofences(listOf(placeId.toString()))
        } catch (_: Exception) {
            // Ignorar fallas al desregistrar
        }
    }
}

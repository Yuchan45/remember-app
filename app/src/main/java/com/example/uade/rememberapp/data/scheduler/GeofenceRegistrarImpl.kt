package com.example.uade.rememberapp.data.scheduler

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.uade.rememberapp.domain.repository.PlaceRepository
import com.example.uade.rememberapp.domain.scheduler.GeofenceRegistrar
import com.example.uade.rememberapp.platform.location.LocationPermissionHelper
import com.example.uade.rememberapp.platform.receiver.GeofenceReceiver
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
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

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java).apply {
            action = GeofenceReceiver.ACTION_GEOFENCE_EVENT
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        PendingIntent.getBroadcast(
            context,
            GEOFENCE_PENDING_INTENT_REQUEST_CODE,
            intent,
            flags,
        )
    }

    override suspend fun ensureRegistered(placeId: Long) {
        val hasFine = LocationPermissionHelper.hasFineLocation(context)
        val hasBackground = LocationPermissionHelper.hasBackgroundLocation(context)

        if (!hasFine || !hasBackground) {
            Log.w(
                TAG,
                "No se puede registrar geocerca para lugar $placeId: faltan permisos (fine=$hasFine, background=$hasBackground).",
            )
            return
        }

        if (!LocationPermissionHelper.isLocationServiceEnabled(context)) {
            Log.w(
                TAG,
                "El servicio de ubicación (GPS) está apagado en el teléfono. Las geocercas no se dispararán hasta que se active.",
            )
        }

        val place = placeRepository.getById(placeId)
        if (place == null) {
            Log.w(TAG, "Lugar con ID $placeId no encontrado en la base de datos.")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(place.id.toString())
            .setCircularRegion(place.latitude, place.longitude, place.radiusMeters.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(5_000)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "Geocerca registrada con éxito para '${place.name}' (id=$placeId) en [${place.latitude}, ${place.longitude}], radio=${place.radiusMeters}m.",
                    )
                }
                .addOnFailureListener { exception ->
                    val errorDetail = if (exception is ApiException) {
                        "ApiException (${exception.statusCode}): ${GeofenceStatusCodes.getStatusCodeString(exception.statusCode)}"
                    } else {
                        exception.message ?: "Error desconocido"
                    }
                    Log.e(
                        TAG,
                        "Fallo al registrar geocerca para '${place.name}' (id=$placeId): $errorDetail",
                        exception,
                    )
                }
        } catch (e: SecurityException) {
            Log.e(
                TAG,
                "SecurityException al registrar geocerca para lugar $placeId. Se requiere ACCESS_BACKGROUND_LOCATION en Android 10+.",
                e,
            )
        }
    }

    override suspend fun remove(placeId: Long) {
        try {
            geofencingClient.removeGeofences(listOf(placeId.toString()))
                .addOnSuccessListener {
                    Log.d(TAG, "Geocerca eliminada con éxito para lugar id=$placeId.")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Fallo al desregistrar geocerca para lugar id=$placeId: ${exception.message}", exception)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al desregistrar geocerca para lugar id=$placeId", e)
        }
    }

    companion object {
        private const val TAG = "GeofenceRegistrar"
        private const val GEOFENCE_PENDING_INTENT_REQUEST_CODE = 1001
    }
}

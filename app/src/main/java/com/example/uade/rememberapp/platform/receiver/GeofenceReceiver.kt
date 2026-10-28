package com.example.uade.rememberapp.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.platform.notification.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * [BroadcastReceiver] invocado por Google Play Services cuando el dispositivo entra o sale
 * del área geográfica de una geocerca configurada.
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive invocado con acción: ${intent.action}")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent.fromIntent(intent) es null.")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMsg = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(
                TAG,
                "Error en GeofencingEvent: código=${geofencingEvent.errorCode}, mensaje=$errorMsg",
            )
            return
        }

        val transition = geofencingEvent.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            Log.w(TAG, "Transición no manejada o desconocida: $transition")
            return
        }

        val eventType = if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            PlaceAlertEvent.Arrive
        } else {
            PlaceAlertEvent.Leave
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences.orEmpty()
        Log.d(
            TAG,
            "Transición detectada: $eventType ($transition). Cantidad de geocercas disparadas: ${triggeringGeofences.size}",
        )
        if (triggeringGeofences.isEmpty()) return

        val app = context.applicationContext as RememberApp
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allReminders = app.container.reminderRepository.observeAll().first()

                for (geofence in triggeringGeofences) {
                    val placeId = geofence.requestId.toLongOrNull()
                    if (placeId == null) {
                        Log.w(TAG, "requestId de geocerca inválido: '${geofence.requestId}'")
                        continue
                    }

                    val place = app.container.placeRepository.getById(placeId)
                    if (place == null) {
                        Log.w(TAG, "Lugar id=$placeId no existe en la base de datos.")
                        continue
                    }

                    val matchingReminders = allReminders.filter { reminder ->
                        reminder.status == ReminderStatus.Active &&
                            !reminder.isDone &&
                            reminder.places.any { it.placeId == placeId && it.event == eventType }
                    }

                    Log.d(
                        TAG,
                        "Lugar '${place.name}' (id=$placeId): encontrados ${matchingReminders.size} recordatorios activos para el evento $eventType.",
                    )

                    for (reminder in matchingReminders) {
                        Log.d(
                            TAG,
                            "Mostrando notificación de lugar para recordatorio id=${reminder.id} ('${reminder.title}') en '${place.name}'.",
                        )
                        NotificationHelper.showPlaceReminderNotification(
                            context = context,
                            reminder = reminder,
                            place = place,
                            event = eventType,
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al procesar evento de geocercas", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        const val ACTION_GEOFENCE_EVENT = "com.example.uade.rememberapp.ACTION_GEOFENCE_EVENT"
    }
}

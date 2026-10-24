package com.example.uade.rememberapp.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.platform.notification.NotificationHelper
import com.google.android.gms.location.Geofence
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
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val transition = geofencingEvent.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            return
        }

        val eventType = if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            PlaceAlertEvent.Arrive
        } else {
            PlaceAlertEvent.Leave
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences.orEmpty()
        if (triggeringGeofences.isEmpty()) return

        val app = context.applicationContext as RememberApp
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allReminders = app.container.reminderRepository.observeAll().first()

                for (geofence in triggeringGeofences) {
                    val placeId = geofence.requestId.toLongOrNull() ?: continue
                    val place = app.container.placeRepository.getById(placeId) ?: continue

                    val matchingReminders = allReminders.filter { reminder ->
                        reminder.status == ReminderStatus.Active &&
                            !reminder.isDone &&
                            reminder.places.any { it.placeId == placeId && it.event == eventType }
                    }

                    for (reminder in matchingReminders) {
                        NotificationHelper.showPlaceReminderNotification(
                            context = context,
                            reminder = reminder,
                            place = place,
                            event = eventType,
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

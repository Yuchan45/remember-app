package com.example.uade.rememberapp.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.data.scheduler.AlarmReminderScheduler
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.platform.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * [BroadcastReceiver] que el sistema operativo despierta cuando vence una alarma de [AlarmManager].
 *
 * Usa [goAsync] para realizar la consulta asíncrona a Room sin bloquear el hilo principal.
 * Si el recordatorio sigue activo y no se completó, muestra la notificación.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AlarmReminderScheduler.EXTRA_REMINDER_ID, -1L)
        val alarmAtMillis = intent.getLongExtra(AlarmReminderScheduler.EXTRA_ALARM_AT, -1L)
        Log.d(TAG, "onReceive recibido para reminderId=$reminderId, alarmAtMillis=$alarmAtMillis")

        if (reminderId == -1L) {
            Log.w(TAG, "reminderId inválido (-1), ignorando broadcast")
            return
        }
        val alarmAt = if (alarmAtMillis != -1L) Instant.ofEpochMilli(alarmAtMillis) else Instant.now()

        val pendingResult = goAsync()
        val app = context.applicationContext as RememberApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = app.container.reminderRepository.getById(reminderId)
                Log.d(TAG, "Recordatorio obtenido de Room: id=${reminder?.id}, titulo='${reminder?.title}', isDone=${reminder?.isDone}, status=${reminder?.status}, tipo=${reminder?.type}")

                if (reminder != null && !reminder.isDone && reminder.status == ReminderStatus.Active) {
                    // Si se repite, primero se programa la próxima vez
                    app.container.advanceRepeatingAlarmUseCase(reminderId, alarmAt)
                    NotificationHelper.showReminderNotification(context, reminder, alarmAt)
                    Log.d(TAG, "showReminderNotification llamado con éxito para id=$reminderId")
                } else {
                    Log.w(TAG, "No se muestra notificación para id=$reminderId (reminder=$reminder, isDone=${reminder?.isDone}, status=${reminder?.status})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando alarma en AlarmReceiver para id=$reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}

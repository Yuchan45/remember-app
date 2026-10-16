package com.example.uade.rememberapp.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        if (reminderId == -1L || alarmAtMillis == -1L) return
        val alarmAt = Instant.ofEpochMilli(alarmAtMillis)

        val pendingResult = goAsync()
        val app = context.applicationContext as RememberApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = app.container.reminderRepository.getById(reminderId)
                // Además, el aviso tiene que seguir existiendo: si se borró o se reemplazó al
                // posponer y la alarma igual sonó, no se muestra.
                val stillExists = reminder?.alarms?.any { it.at == alarmAt } == true
                if (reminder != null && stillExists && !reminder.isDone && reminder.status == ReminderStatus.Active) {
                    NotificationHelper.showReminderNotification(context, reminder, alarmAt)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

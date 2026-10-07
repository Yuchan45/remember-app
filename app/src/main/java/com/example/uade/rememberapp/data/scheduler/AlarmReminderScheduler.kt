package com.example.uade.rememberapp.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import com.example.uade.rememberapp.platform.receiver.AlarmReceiver
import java.time.Instant

/**
 * Implementación de [ReminderScheduler] con [AlarmManager].
 *
 * Programa y cancela alarmas exactas que despiertan al dispositivo incluso si la app
 * está cerrada o en modo reposo (Doze mode).
 */
class AlarmReminderScheduler(
    private val context: Context,
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun schedule(reminderId: Long, at: Instant) {
        val triggerMillis = at.toEpochMilli()
        // No programamos avisos para el pasado
        if (triggerMillis <= System.currentTimeMillis()) return

        val pendingIntent = createPendingIntent(reminderId)

        // En Android 12+ (API 31+), verificamos si podemos programar alarmas exactas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent,
                )
            } else {
                // Fallback aproximado si el usuario revocó el permiso de alarmas exactas
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent,
                )
            }
        } else {
            // Android 8.0 a 11 (API 26 a 30): siempre tiene permiso para alarmas exactas
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent,
            )
        }
    }

    override suspend fun cancel(reminderId: Long) {
        val pendingIntent = createPendingIntent(reminderId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun createPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_REMINDER_ALARM = "com.example.uade.rememberapp.ACTION_REMINDER_ALARM"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}

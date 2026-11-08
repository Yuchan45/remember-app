package com.example.uade.rememberapp.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.example.uade.rememberapp.MainActivity
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
        val nowMillis = System.currentTimeMillis()
        val originalMillis = at.toEpochMilli()

        // Si ya pasó hace más de 2 minutos, no lo programamos (es un aviso vencido del pasado).
        // Si venció hace menos de 2 minutos (por ejemplo, el usuario eligió el minuto actual
        // o tardó unos segundos en guardar), lo ejecutamos inmediatamente (en 1 segundo).
        val triggerMillis = if (originalMillis <= nowMillis) {
            if (nowMillis - originalMillis <= 120_000L) {
                nowMillis + 1000L
            } else {
                Log.w(TAG, "Aviso ignorado para id=$reminderId: $originalMillis ya pasó hace más de 2 min respecto a now=$nowMillis")
                return
            }
        } else {
            originalMillis
        }

        val pendingIntent = createPendingIntent(reminderId, at)

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            // setAlarmClock es el mecanismo más confiable de Android:
            // 1. Despierta al dispositivo incluso en Doze mode profundo.
            // 2. Es 100% exacto al milisegundo (no sufre throttling ni retrasos por ahorro de energía).
            // 3. No requiere el permiso runtime SCHEDULE_EXACT_ALARM.
            // 4. Muestra el ícono de reloj en la barra de estado dando certeza al usuario.
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent),
                pendingIntent,
            )
            Log.d(TAG, "Alarma programada con setAlarmClock para id=$reminderId a las $at (triggerMillis=$triggerMillis)")
        } catch (e: Exception) {
            Log.w(TAG, "Fallo setAlarmClock para id=$reminderId, probando setExactAndAllowWhileIdle", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent,
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent,
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent,
                    )
                }
                Log.d(TAG, "Alarma programada con fallback para id=$reminderId a las $at")
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Error fatal al programar alarma para id=$reminderId", fallbackEx)
            }
        }
    }

    override suspend fun cancel(reminderId: Long, at: Instant) {
        val pendingIntent = createPendingIntent(reminderId, at)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Alarma cancelada para id=$reminderId a las $at")
    }

    /**
     * El PendingIntent de UN aviso. Android decide si dos PendingIntent son "el mismo" comparando
     * acción, data y requestCode, pero no los extras: por eso cada aviso lleva una URI propia
     * (`reminder-alarm://<id>/<millis>`). Si no, programar el segundo aviso de un recordatorio
     * pisaría al primero.
     */
    private fun createPendingIntent(reminderId: Long, at: Instant): PendingIntent {
        val atMillis = at.toEpochMilli()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            data = "reminder-alarm://$reminderId/$atMillis".toUri()
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_ALARM_AT, atMillis)
        }

        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "AlarmReminderScheduler"
        const val ACTION_REMINDER_ALARM = "com.example.uade.rememberapp.ACTION_REMINDER_ALARM"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"

        /** La hora del aviso que sonó, en epoch millis (para "Posponer"). */
        const val EXTRA_ALARM_AT = "extra_alarm_at"
    }
}

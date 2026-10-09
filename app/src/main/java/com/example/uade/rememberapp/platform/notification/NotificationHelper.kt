package com.example.uade.rememberapp.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.uade.rememberapp.R

/**
 * Gestor de notificaciones de la aplicación.
 * Crea canales y construye avisos del sistema para recordatorios y tareas en segundo plano.
 */
class NotificationHelper(private val context: Context) {

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Recordatorios",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notificaciones de recordatorios y alarmas"
            }

            val aiChannel = NotificationChannel(
                CHANNEL_AI,
                "Procesamiento IA",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Avisos de transcripción y estructuración de audio"
            }

            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(aiChannel)
        }
    }

    fun showSimpleNotification(id: Int, title: String, text: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_AI)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Sin permiso POST_NOTIFICATIONS
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders_channel"
        const val CHANNEL_AI = "ai_channel"
    }
}

package com.example.uade.rememberapp.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.Instant
import com.example.uade.rememberapp.MainActivity
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.platform.receiver.ReminderActionReceiver

/**
 * Responsable de la creación de canales y del ensamblado de notificaciones para los recordatorios
 * y tareas de IA en segundo plano.
 * Pertenece a la capa `platform` porque trata directamente con las APIs de Android.
 */
object NotificationHelper {

    const val CHANNEL_ID = "reminders_channel"
    const val CHANNEL_AI = "ai_channel"
    const val EXTRA_NAVIGATE_TO_REMINDER_ID = "extra_navigate_to_reminder_id"

    private const val PENDING_INTENT_FLAGS =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun createNotificationChannel(context: Context) {
        val name = context.getString(R.string.notification_channel_reminders_name)
        val description = context.getString(R.string.notification_channel_reminders_desc)
        val channel = NotificationChannel(
            CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            this.description = description
            enableVibration(true)
        }

        val aiChannel = NotificationChannel(
            CHANNEL_AI,
            "Procesamiento IA",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            this.description = "Avisos de transcripción y estructuración de audio"
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        notificationManager.createNotificationChannel(aiChannel)
    }

    /**
     * Muestra la notificación del aviso que sonó a las [alarmAt]. Va con el recordatorio para que
     * "Posponer" sepa qué aviso reemplazar. Hay una sola notificación por recordatorio: si suena
     * otro aviso del mismo, reemplaza a la anterior.
     */
    fun showReminderNotification(context: Context, reminder: Reminder, alarmAt: Instant) {
        createNotificationChannel(context)

        // Acción al tocar la notificación: abre la app directamente en el recordatorio
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_REMINDER_ID, reminder.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            contentIntent,
            PENDING_INTENT_FLAGS,
        )

        // Acción: "Hecho"
        val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_MARK_DONE
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 31 + 1).hashCode(),
            doneIntent,
            PENDING_INTENT_FLAGS,
        )

        // Acción: "Posponer"
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
            putExtra(ReminderActionReceiver.EXTRA_ALARM_AT, alarmAt.toEpochMilli())
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 31 + 2).hashCode(),
            snoozeIntent,
            PENDING_INTENT_FLAGS,
        )

        val title = reminder.title.takeUnless { it.isNullOrBlank() }
            ?: context.getString(R.string.notification_default_title)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .apply {
                if (!reminder.description.isNullOrBlank()) {
                    setContentText(reminder.description)
                }
            }
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_check,
                context.getString(R.string.notification_action_done),
                donePendingIntent,
            )
            .addAction(
                R.drawable.ic_timer,
                context.getString(R.string.notification_action_snooze),
                snoozePendingIntent,
            )

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(reminder.id.toInt(), builder.build())
        } catch (_: SecurityException) {
            // En Android 13+, si el usuario denegó POST_NOTIFICATIONS
        }
    }

    /**
     * Muestra la notificación de aviso por geolocalización al llegar o salir de un lugar.
     */
    fun showPlaceReminderNotification(
        context: Context,
        reminder: Reminder,
        place: Place,
        event: PlaceAlertEvent,
    ) {
        createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_REMINDER_ID, reminder.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            contentIntent,
            PENDING_INTENT_FLAGS,
        )

        // Acción: "Hecho"
        val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_MARK_DONE
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 31 + 1).hashCode(),
            doneIntent,
            PENDING_INTENT_FLAGS,
        )

        val title = reminder.title.takeUnless { it.isNullOrBlank() }
            ?: context.getString(R.string.notification_default_title)

        val eventSubtitle = when (event) {
            PlaceAlertEvent.Arrive -> context.getString(R.string.reminders_capture_trigger_arrive_at, place.name)
            PlaceAlertEvent.Leave -> context.getString(R.string.reminders_capture_trigger_leave_from, place.name)
        }

        val text = if (!reminder.description.isNullOrBlank()) {
            "$eventSubtitle • ${reminder.description}"
        } else {
            eventSubtitle
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location_on)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_check,
                context.getString(R.string.notification_action_done),
                donePendingIntent,
            )

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(reminder.id.toInt(), builder.build())
        } catch (_: SecurityException) {
            // En Android 13+, si el usuario denegó POST_NOTIFICATIONS
        }
    }

    fun showSimpleNotification(context: Context, id: Int, title: String, text: String) {
        createNotificationChannel(context)
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
}

package com.example.uade.rememberapp.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.Instant
import com.example.uade.rememberapp.MainActivity
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.Visibility
import com.example.uade.rememberapp.platform.receiver.ReminderActionReceiver

/**
 * Responsable de la creación de canales y del ensamblado de notificaciones para los recordatorios.
 * Pertenece a la capa `platform` porque trata directamente con las APIs de Android.
 */
object NotificationHelper {

    const val CHANNEL_ID = "reminders_channel"
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

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
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
            .applyVisibility(context, reminder.visibility, title)

        // Con visibilidad "Predeterminado" y portada, la notificación lleva la imagen: plegada,
        // como miniatura a la derecha; expandida, a todo el ancho. Con "Privado" o "Secreto" no:
        // la idea de esas opciones es mostrar menos, y una foto puede revelar más que el texto.
        if (reminder.visibility == Visibility.Default) {
            reminder.photoPath?.let(::decodeCover)?.let { cover ->
                builder
                    .setLargeIcon(cover)
                    .setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(cover)
                            // Expandida, la miniatura sobra: ya se ve la foto grande.
                            .bigLargeIcon(null as Bitmap?)
                            .setSummaryText(reminder.description),
                    )
            }
        }

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(reminder.id.toInt(), builder.build())
        } catch (_: SecurityException) {
            // En Android 13+, si el usuario denegó POST_NOTIFICATIONS
        }
    }

    /**
     * Lee la portada para la notificación, achicada si hace falta: el sistema descarta las
     * notificaciones con imágenes muy pesadas. Null si el archivo no existe o no se puede leer
     * (la notificación sale igual, sin imagen).
     *
     * Se llama desde AlarmReceiver, que ya corre fuera del hilo principal.
     */
    private fun decodeCover(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MaxPictureWidth) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** Ancho máximo de la imagen de la notificación (la portada se guarda a 1280 de ancho). */
    private const val MaxPictureWidth = 1024

    /**
     * Qué se ve con el teléfono bloqueado, según la [Visibility] del recordatorio:
     * - Default → todo (VISIBILITY_PUBLIC).
     * - Private → VISIBILITY_PRIVATE con una "versión pública" que tiene solo el título: Android
     *   muestra esa versión en la pantalla bloqueada en lugar de la completa.
     * - Secret → nada (VISIBILITY_SECRET).
     */
    private fun NotificationCompat.Builder.applyVisibility(
        context: Context,
        visibility: Visibility,
        title: String,
    ): NotificationCompat.Builder = when (visibility) {
        Visibility.Default -> setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        Visibility.Private -> setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(title)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build(),
        )
        Visibility.Secret -> setVisibility(NotificationCompat.VISIBILITY_SECRET)
    }
}

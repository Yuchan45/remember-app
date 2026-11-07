package com.example.uade.rememberapp.platform.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.example.uade.rememberapp.MainActivity
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AlarmRepeat
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.platform.receiver.ReminderActionReceiver
import java.time.Instant

/**
 * Responsable de la creación de canales y del ensamblado de notificaciones para los recordatorios
 * y tareas de IA en segundo plano.
 * Pertenece a la capa `platform` porque trata directamente con las APIs de Android.
 */
object NotificationHelper {

    const val CHANNEL_AI = "ai_channel"
    const val EXTRA_NAVIGATE_TO_REMINDER_ID = "extra_navigate_to_reminder_id"

    /** El canal único de antes (todo con importancia alta). Se borra al crear los nuevos. */
    private const val LEGACY_CHANNEL_ID = "reminders_channel"

    private const val PENDING_INTENT_FLAGS =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /**
     * El canal de cada [Importance]. En Android el sonido, la vibración y si la notificación
     * aparece arriba de la pantalla los decide el CANAL, no la notificación (desde Android 8), y
     * un canal no se puede cambiar después de creado: por eso hay uno por nivel, con su id fijo.
     */
    private fun channelId(importance: Importance): String = when (importance) {
        Importance.Low -> "reminders_low"
        Importance.Default -> "reminders_default"
        Importance.High -> "reminders_high"
        Importance.Critical -> "reminders_critical"
    }

    fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Baja: IMPORTANCE_MIN = sin sonido, sin vibración y sin ícono en la barra de estado.
        val low = NotificationChannel(
            channelId(Importance.Low),
            context.getString(R.string.notification_channel_low_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = context.getString(R.string.notification_channel_low_desc)
            setSound(null, null)
            enableVibration(false)
        }
        // Predeterminada: suena y vibra, pero no aparece arriba de la pantalla.
        val default = NotificationChannel(
            channelId(Importance.Default),
            context.getString(R.string.notification_channel_default_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_default_desc)
            enableVibration(true)
        }
        // Alta: además aparece como alerta emergente (heads-up) arriba de la pantalla.
        val high = NotificationChannel(
            channelId(Importance.High),
            context.getString(R.string.notification_channel_high_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_high_desc)
            enableVibration(true)
        }
        // Crítica: emergente y con el sonido de alarma por el canal de ALARMA, que suena aunque el
        // teléfono esté en silencio o vibración (usa el volumen de alarmas, no el del timbre).
        // Que se repita hasta marcarla hecha lo hace FLAG_INSISTENT en la notificación.
        val critical = NotificationChannel(
            channelId(Importance.Critical),
            context.getString(R.string.notification_channel_critical_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_critical_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 400, 800)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }

        val aiChannel = NotificationChannel(
            CHANNEL_AI,
            "Procesamiento IA",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            this.description = "Avisos de transcripción y estructuración de audio"
        }

        notificationManager.createNotificationChannels(listOf(low, default, high, critical, aiChannel))
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
    }

    /**
     * Lo que depende de la importancia en la notificación misma (el resto lo pone el canal):
     * - la prioridad, que usan los Android anteriores a los canales;
     * - en Crítica, la categoría de alarma, que no se pueda deslizar para descartarla (`ongoing`:
     *   se quita con "Hecho" o "Posponer") y FLAG_INSISTENT, que repite el sonido hasta que se
     *   toque. Tampoco se cierra sola al tocarla, para que no se pierda sin marcarla.
     */
    private fun NotificationCompat.Builder.applyImportance(importance: Importance): NotificationCompat.Builder =
        apply {
            priority = when (importance) {
                Importance.Low -> NotificationCompat.PRIORITY_MIN
                Importance.Default -> NotificationCompat.PRIORITY_DEFAULT
                Importance.High -> NotificationCompat.PRIORITY_HIGH
                Importance.Critical -> NotificationCompat.PRIORITY_MAX
            }
            if (importance == Importance.Critical) {
                setCategory(NotificationCompat.CATEGORY_ALARM)
                setOngoing(true)
                setAutoCancel(false)
            } else {
                setCategory(NotificationCompat.CATEGORY_REMINDER)
                setAutoCancel(true)
            }
        }

    /**
     * Lo que comparten las notificaciones por hora y por lugar, agregado al final: el botón de la
     * acción rápida (si tiene) y la portada (si tiene). Después construye y muestra; en Crítica
     * marca el sonido como insistente (se repite).
     */
    private fun notify(context: Context, reminder: Reminder, builder: NotificationCompat.Builder): Boolean {
        reminder.action?.let { action ->
            builder.addAction(
                quickActionIcon(action),
                quickActionLabel(context, action),
                PendingIntent.getActivity(
                    context,
                    (reminder.id * 31 + 3).hashCode(),
                    quickActionIntent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PENDING_INTENT_FLAGS,
                ),
            )
        }
        // Con portada: plegada se ve como miniatura a la derecha; expandida, a todo el ancho.
        reminder.photoPath?.let(::decodeCover)?.let { cover ->
            builder
                .setLargeIcon(cover)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(cover)
                        // Expandida, la miniatura sobra: ya se ve la foto grande.
                        .bigLargeIcon(null as Bitmap?),
                )
        }

        val notification = builder.build().apply {
            if (reminder.importance == Importance.Critical) flags = flags or Notification.FLAG_INSISTENT
        }
        return try {
            NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
            true
        } catch (_: SecurityException) {
            // En Android 13+, si el usuario denegó POST_NOTIFICATIONS.
            false
        }
    }

    /**
     * "Hecho", salvo que la lista no esté completa: ahí el botón avisa lo que va a hacer ("Marcar
     * todo y finalizar"), porque desde la notificación no se puede preguntar como en la Home. Con
     * un aviso que se repite, "Hecho" solo vale para esta vez (ver CompleteFromNotificationUseCase).
     */
    private fun doneLabel(context: Context, reminder: Reminder): String {
        val repeats = reminder.alarms.any { it.repeat != AlarmRepeat.None }
        return if (!repeats && reminder.items.any { !it.isChecked }) {
            context.getString(R.string.notification_action_complete_all)
        } else {
            context.getString(R.string.notification_action_done)
        }
    }

    /**
     * El Intent que abre la app que corresponde a la acción rápida, ya con el destinatario (y el
     * texto, si tiene). Abre la app lista para enviar o llamar, pero no lo hace sola: el usuario
     * confirma ahí (por eso ACTION_DIAL y no ACTION_CALL, que además pediría otro permiso).
     *
     * WhatsApp e Instagram van por sus links web (wa.me, ig.me): si la app está instalada los
     * abre ella y, si no, el navegador. wa.me necesita el número con código de país.
     */
    private fun quickActionIntent(action: NotificationAction): Intent = when (action) {
        is NotificationAction.Call -> Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(action.phone)}".toUri())
        is NotificationAction.Message -> when (action.channel) {
            MessageChannel.Sms -> Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(action.to)}".toUri()).apply {
                action.text?.let { putExtra("sms_body", it) }
            }
            MessageChannel.WhatsApp -> {
                val digits = action.to.filter { it.isDigit() }
                val text = action.text?.let { "?text=${Uri.encode(it)}" }.orEmpty()
                Intent(Intent.ACTION_VIEW, "https://wa.me/$digits$text".toUri())
            }
            MessageChannel.Instagram ->
                Intent(Intent.ACTION_VIEW, "https://ig.me/m/${Uri.encode(action.to.removePrefix("@"))}".toUri())
        }
        is NotificationAction.Email -> Intent(Intent.ACTION_SENDTO, "mailto:${Uri.encode(action.to)}".toUri()).apply {
            action.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
    }

    private fun quickActionLabel(context: Context, action: NotificationAction): String = context.getString(
        when (action) {
            is NotificationAction.Call -> R.string.notification_action_call
            is NotificationAction.Message -> when (action.channel) {
                MessageChannel.Sms -> R.string.notification_action_sms
                MessageChannel.WhatsApp -> R.string.notification_action_whatsapp
                MessageChannel.Instagram -> R.string.notification_action_instagram
            }
            is NotificationAction.Email -> R.string.notification_action_email
        },
    )

    private fun quickActionIcon(action: NotificationAction): Int = when (action) {
        is NotificationAction.Call -> R.drawable.ic_call
        is NotificationAction.Message -> R.drawable.ic_send
        is NotificationAction.Email -> R.drawable.ic_mail
    }

    /**
     * Lee la portada para la notificación, achicada si hace falta: el sistema descarta las
     * notificaciones con imágenes muy pesadas. Null si el archivo no existe o no se puede leer
     * (la notificación sale igual, sin imagen). Ancho máximo 1024 (la portada se guarda a 1280).
     *
     * Se llama desde los receivers, que ya corren fuera del hilo principal.
     */
    private fun decodeCover(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1024) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
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
            // SINGLE_TOP + CLEAR_TOP: si la app ya está abierta no se recrea, le llega el intent
            // nuevo (MainActivity.onNewIntent) y abre el recordatorio encima de lo que estaba.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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

        val builder = NotificationCompat.Builder(context, channelId(reminder.importance))
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .apply {
                if (!reminder.description.isNullOrBlank()) {
                    setContentText(reminder.description)
                }
            }
            .applyImportance(reminder.importance)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_check,
                doneLabel(context, reminder),
                donePendingIntent,
            )
            .addAction(
                R.drawable.ic_timer,
                context.getString(R.string.notification_action_snooze),
                snoozePendingIntent,
            )

        notify(context, reminder, builder)
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
            // SINGLE_TOP + CLEAR_TOP: si la app ya está abierta no se recrea, le llega el intent
            // nuevo (MainActivity.onNewIntent) y abre el recordatorio encima de lo que estaba.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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

        val builder = NotificationCompat.Builder(context, channelId(reminder.importance))
            .setSmallIcon(R.drawable.ic_location_on)
            .setContentTitle(title)
            .setContentText(text)
            .applyImportance(reminder.importance)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_check,
                doneLabel(context, reminder),
                donePendingIntent,
            )

        if (notify(context, reminder, builder)) {
            android.util.Log.d("NotificationHelper", "Notificación de lugar emitida para recordatorio id=${reminder.id}")
        } else {
            android.util.Log.e("NotificationHelper", "No se pudo emitir la notificación para recordatorio id=${reminder.id}: permiso POST_NOTIFICATIONS denegado")
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

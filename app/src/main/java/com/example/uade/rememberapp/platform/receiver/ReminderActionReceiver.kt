package com.example.uade.rememberapp.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.uade.rememberapp.RememberApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [BroadcastReceiver] que atiende los botones de acción ("Hecho" y "Posponer") de la notificación.
 *
 * Cierra la notificación visual inmediatamente y delega la lógica de negocio a los
 * casos de uso correspondientes del dominio.
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        // Cerramos la notificación de inmediato en la barra de estado
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())

        val app = context.applicationContext as RememberApp
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_MARK_DONE -> {
                        app.container.markReminderDoneUseCase(reminderId)
                    }
                    ACTION_SNOOZE -> {
                        // Pospone por defecto 15 minutos
                        app.container.snoozeReminderUseCase(reminderId, minutes = 15)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.example.uade.rememberapp.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.example.uade.rememberapp.ACTION_SNOOZE"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}

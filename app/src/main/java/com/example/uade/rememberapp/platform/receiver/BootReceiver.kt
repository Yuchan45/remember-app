package com.example.uade.rememberapp.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.uade.rememberapp.RememberApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [BroadcastReceiver] que el sistema operativo invoca al encender o reiniciar el teléfono.
 *
 * Como las alarmas de [android.app.AlarmManager] se borran de la memoria del sistema al apagarse,
 * este receptor delega a [com.example.uade.rememberapp.domain.usecase.RescheduleRemindersUseCase]
 * para reprogramar todos los recordatorios activos pendientes.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val app = context.applicationContext as RememberApp
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.rescheduleRemindersUseCase()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

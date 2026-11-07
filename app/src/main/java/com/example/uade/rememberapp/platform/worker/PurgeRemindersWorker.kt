package com.example.uade.rememberapp.platform.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.uade.rememberapp.RememberApp
import java.util.concurrent.TimeUnit

/**
 * Una vez por día borra del Historial los recordatorios vencidos (hechos de hace más de 7 días,
 * eliminados de hace más de 15), con PurgeExpiredRemindersUseCase.
 *
 * Es WorkManager y no una alarma porque no importa la hora exacta: el sistema lo corre cuando le
 * conviene (ej. cargando), y sobrevive a reinicios del teléfono sin un BootReceiver.
 */
class PurgeRemindersWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RememberApp ?: return Result.failure()
        app.container.purgeExpiredRemindersUseCase()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "purge_expired_reminders"

        /**
         * La programa si todavía no estaba. KEEP: llamarlo en cada arranque de la app no reinicia
         * el contador de 24 h.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PurgeRemindersWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

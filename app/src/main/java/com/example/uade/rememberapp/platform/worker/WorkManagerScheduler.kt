package com.example.uade.rememberapp.platform.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * Encola el procesamiento diferido de un audio mediante WorkManager cuando hay conexión a Internet.
 */
class WorkManagerScheduler(private val context: Context) {

    fun scheduleAudioProcessing(reminderId: Long, audioPath: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<AudioProcessingWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    AudioProcessingWorker.KEY_REMINDER_ID to reminderId,
                    AudioProcessingWorker.KEY_AUDIO_PATH to audioPath,
                ),
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}

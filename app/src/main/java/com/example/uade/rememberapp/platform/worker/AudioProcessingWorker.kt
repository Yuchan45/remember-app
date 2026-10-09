package com.example.uade.rememberapp.platform.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.AiProcessingStatus
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import java.time.Instant

/**
 * Worker ejecutado por WorkManager cuando el dispositivo recupera conexión a Internet
 * para procesar en segundo plano las grabaciones de voz pendientes (Offline First).
 */
class AudioProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong(KEY_REMINDER_ID, -1L)
        val audioPath = inputData.getString(KEY_AUDIO_PATH) ?: return Result.failure()

        val app = applicationContext as? RememberApp ?: return Result.failure()
        val container = app.container

        val existing = container.reminderRepository.getById(reminderId)
            ?: return Result.failure()

        val aiResult = container.processVoiceWithAiUseCase(audioPath)

        return aiResult.fold(
            onSuccess = { analysis ->
                val proposals = analysis.proposedReminders.filter { it.isSelected }

                if (proposals.isEmpty()) {
                    container.saveReminderUseCase(
                        existing.copy(
                            title = analysis.rawTranscript.ifBlank { "Nota de voz procesada" },
                            aiStatus = AiProcessingStatus.Completed,
                        ),
                    )
                } else if (proposals.size == 1) {
                    val p = proposals.first()
                    container.saveReminderUseCase(
                        existing.copy(
                            title = p.title,
                            description = p.description,
                            type = p.type,
                            items = p.items,
                            trigger = p.trigger,
                            aiStatus = AiProcessingStatus.Completed,
                        ),
                    )
                } else {
                    // Split automático en varios recordatorios (03.B)
                    // Actualizamos el existente con el primer recordatorio
                    val first = proposals.first()
                    container.saveReminderUseCase(
                        existing.copy(
                            title = first.title,
                            description = first.description,
                            type = first.type,
                            items = first.items,
                            trigger = first.trigger,
                            aiStatus = AiProcessingStatus.Completed,
                        ),
                    )

                    // Y guardamos los restantes
                    for (i in 1 until proposals.size) {
                        val p = proposals[i]
                        val additionalReminder = Reminder(
                            title = p.title,
                            description = p.description,
                            type = p.type,
                            items = p.items,
                            trigger = p.trigger,
                            audioPath = audioPath,
                            aiStatus = AiProcessingStatus.Completed,
                            createdAt = Instant.now(),
                        )
                        container.saveReminderUseCase(additionalReminder)
                    }
                }

                container.notificationHelper.showSimpleNotification(
                    id = reminderId.toInt(),
                    title = "Nota de voz procesada",
                    text = "La IA extrajo ${proposals.size} recordatorio(s) de tu audio.",
                )

                Result.success()
            },
            onFailure = {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    container.saveReminderUseCase(
                        existing.copy(aiStatus = AiProcessingStatus.Failed),
                    )
                    Result.failure()
                }
            },
        )
    }

    companion object {
        const val KEY_REMINDER_ID = "REMINDER_ID"
        const val KEY_AUDIO_PATH = "AUDIO_PATH"
    }
}

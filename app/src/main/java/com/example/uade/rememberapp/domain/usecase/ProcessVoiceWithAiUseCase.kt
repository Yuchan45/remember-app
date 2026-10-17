package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.repository.AiReminderService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Caso de uso para transcribir y estructurar una nota de voz mediante la IA de Groq.
 */
class ProcessVoiceWithAiUseCase(
    private val aiReminderService: AiReminderService,
) {
    suspend operator fun invoke(
        audioFilePath: String,
        referenceTime: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Result<AiAnalysisResult> {
        val referenceIso = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(zoneId)
            .format(referenceTime)

        return aiReminderService.processAudio(
            audioFilePath = audioFilePath,
            referenceDateTimeIso = referenceIso,
        )
    }
}

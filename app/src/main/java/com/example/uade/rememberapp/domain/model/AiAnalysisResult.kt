package com.example.uade.rememberapp.domain.model

/**
 * Resultado completo del pipeline de IA (transcripción Whisper + estructuración LLM).
 * Contiene lo dicho por el usuario y la lista de propuestas detectadas.
 */
data class AiAnalysisResult(
    val rawTranscript: String,
    val summaryBadge: String,
    val proposedReminders: List<ProposedReminder>,
    val isChecklistCandidate: Boolean = false,
    /**
     * La grabación que se analizó, para guardarla en los recordatorios que se creen (y poder
     * volver a escucharla). null si el análisis vino de un texto pegado.
     */
    val audioPath: String? = null,
)

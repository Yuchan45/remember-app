package com.example.uade.rememberapp.ui.reminders.voice

import java.util.Locale

/**
 * Estado de la UI del modal de grabación de voz.
 */
data class VoiceCaptureUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val durationSeconds: Int = 0,
    val amplitudes: List<Float> = emptyList(),
    /** Switch: Si está activo transcribe con Groq; si no, guarda el audio directo. */
    val isTranscribeWithAiEnabled: Boolean = true,
    val liveTranscription: String = "",
    val isProcessing: Boolean = false,
    val isSaved: Boolean = false,
    /** Ruta del audio cuando termina la grabación con switch OFF para configurar en QuickCapture. */
    val completedAudioPathForConfig: String? = null,
    /** Resultado del análisis de Groq IA cuando el switch está ON. */
    val aiAnalysisResult: com.example.uade.rememberapp.domain.model.AiAnalysisResult? = null,
    /** Indica si se guardó como pendiente sin conexión para procesamiento con WorkManager. */
    val offlineSavedPending: Boolean = false,
    val errorMessage: String? = null,
    val hasAudioPermission: Boolean = false,
) {
    /** Formato mm:ss, por ejemplo "0:12" como en el mockup. */
    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
}

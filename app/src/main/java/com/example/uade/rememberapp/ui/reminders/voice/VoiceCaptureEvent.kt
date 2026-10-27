package com.example.uade.rememberapp.ui.reminders.voice

import androidx.annotation.StringRes

/**
 * Eventos puntuales de un solo uso emitidos por [VoiceCaptureViewModel]
 * mediante [kotlinx.coroutines.flow.SharedFlow] hacia la UI (Compose).
 */
sealed interface VoiceCaptureEvent {

    /**
     * Solicita mostrar un Snackbar con un mensaje educativo/de error y una acción opcional.
     */
    data class ShowSnackbar(
        @get:StringRes val messageResId: Int,
        @get:StringRes val actionLabelResId: Int? = null,
    ) : VoiceCaptureEvent
}

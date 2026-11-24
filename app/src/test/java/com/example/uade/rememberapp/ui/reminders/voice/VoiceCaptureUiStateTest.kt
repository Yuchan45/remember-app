package com.example.uade.rememberapp.ui.reminders.voice

import com.example.uade.rememberapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCaptureUiStateTest {

    @Test
    fun `formattedDuration formatea correctamente segundos en mm-ss`() {
        assertEquals("0:00", VoiceCaptureUiState(durationSeconds = 0).formattedDuration)
        assertEquals("0:09", VoiceCaptureUiState(durationSeconds = 9).formattedDuration)
        assertEquals("0:12", VoiceCaptureUiState(durationSeconds = 12).formattedDuration)
        assertEquals("1:05", VoiceCaptureUiState(durationSeconds = 65).formattedDuration)
        assertEquals("10:00", VoiceCaptureUiState(durationSeconds = 600).formattedDuration)
    }

    @Test
    fun `switch de IA arranca activo por defecto`() {
        val state = VoiceCaptureUiState()
        assertTrue(state.isTranscribeWithAiEnabled)
    }

    @Test
    fun `switch de IA se puede desactivar para solo guardar audio`() {
        val state = VoiceCaptureUiState(isTranscribeWithAiEnabled = false)
        assertFalse(state.isTranscribeWithAiEnabled)
    }

    @Test
    fun `evento ShowSnackbar contiene mensaje y accion para volver a grabar`() {
        val event = VoiceCaptureEvent.ShowSnackbar(
            messageResId = R.string.voice_capture_error_empty_or_unintelligible,
            actionLabelResId = R.string.voice_capture_action_retry,
        )
        assertEquals(R.string.voice_capture_error_empty_or_unintelligible, event.messageResId)
        assertEquals(R.string.voice_capture_action_retry, event.actionLabelResId)
    }
}

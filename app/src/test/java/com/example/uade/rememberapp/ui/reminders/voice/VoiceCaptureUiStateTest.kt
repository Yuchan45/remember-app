package com.example.uade.rememberapp.ui.reminders.voice

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
}

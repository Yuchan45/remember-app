package com.example.uade.rememberapp.ui.reminders.voice.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Colores del visualizador inspirados en la imagen 2
private val WaveformActiveColor = Color(0xFFEF5350)
private val WaveformInactiveColor = Color(0xFF3B4B56)
private const val TOTAL_BARS = 28

/**
 * Visualizador de ondas de audio (ecualizador).
 * Dibuja barras verticales cuya altura representa la amplitud reciente de la voz.
 */
@Composable
fun VoiceWaveform(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    // Rellenamos hasta TOTAL_BARS con valores mínimos para que siempre tenga la misma estructura
    val paddedAmplitudes = if (amplitudes.size >= TOTAL_BARS) {
        amplitudes.takeLast(TOTAL_BARS)
    } else {
        List(TOTAL_BARS - amplitudes.size) { 0.05f } + amplitudes
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        paddedAmplitudes.forEachIndexed { index, amp ->
            val animatedHeightFraction by animateFloatAsState(
                targetValue = if (isRecording) amp.coerceIn(0.08f, 1f) else 0.08f,
                label = "waveform_bar_$index",
            )

            // Las barras más recientes (a la derecha o en el centro) son más activas
            val isRecent = index >= TOTAL_BARS / 2 || amp > 0.15f
            val barColor = if (isRecent && isRecording) WaveformActiveColor else WaveformInactiveColor

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((animatedHeightFraction * 52).dp)
                    .background(
                        color = barColor,
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

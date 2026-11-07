package com.example.uade.rememberapp.ui.reminders.detail.components

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import kotlinx.coroutines.delay
import java.io.File

/**
 * La nota de voz de la que salió el recordatorio, en el detalle:
 * ```
 * ┌──────────────────────────────────────────┐
 * │ (▶)  Nota de voz               0:07 / 0:12│
 * │      ━━━━━━━━━━━━━━━━━━━━━━──────────── │
 * │ Ver transcripción ⌄                       │
 * │ "A las 9, ir al supermercado y comprar…"  │  ← desplegada
 * └──────────────────────────────────────────┘
 * ```
 * Es de solo lectura: se escucha y se lee, no se edita (es lo que el usuario dijo).
 *
 * El reproductor es un [MediaPlayer] de Android que se crea recién al tocar ▶ y se libera al
 * salir de la pantalla. Si el archivo ya no existe (ej. se borró), se muestra solo el texto.
 */
@Composable
fun VoiceNoteSection(
    audioPath: String?,
    transcript: String?,
    modifier: Modifier = Modifier,
) {
    val audioFile = remember(audioPath) { audioPath?.let(::File)?.takeIf { it.exists() } }
    var showTranscript by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (audioFile != null) {
                AudioPlayerRow(audioFile = audioFile)
            } else {
                Text(
                    text = stringResource(R.string.reminder_detail_voice_note),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (!transcript.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { showTranscript = !showTranscript }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (showTranscript) R.string.reminder_detail_hide_transcript else R.string.reminder_detail_show_transcript,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        painter = painterResource(if (showTranscript) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                AnimatedVisibility(visible = showTranscript) {
                    Text(
                        text = "“${transcript.trim()}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** ▶/❚❚, el título, el tiempo y una barra de avance. */
@Composable
private fun AudioPlayerRow(audioFile: File) {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Al salir de la pantalla se libera el reproductor (si no, sigue sonando y ocupa memoria).
    DisposableEffect(audioFile) {
        onDispose {
            player?.release()
            player = null
        }
    }
    // Mientras suena, se actualiza el tiempo unas veces por segundo.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            player?.let {
                positionMs = it.currentPosition
                progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
            }
            delay(200)
        }
    }

    fun togglePlay() {
        val current = player ?: try {
            MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    isPlaying = false
                    positionMs = 0
                    progress = 0f
                }
            }.also {
                player = it
                durationMs = it.duration
            }
        } catch (e: Exception) {
            // Archivo dañado o formato no soportado: no hay nada que reproducir.
            return
        }
        if (current.isPlaying) {
            current.pause()
            isPlaying = false
        } else {
            current.start()
            isPlaying = true
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledIconButton(
            onClick = ::togglePlay,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                contentDescription = stringResource(
                    if (isPlaying) R.string.reminder_detail_pause_audio else R.string.reminder_detail_play_audio,
                ),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.reminder_detail_voice_note),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (durationMs > 0) {
                    Text(
                        text = "${formatMs(positionMs)} / ${formatMs(durationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 7_500 ms → "0:07". */
private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Preview(showBackground = true, backgroundColor = 0xFF0B141B)
@Composable
private fun VoiceNoteSectionPreview() {
    RememberAppTheme {
        // Sin archivo real: la preview muestra solo el título y la transcripción.
        VoiceNoteSection(
            audioPath = null,
            transcript = "A las 9, ir al supermercado y comprar carne.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

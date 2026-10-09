package com.example.uade.rememberapp.ui.reminders.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.reminders.voice.components.VoiceWaveform
import com.example.uade.rememberapp.ui.theme.CardSurface
import com.example.uade.rememberapp.ui.theme.Outline
import com.example.uade.rememberapp.ui.theme.Primary
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.SurfaceVariant

import com.example.uade.rememberapp.domain.model.AiAnalysisResult

private val RecordingRed = Color(0xFFEF5350)
private val StopButtonColor = Color(0xFFEF5350)
private val ControlButtonBg = Color(0xFF243642)

/**
 * Modal que sube desde abajo al presionar el micrófono para grabar audio.
 * Diseñado fielmente al mockup de la imagen 2 y conectado con Groq IA (imagen 03.B).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCaptureSheet(
    onDismiss: () -> Unit,
    onAudioRecordedForConfig: (audioPath: String) -> Unit = {},
    onAiAnalysisReady: (AiAnalysisResult) -> Unit = {},
    onOfflineSavedPending: () -> Unit = {},
    viewModel: VoiceCaptureViewModel = viewModel(factory = VoiceCaptureViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(hasPerm)
        if (!hasPerm) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            sheetState.hide()
            onDismiss()
        }
    }

    LaunchedEffect(uiState.completedAudioPathForConfig) {
        val audioPath = uiState.completedAudioPathForConfig
        if (audioPath != null) {
            sheetState.hide()
            viewModel.onReset()
            onAudioRecordedForConfig(audioPath)
        }
    }

    LaunchedEffect(uiState.aiAnalysisResult) {
        val result = uiState.aiAnalysisResult
        if (result != null) {
            onAiAnalysisReady(result)
            viewModel.onReset()
        }
    }

    LaunchedEffect(uiState.offlineSavedPending) {
        if (uiState.offlineSavedPending) {
            sheetState.hide()
            viewModel.onReset()
            onOfflineSavedPending()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.onDiscard()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Outline),
            )
        },
    ) {
        VoiceCaptureContent(
            uiState = uiState,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onDiscard = viewModel::onDiscard,
            onStopAndSave = viewModel::onStopAndSave,
            onPauseToggle = viewModel::onPauseToggle,
            onSwitchAiToggled = viewModel::onSwitchAiToggled,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
fun VoiceCaptureContent(
    uiState: VoiceCaptureUiState,
    onRequestPermission: () -> Unit,
    onDiscard: () -> Unit,
    onStopAndSave: () -> Unit,
    onPauseToggle: () -> Unit,
    onSwitchAiToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!uiState.hasAudioPermission) {
            // Estado si falta permiso de micrófono
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.voice_capture_permission_required),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRequestPermission) {
                    Text(text = stringResource(R.string.voice_capture_grant_permission))
                }
            }
            return
        }

        // Fila superior: "🔴 Grabando" / "⏸ Pausado" a la izquierda, Temporizador "0:12" a la derecha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (uiState.isPaused) Color.Gray else RecordingRed,
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = if (uiState.isPaused) {
                        stringResource(R.string.voice_capture_paused)
                    } else {
                        stringResource(R.string.voice_capture_recording)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
            }

            Text(
                text = uiState.formattedDuration,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Onda sonara / Ecualizador
        VoiceWaveform(
            amplitudes = uiState.amplitudes,
            isRecording = uiState.isRecording && !uiState.isPaused,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        // Switch para decidir si transcribe con IA Groq o solo guarda audio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (uiState.isTranscribeWithAiEnabled) {
                    stringResource(R.string.voice_capture_switch_ai)
                } else {
                    stringResource(R.string.voice_capture_switch_audio_only)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = uiState.isTranscribeWithAiEnabled,
                onCheckedChange = onSwitchAiToggled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }

        // Tarjeta de Transcripción / Información
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Outline, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = CardSurface,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (uiState.isTranscribeWithAiEnabled) R.drawable.ic_description else R.drawable.ic_mic,
                        ),
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (uiState.isTranscribeWithAiEnabled) {
                            stringResource(R.string.voice_capture_transcription_header)
                        } else {
                            stringResource(R.string.voice_capture_switch_audio_only)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (uiState.isProcessing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Primary,
                        )
                        Text(
                            text = "Transcribiendo y analizando con Groq IA… ✨",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Text(
                        text = if (uiState.isTranscribeWithAiEnabled) {
                            if (uiState.liveTranscription.isNotBlank()) {
                                "“${uiState.liveTranscription}”"
                            } else {
                                stringResource(R.string.voice_capture_listening)
                            }
                        } else {
                            "El audio se guardará como archivo adjunto en el recordatorio sin consumir red."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Botonera de control (Descartar, Detener central, Pausar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Botón Descartar (Basurero)
            IconButton(
                onClick = onDiscard,
                modifier = Modifier
                    .size(54.dp)
                    .background(ControlButtonBg, CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.voice_capture_discard),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Botón Principal Detener y Guardar (Coral/Rojo con cuadrado)
            IconButton(
                onClick = onStopAndSave,
                modifier = Modifier
                    .size(72.dp)
                    .background(StopButtonColor, CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_stop),
                    contentDescription = stringResource(R.string.voice_capture_stop),
                    tint = Color(0xFF0B141B),
                    modifier = Modifier.size(28.dp),
                )
            }

            // Botón Pausar / Reanudar
            IconButton(
                onClick = onPauseToggle,
                modifier = Modifier
                    .size(54.dp)
                    .background(ControlButtonBg, CircleShape),
            ) {
                Icon(
                    painter = painterResource(
                        if (uiState.isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause,
                    ),
                    contentDescription = if (uiState.isPaused) {
                        stringResource(R.string.voice_capture_resume)
                    } else {
                        stringResource(R.string.voice_capture_pause)
                    },
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Pie de página explicativo
        Text(
            text = if (uiState.isTranscribeWithAiEnabled) {
                stringResource(R.string.voice_capture_footer_ai)
            } else {
                stringResource(R.string.voice_capture_footer_audio_only)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceCaptureContentPreview() {
    RememberAppTheme {
        VoiceCaptureContent(
            uiState = VoiceCaptureUiState(
                isRecording = true,
                durationSeconds = 12,
                amplitudes = listOf(0.1f, 0.4f, 0.8f, 0.5f, 0.3f, 0.7f, 0.9f, 0.4f),
                isTranscribeWithAiEnabled = true,
                liveTranscription = "Mañana a las 9 viene el técnico del aire y después tengo que acordarme de comprar papas...",
                hasAudioPermission = true,
            ),
            onRequestPermission = {},
            onDiscard = {},
            onStopAndSave = {},
            onPauseToggle = {},
            onSwitchAiToggled = {},
        )
    }
}

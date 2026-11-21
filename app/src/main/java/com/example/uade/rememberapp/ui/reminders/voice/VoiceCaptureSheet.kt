package com.example.uade.rememberapp.ui.reminders.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.platform.permissions.PermissionsHelper
import com.example.uade.rememberapp.platform.permissions.findActivity
import com.example.uade.rememberapp.ui.components.ContextualPermission
import com.example.uade.rememberapp.ui.components.ContextualPermissionDialog
import com.example.uade.rememberapp.ui.reminders.voice.components.VoiceWaveform
import com.example.uade.rememberapp.ui.theme.CardSurface
import com.example.uade.rememberapp.ui.theme.Outline
import com.example.uade.rememberapp.ui.theme.Primary
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

private val RecordingRed = Color(0xFFEF5350)
private val StopButtonColor = Color(0xFFEF5350)
private val ControlButtonBg = Color(0xFF243642)

/**
 * Modal que sube desde abajo al presionar el micrófono para grabar audio.
 * Diseñado fielmente al mockup de la imagen 2 y conectado con Groq IA (imagen 03.B).
 *
 * Escucha eventos puntuales mediante [VoiceCaptureViewModel.events] ([kotlinx.coroutines.flow.SharedFlow])
 * para mostrar advertencias con Snackbar ante audio vacío o ininteligible.
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    // LocalResources y no context.resources: se actualiza si cambia la configuración (ej. idioma).
    val resources = LocalResources.current

    fun computeCanRetryMic(): Boolean {
        val activity = context.findActivity() ?: return true
        return !PermissionsHelper.isPermanentlyDenied(activity, Manifest.permission.RECORD_AUDIO)
    }

    var canRetryMicPermission by remember { mutableStateOf(computeCanRetryMic()) }
    var showMicExplanationDialog by remember { mutableStateOf(false) }

    // Se graba recién al tocar el botón de grabar, no al abrir el modal: así se puede pensar qué
    // decir (o cerrarlo) sin que ya esté grabando. Si falta el permiso del micrófono, se solicita
    // en contexto mediante una ventana explicativa la primera vez (RNF07).
    var startAfterPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
        canRetryMicPermission = computeCanRetryMic()
        if (isGranted && startAfterPermission) viewModel.startRecording()
        startAfterPermission = false
    }
    val launchMicPermission = {
        PermissionsHelper.setMicrophonePrompted(context, true)
        val activity = context.findActivity()
        val permanentlyDenied = activity != null &&
            PermissionsHelper.isPermanentlyDenied(activity, Manifest.permission.RECORD_AUDIO)
        if (permanentlyDenied) {
            PermissionsHelper.openAppSettings(context)
        } else {
            PermissionsHelper.markPermissionRequested(context, Manifest.permission.RECORD_AUDIO)
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val onRecordClick = {
        if (uiState.hasAudioPermission) {
            viewModel.startRecording()
        } else {
            startAfterPermission = true
            showMicExplanationDialog = true
        }
    }

    // Colecta de eventos puntuales vía SharedFlow (Snackbar para audio vacío/ininteligible)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is VoiceCaptureEvent.ShowSnackbar -> {
                    val message = resources.getString(event.messageResId)
                    val actionLabel = event.actionLabelResId?.let { resources.getString(it) }
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = actionLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) onRecordClick()
                }
            }
        }
    }

    // Al abrir verifica el permiso y, solo la primera vez que se usa voz sin permiso, muestra la explicación.
    LaunchedEffect(Unit) {
        val hasPerm = PermissionsHelper.hasRecordAudioPermission(context)
        viewModel.onPermissionResult(hasPerm)
        canRetryMicPermission = computeCanRetryMic()
        if (!hasPerm && !PermissionsHelper.hasPromptedMicrophone(context)) {
            showMicExplanationDialog = true
        }
    }

    // Al volver desde Ajustes del sistema, actualiza el estado por si habilitó el micrófono.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPerm = PermissionsHelper.hasRecordAudioPermission(context)
                viewModel.onPermissionResult(hasPerm)
                canRetryMicPermission = computeCanRetryMic()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        Box(modifier = Modifier.fillMaxWidth()) {
            VoiceCaptureContent(
                uiState = uiState,
                onRequestPermission = { showMicExplanationDialog = true },
                onOpenSettings = { PermissionsHelper.openAppSettings(context) },
                canRequestRuntimePermission = canRetryMicPermission,
                onStartRecording = onRecordClick,
                onDiscard = viewModel::onDiscard,
                onStopAndSave = viewModel::onStopAndSave,
                onPauseToggle = viewModel::onPauseToggle,
                onSwitchAiToggled = viewModel::onSwitchAiToggled,
                modifier = Modifier.navigationBarsPadding(),
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }
    }

    if (showMicExplanationDialog) {
        ContextualPermissionDialog(
            permission = ContextualPermission.Microphone,
            onAllow = {
                showMicExplanationDialog = false
                launchMicPermission()
            },
            onReject = {
                showMicExplanationDialog = false
                startAfterPermission = false
                PermissionsHelper.setMicrophonePrompted(context, true)
            },
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
    onStartRecording: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    canRequestRuntimePermission: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!uiState.hasAudioPermission) {
            // Degradación si falta permiso de micrófono: explica limitación y cómo habilitarlo en Ajustes (RNF07)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.voice_capture_permission_denied_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.voice_capture_permission_denied_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (canRequestRuntimePermission) {
                        OutlinedButton(onClick = onOpenSettings) {
                            Text(text = stringResource(R.string.voice_capture_open_settings))
                        }
                        Button(onClick = onRequestPermission) {
                            Text(text = stringResource(R.string.voice_capture_retry_permission))
                        }
                    } else {
                        Button(onClick = onOpenSettings) {
                            Text(text = stringResource(R.string.voice_capture_open_settings))
                        }
                    }
                }
            }
            return
        }

        // Fila superior: "🔴 Grabando" / "⏸ Pausado" / "Listo para grabar", Temporizador "0:12" a la derecha
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
                            color = when {
                                uiState.isRecording && uiState.isPaused -> Color.Gray
                                uiState.isRecording -> RecordingRed
                                uiState.isProcessing -> Primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = when {
                        uiState.isRecording && uiState.isPaused -> stringResource(R.string.voice_capture_paused)
                        uiState.isRecording -> stringResource(R.string.voice_capture_recording)
                        uiState.isProcessing -> stringResource(R.string.voice_capture_processing)
                        else -> stringResource(R.string.voice_capture_ready_to_record)
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

        // Botonera de control (Descartar, Detener/Grabar central, Pausar)
        val isCurrentlyRecording = uiState.isRecording
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Botón Descartar (Basurero)
            IconButton(
                onClick = onDiscard,
                enabled = isCurrentlyRecording || uiState.durationSeconds > 0,
                modifier = Modifier
                    .size(54.dp)
                    .background(ControlButtonBg, CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.voice_capture_discard),
                    tint = if (isCurrentlyRecording || uiState.durationSeconds > 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }

            // Botón Principal Detener y Guardar (si graba) o Grabar (si no está grabando)
            IconButton(
                onClick = if (isCurrentlyRecording) onStopAndSave else onStartRecording,
                enabled = !uiState.isProcessing,
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        if (isCurrentlyRecording) StopButtonColor else MaterialTheme.colorScheme.primary,
                        CircleShape,
                    ),
            ) {
                Icon(
                    painter = painterResource(
                        if (isCurrentlyRecording) R.drawable.ic_stop else R.drawable.ic_mic,
                    ),
                    contentDescription = stringResource(
                        if (isCurrentlyRecording) R.string.voice_capture_stop else R.string.voice_capture_start_recording,
                    ),
                    tint = if (isCurrentlyRecording) Color(0xFF0B141B) else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }

            // Botón Pausar / Reanudar
            IconButton(
                onClick = onPauseToggle,
                enabled = isCurrentlyRecording,
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
                    tint = if (isCurrentlyRecording) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
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
                liveTranscription = "Comprar verduras y llamar al dentista a las 5...",
                hasAudioPermission = true,
            ),
            onRequestPermission = {},
            onStartRecording = {},
            onDiscard = {},
            onStopAndSave = {},
            onPauseToggle = {},
            onSwitchAiToggled = {},
        )
    }
}

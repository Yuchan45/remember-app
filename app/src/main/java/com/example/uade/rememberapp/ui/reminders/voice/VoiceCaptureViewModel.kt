package com.example.uade.rememberapp.ui.reminders.voice

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.AiProcessingStatus
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.usecase.ProcessVoiceWithAiUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.platform.audio.AudioRecorder
import com.example.uade.rememberapp.platform.worker.WorkManagerScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

/**
 * ViewModel del modal de grabación de voz.
 * Controla la grabación de audio, la medición de amplitudes para el visualizador
 * y el guardado directo o procesamiento por IA con Groq y WorkManager.
 */
class VoiceCaptureViewModel(
    private val saveReminder: SaveReminderUseCase,
    private val processVoiceWithAi: ProcessVoiceWithAiUseCase,
    private val audioRecorder: AudioRecorder,
    private val workManagerScheduler: WorkManagerScheduler,
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceCaptureUiState())
    val uiState: StateFlow<VoiceCaptureUiState> = _uiState.asStateFlow()

    private var recordingJob: Job? = null
    private var currentAudioFile: File? = null

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = granted) }
        if (granted && !_uiState.value.isRecording) {
            startRecording()
        }
    }

    fun startRecording() {
        val audioDir = File(appContext.filesDir, "audios")
        audioDir.mkdirs()
        val file = File(audioDir, "audio_${System.currentTimeMillis()}.m4a")
        currentAudioFile = file

        val started = audioRecorder.start(file)
        if (!started) {
            _uiState.update { it.copy(errorMessage = "No se pudo iniciar la grabación") }
            return
        }

        _uiState.update {
            it.copy(
                isRecording = true,
                isPaused = false,
                durationSeconds = 0,
                amplitudes = emptyList(),
                isSaved = false,
                errorMessage = null,
            )
        }

        startSampling()
    }

    private fun startSampling() {
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                delay(100)
                if (!_uiState.value.isPaused && _uiState.value.isRecording) {
                    tick++
                    val newSeconds = tick / 10
                    val maxAmp = audioRecorder.getMaxAmplitude()
                    val normAmp = (maxAmp / 32767f).coerceIn(0f, 1f)

                    _uiState.update { state ->
                        val updatedAmplitudes = (state.amplitudes + normAmp).takeLast(40)
                        state.copy(
                            durationSeconds = newSeconds,
                            amplitudes = updatedAmplitudes,
                        )
                    }
                }
            }
        }
    }

    fun onPauseToggle() {
        val currentlyPaused = _uiState.value.isPaused
        if (currentlyPaused) {
            audioRecorder.resume()
            _uiState.update { it.copy(isPaused = false) }
        } else {
            audioRecorder.pause()
            _uiState.update { it.copy(isPaused = true) }
        }
    }

    fun onDiscard() {
        recordingJob?.cancel()
        audioRecorder.cancel()
        currentAudioFile = null
        _uiState.update {
            it.copy(
                isRecording = false,
                isPaused = false,
                durationSeconds = 0,
                amplitudes = emptyList(),
                isSaved = true,
            )
        }
    }

    fun onSwitchAiToggled(enabled: Boolean) {
        _uiState.update { it.copy(isTranscribeWithAiEnabled = enabled) }
    }

    fun onStopAndSave() {
        recordingJob?.cancel()
        val file = audioRecorder.stop() ?: currentAudioFile

        if (file == null || !file.exists()) {
            _uiState.update { it.copy(errorMessage = "Grabación vacía o no guardada") }
            return
        }

        val state = _uiState.value

        if (!state.isTranscribeWithAiEnabled) {
            // Con switch apagado: emitimos la ruta para abrir la ventana de configuración (horario, lugar, etiquetas)
            _uiState.update {
                it.copy(
                    isRecording = false,
                    isPaused = false,
                    completedAudioPathForConfig = file.absolutePath,
                )
            }
        } else {
            // Con switch encendido: flujo de IA con Groq
            if (!isNetworkAvailable()) {
                // Sin conexión: guardado Offline First en Room como "Pending" y encolado en WorkManager
                viewModelScope.launch {
                    _uiState.update { it.copy(isProcessing = true) }
                    val reminder = Reminder(
                        type = ReminderType.Note,
                        title = "Nota de voz pendiente de IA",
                        audioPath = file.absolutePath,
                        aiStatus = AiProcessingStatus.Pending,
                        createdAt = Instant.now(),
                    )
                    val savedId = saveReminder(reminder)
                    workManagerScheduler.scheduleAudioProcessing(savedId, file.absolutePath)
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            isPaused = false,
                            isProcessing = false,
                            offlineSavedPending = true,
                        )
                    }
                }
            } else {
                // Con conexión: procesamos de inmediato con Whisper + LLM
                viewModelScope.launch {
                    _uiState.update { it.copy(isProcessing = true) }
                    val result = processVoiceWithAi(file.absolutePath)
                    result.fold(
                        onSuccess = { analysis ->
                            _uiState.update {
                                it.copy(
                                    isRecording = false,
                                    isPaused = false,
                                    isProcessing = false,
                                    aiAnalysisResult = analysis,
                                )
                            }
                        },
                        onFailure = {
                            val reminder = Reminder(
                                type = ReminderType.Note,
                                title = "Nota de voz (reintento pendiente)",
                                audioPath = file.absolutePath,
                                aiStatus = AiProcessingStatus.Pending,
                                createdAt = Instant.now(),
                            )
                            val savedId = saveReminder(reminder)
                            workManagerScheduler.scheduleAudioProcessing(savedId, file.absolutePath)
                            _uiState.update {
                                it.copy(
                                    isRecording = false,
                                    isPaused = false,
                                    isProcessing = false,
                                    errorMessage = "Error de red con Groq: se procesará automáticamente al reconectar",
                                    offlineSavedPending = true,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun onReset() {
        _uiState.update {
            it.copy(
                isRecording = false,
                isPaused = false,
                durationSeconds = 0,
                amplitudes = emptyList(),
                isSaved = false,
                completedAudioPathForConfig = null,
                aiAnalysisResult = null,
                offlineSavedPending = false,
                errorMessage = null,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        audioRecorder.cancel()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                VoiceCaptureViewModel(
                    saveReminder = app.container.saveReminderUseCase,
                    processVoiceWithAi = app.container.processVoiceWithAiUseCase,
                    audioRecorder = app.container.audioRecorder,
                    workManagerScheduler = app.container.workManagerScheduler,
                    appContext = app.applicationContext,
                )
            }
        }
    }
}

package com.example.uade.rememberapp.ui.reminders.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.usecase.ObservePlacesUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.model.ReminderImportance
import com.example.uade.rememberapp.ui.reminders.model.RepeatOption
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import com.example.uade.rememberapp.ui.reminders.model.resolveReminderTime
import com.example.uade.rememberapp.ui.reminders.model.toDomain
import com.example.uade.rememberapp.ui.reminders.sample.SampleQuickCapture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * ViewModel del modal de captura rápida. Guarda notas con título y, opcionalmente, fecha,
 * hora y avisos por lugar basados en los lugares reales del usuario.
 */
class QuickCaptureViewModel(
    private val saveReminder: SaveReminderUseCase,
    tagRepository: TagRepository,
    private val observePlaces: ObservePlacesUseCase,
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(SampleQuickCapture.uiState())
    val uiState: StateFlow<QuickCaptureUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePlaces().collect { places ->
                _uiState.update { it.copy(favoritePlaces = places) }
            }
        }
    }

    /**
     * Todas las etiquetas, al día. El estado solo guarda los ids elegidos; al guardar hace falta
     * el Tag completo para armar el Reminder.
     */
    private val allTags: StateFlow<List<Tag>> =
        tagRepository.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    /** Abre el panel tocado, o lo cierra si ya estaba abierto. Hay uno solo abierto a la vez. */
    fun onPanelToggle(panel: CapturePanel) {
        _uiState.update { state ->
            state.copy(expandedPanel = if (state.expandedPanel == panel) null else panel)
        }
    }

    /**
     * Se tiró de la manija hacia arriba: si el modal está en su tamaño chico (sin panel abierto
     * y sin pantalla completa), se "extiende" abriendo el panel de Fecha y hora.
     */
    fun onDragHandlePulledUp() {
        _uiState.update { state ->
            if (state.expandedPanel == null && !state.isFullScreen) {
                state.copy(expandedPanel = CapturePanel.Time)
            } else {
                state
            }
        }
    }

    /** Al cerrar el modal se descarta todo, así la próxima vez abre vacío. */
    fun onDismissed() {
        _uiState.value = SampleQuickCapture.uiState().copy(
            favoritePlaces = _uiState.value.favoritePlaces,
        )
    }

    /**
     * Tocar un atajo lo elige; tocarlo de nuevo lo deselecciona (la nota queda sin hora).
     */
    fun onTimeSelected(shortcut: TimeShortcut) {
        _uiState.update { state ->
            when {
                shortcut == TimeShortcut.PickDate -> state.copy(isDatePickerOpen = true)
                state.selectedTime == shortcut -> state.copy(selectedTime = null)
                else -> state.copy(selectedTime = shortcut)
            }
        }
    }

    fun onDatePickerDismiss() {
        _uiState.update { it.copy(isDatePickerOpen = false) }
    }

    /** "Listo" en el calendario: queda elegido "Elegir fecha…" con ese día y hora. */
    fun onDatePicked(date: LocalDate, time: LocalTime?) {
        _uiState.update {
            it.copy(
                selectedTime = TimeShortcut.PickDate,
                pickedDate = date,
                pickedTime = time,
                isDatePickerOpen = false,
            )
        }
    }

    fun onRepeatSelected(option: RepeatOption) {
        _uiState.update { it.copy(selectedRepeat = option) }
    }

    fun onPlaceSelected(placeId: Long) {
        _uiState.update { state ->
            state.copy(selectedPlaceId = if (state.selectedPlaceId == placeId) null else placeId)
        }
    }

    fun onPlaceEventSelected(event: PlaceEvent) {
        _uiState.update { it.copy(selectedPlaceEvent = event) }
    }

    fun onImportanceSelected(importance: ReminderImportance) {
        _uiState.update { it.copy(selectedImportance = importance) }
    }

    fun onSearchAddress() {
        _uiState.update { it.copy(isCreatingPlace = true) }
    }

    fun onDismissPlaceSheet() {
        _uiState.update { it.copy(isCreatingPlace = false) }
    }

    fun onTagsChanged(ids: Set<Long>) {
        _uiState.update { it.copy(selectedTagIds = ids) }
    }

    fun onVoiceClick() = Unit
    fun onPhotoClick() = Unit
    fun onChecklistClick() = Unit

    fun onFullScreenToggle() {
        _uiState.update { it.copy(isFullScreen = !it.isFullScreen) }
    }

    fun initFromAudio(audioPath: String) {
        _uiState.update {
            it.copy(
                audioPath = audioPath,
                title = it.title.ifBlank { "Nota de voz" },
            )
        }
    }

    fun onRemoveAudio() {
        _uiState.update { it.copy(audioPath = null) }
    }

    /**
     * "Guardar": arma la nota y la guarda en la base, incluyendo avisos por hora y por lugar.
     */
    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }

        val current = now()
        val at = resolveReminderTime(state.selectedTime, state.pickedDate, state.pickedTime, current)
        val finalTitle = state.title.trim().ifBlank {
            if (state.audioPath != null) "Nota de voz" else ""
        }

        val placeAlerts = state.selectedPlaceId?.let { placeId ->
            listOf(PlaceAlert(placeId = placeId, event = state.selectedPlaceEvent.toDomain()))
        } ?: emptyList()

        val reminder = Reminder(
            type = ReminderType.Note,
            title = finalTitle,
            audioPath = state.audioPath,
            tags = allTags.value.filter { it.id in state.selectedTagIds },
            alarms = listOfNotNull(at?.let { Alarm(it.toInstant()) }),
            places = placeAlerts,
            importance = state.selectedImportance.importance,
            createdAt = current.toInstant(),
        )
        viewModelScope.launch {
            saveReminder(reminder)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                QuickCaptureViewModel(
                    saveReminder = app.container.saveReminderUseCase,
                    tagRepository = app.container.tagRepository,
                    observePlaces = app.container.observePlacesUseCase,
                )
            }
        }
    }
}

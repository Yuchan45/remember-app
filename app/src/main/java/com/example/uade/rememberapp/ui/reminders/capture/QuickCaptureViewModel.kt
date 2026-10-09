package com.example.uade.rememberapp.ui.reminders.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.ui.reminders.sample.SampleQuickCapture
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * ViewModel del modal de captura rápida. Guarda notas con título y, opcionalmente, fecha y
 * hora. El resto de las opciones (lugar, repetir, etiquetas) todavía es maquetado.
 *
 * [now] es el reloj: se recibe por constructor para que los tests puedan fijar la hora.
 *
 * TODO: recibir ObservePlacesUseCase y tomar los lugares favoritos de la base de datos.
 */
class QuickCaptureViewModel(
    private val saveReminder: SaveReminderUseCase,
    tagRepository: TagRepository,
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) : ViewModel() {

    // TODO: reemplazar las opciones de ejemplo por datos reales.
    private val _uiState = MutableStateFlow(SampleQuickCapture.uiState())
    val uiState: StateFlow<QuickCaptureUiState> = _uiState.asStateFlow()

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
     * y sin pantalla completa), se "extiende" abriendo el panel de Fecha y hora. Si ya está
     * extendido no hace nada (la manija solo se estira y rebota).
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
        _uiState.value = SampleQuickCapture.uiState()
    }

    /**
     * Tocar un atajo lo elige; tocarlo de nuevo lo deselecciona (la nota queda sin hora).
     * "Elegir fecha…" en cambio abre el calendario, y queda elegido recién con "Listo".
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

    // TODO: guardar la opción elegida en el estado.
    fun onRepeatSelected(option: RepeatOption) = Unit
    fun onPlaceSelected(placeId: Long) = Unit
    fun onPlaceEventSelected(event: PlaceEvent) = Unit

    /**
     * Elige la importancia. Tocar la ya elegida no la deselecciona: siempre tiene que haber una.
     * Se guarda en el recordatorio. TODO: usarla para el aviso (canal de notificación).
     */
    fun onImportanceSelected(importance: ReminderImportance) {
        _uiState.update { it.copy(selectedImportance = importance) }
    }

    // TODO: abrir el buscador de direcciones (mapa).
    fun onSearchAddress() = Unit

    // El chip "Etiqueta" abre el panel de etiquetas con onPanelToggle(CapturePanel.Tags).

    /**
     * El panel de etiquetas cambió las asignadas (se aplica al momento, sin cerrar el panel).
     * Se guardan en el recordatorio al tocar "Guardar".
     */
    fun onTagsChanged(ids: Set<Long>) {
        _uiState.update { it.copy(selectedTagIds = ids) }
    }

    // TODO: dictado, foto y convertir en lista.
    fun onVoiceClick() = Unit
    fun onPhotoClick() = Unit
    fun onChecklistClick() = Unit

    /** Alterna entre el modal chico y pantalla completa. */
    fun onFullScreenToggle() {
        _uiState.update { it.copy(isFullScreen = !it.isFullScreen) }
    }

    /**
     * "Guardar": arma la nota y la guarda en la base. Al terminar marca [QuickCaptureUiState.isSaved]
     * para que el modal se cierre. Con el título vacío no hace nada (el botón ya se ve
     * deshabilitado).
     *
     * TODO: guardar también lugar y repetición.
     */
    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }

        val current = now()
        val at = resolveReminderTime(state.selectedTime, state.pickedDate, state.pickedTime, current)
        val reminder = Reminder(
            type = ReminderType.Note,
            title = state.title.trim(),
            // Si una elegida se borró mientras tanto, ya no está en allTags y queda afuera.
            tags = allTags.value.filter { it.id in state.selectedTagIds },
            // La nota rápida elige una sola hora; varias se agregan desde el detalle.
            alarms = listOfNotNull(at?.let { Alarm(it.toInstant()) }),
            importance = state.selectedImportance.importance,
            createdAt = current.toInstant(),
        )
        viewModelScope.launch {
            saveReminder(reminder)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    companion object {
        /** Crea el ViewModel con el caso de uso del [RememberApp.container]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                QuickCaptureViewModel(
                    saveReminder = app.container.saveReminderUseCase,
                    tagRepository = app.container.tagRepository,
                )
            }
        }
    }
}

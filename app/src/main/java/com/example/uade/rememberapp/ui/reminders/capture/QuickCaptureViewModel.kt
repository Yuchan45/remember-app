package com.example.uade.rememberapp.ui.reminders.capture

import androidx.lifecycle.ViewModel
import com.example.uade.rememberapp.ui.reminders.sample.SampleQuickCapture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime

/**
 * ViewModel del modal de captura rápida. Por ahora es solo maquetado: se puede escribir el
 * título y abrir o cerrar los paneles de opciones; elegir opciones y guardar todavía no hace
 * nada.
 *
 * TODO: recibir SaveReminderUseCase y ObservePlacesUseCase por constructor (Factory desde el
 * AppContainer) y tomar los lugares favoritos de la base de datos.
 */
class QuickCaptureViewModel : ViewModel() {

    // TODO: reemplazar las opciones de ejemplo por datos reales.
    private val _uiState = MutableStateFlow(SampleQuickCapture.uiState())
    val uiState: StateFlow<QuickCaptureUiState> = _uiState.asStateFlow()

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    /** Abre el panel tocado, o lo cierra si ya estaba abierto. Hay uno solo abierto a la vez. */
    fun onPanelToggle(panel: CapturePanel) {
        _uiState.update { state ->
            state.copy(expandedPanel = if (state.expandedPanel == panel) null else panel)
        }
    }

    /** Al cerrar el modal se descarta todo, así la próxima vez abre vacío. */
    fun onDismissed() {
        _uiState.value = SampleQuickCapture.uiState()
    }

    /** "Elegir fecha…" abre el calendario. TODO: guardar los demás atajos en el estado. */
    fun onTimeSelected(shortcut: TimeShortcut) {
        if (shortcut == TimeShortcut.PickDate) {
            _uiState.update { it.copy(isDatePickerOpen = true) }
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

    // TODO: abrir el buscador de direcciones (mapa).
    fun onSearchAddress() = Unit

    // El chip "Etiqueta" abre el panel de etiquetas con onPanelToggle(CapturePanel.Tags).

    /**
     * El panel de etiquetas cambió las asignadas (se aplica al momento, sin cerrar el panel).
     * TODO: guardarlas en el recordatorio al crear la nota.
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

    // TODO: validar (título, descripción o foto) y guardar con SaveReminderUseCase.
    fun onSave() = Unit
}

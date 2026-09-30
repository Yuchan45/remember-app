package com.example.uade.rememberapp.ui.reminders.list

import androidx.lifecycle.ViewModel
import com.example.uade.rememberapp.ui.reminders.create.CreateReminderOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel de la Home. Por ahora solo expone los datos de ejemplo: todos los eventos
 * existen (la pantalla ya está conectada) pero todavía no hacen nada.
 *
 * TODO: recibir ObserveRemindersUseCase por constructor, construirlo con un Factory que lo
 * tome del AppContainer, y armar las secciones según los filtros.
 */
class RemindersListViewModel : ViewModel() {

    // TODO: reemplazar por los datos del repositorio cuando exista la capa de datos.
    private val _uiState = MutableStateFlow(RemindersListUiState(userName = "Yu"))
    val uiState: StateFlow<RemindersListUiState> = _uiState.asStateFlow()

    // TODO: abrir el menú de opciones de cada filtro.
    fun onTypeFilterClick() = Unit
    fun onGroupingClick() = Unit
    fun onSortClick() = Unit

    // TODO: alternar entre lista y grilla (isGridLayout).
    fun onToggleLayout() = Unit

    // TODO: colapsar todas las secciones.
    fun onCollapseAll() = Unit

    // TODO: abrir la búsqueda.
    fun onSearchClick() = Unit

    // TODO: colapsar o expandir la sección [key].
    fun onSectionToggle(key: ReminderSectionKey) = Unit

    // TODO: navegar al detalle del recordatorio.
    fun onReminderClick(id: Long) = Unit

    /** Abre el modal de captura rápida (tocar el texto "Toma una nota rápida…"). */
    fun onQuickCaptureClick() {
        _uiState.update { it.copy(isQuickCaptureOpen = true) }
    }

    fun onQuickCaptureDismiss() {
        _uiState.update { it.copy(isQuickCaptureOpen = false) }
    }

    /** El botón "+": abre el menú "Crear" con los tipos de recordatorio. */
    fun onNewReminderClick() {
        _uiState.update { it.copy(isCreateMenuOpen = true) }
    }

    fun onCreateMenuDismiss() {
        _uiState.update { it.copy(isCreateMenuOpen = false) }
    }

    // TODO: abrir la creación del tipo elegido (Nota, Checklist, Audio, …). Por ahora es maquetado.
    fun onCreateOptionClick(option: CreateReminderOption) = Unit

    // TODO: dictar un recordatorio o crearlo con una foto desde la barra de captura.
    fun onVoiceCaptureClick() = Unit
    fun onPhotoCaptureClick() = Unit
}

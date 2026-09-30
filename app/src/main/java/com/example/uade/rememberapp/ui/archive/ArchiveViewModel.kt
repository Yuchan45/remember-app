package com.example.uade.rememberapp.ui.archive

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel de la sección Archivo. Por ahora es template: las listas arrancan vacías y solo
 * funciona cambiar de pestaña.
 *
 * TODO: recibir ObserveRemindersUseCase por constructor y llenar las listas con los
 * recordatorios archivados y eliminados (ReminderStatus).
 */
class ArchiveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    fun onTabSelected(tab: ArchiveTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // TODO: abrir el detalle del recordatorio.
    fun onReminderClick(id: Long) = Unit

    // TODO: volver a ponerlo en la lista principal (ReminderStatus.Active).
    fun onRestore(id: Long) = Unit

    // TODO: borrarlo para siempre (solo desde Eliminados).
    fun onDeleteForever(id: Long) = Unit
}

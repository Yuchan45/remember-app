package com.example.uade.rememberapp.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.usecase.ObservePlacesUseCase
import com.example.uade.rememberapp.domain.usecase.ObserveRemindersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la sección Archivo: llena cada pestaña con los recordatorios de ese estado
 * (eliminar no borra de la base: solo cambia el estado a [ReminderStatus.Deleted]).
 *
 * Las listas se actualizan en vivo: si se elimina un recordatorio desde la Home, aparece acá sin
 * hacer nada, porque la base vuelve a emitir y [observeReminders] filtra de nuevo.
 */
class ArchiveViewModel(
    observeReminders: ObserveRemindersUseCase,
    observePlaces: ObservePlacesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState(isLoading = true))
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePlaces().collect { places ->
                val names = places.associate { it.id to it.name }
                _uiState.update { it.copy(placeNames = names) }
            }
        }
        viewModelScope.launch {
            // combine: espera un valor de cada lista y vuelve a emitir cuando cambia cualquiera.
            combine(
                observeReminders(ReminderStatus.Archived),
                observeReminders(ReminderStatus.Deleted),
            ) { archived, deleted -> archived to deleted }
                .collect { (archived, deleted) ->
                    _uiState.update { it.copy(isLoading = false, archived = archived, deleted = deleted) }
                }
        }
    }

    fun onTabSelected(tab: ArchiveTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // TODO: abrir el detalle del recordatorio.
    fun onReminderClick(id: Long) = Unit

    // TODO: volver a ponerlo en la lista principal (ReminderStatus.Active).
    fun onRestore(id: Long) = Unit

    // TODO: borrarlo para siempre (solo desde Eliminados).
    fun onDeleteForever(id: Long) = Unit

    companion object {
        /** Crea el ViewModel con el caso de uso del [RememberApp.container]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                ArchiveViewModel(
                    observeReminders = app.container.observeRemindersUseCase,
                    observePlaces = app.container.observePlacesUseCase,
                )
            }
        }
    }
}

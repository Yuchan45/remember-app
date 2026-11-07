package com.example.uade.rememberapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.usecase.ArchiveReminderUseCase
import com.example.uade.rememberapp.domain.usecase.DeleteRemindersForeverUseCase
import com.example.uade.rememberapp.domain.usecase.MarkReminderDoneUseCase
import com.example.uade.rememberapp.domain.usecase.ObservePlacesUseCase
import com.example.uade.rememberapp.domain.usecase.ObserveRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.RestoreReminderUseCase
import com.example.uade.rememberapp.domain.usecase.TrashReminderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * ViewModel del Historial: llena cada pestaña (Hechos, Archivados, Eliminados) y maneja restaurar,
 * borrar para siempre y "Vaciar".
 *
 * Las listas se actualizan en vivo: si se elimina un recordatorio desde la Home, aparece acá sin
 * hacer nada, porque la base vuelve a emitir.
 */
class HistoryViewModel(
    observeReminders: ObserveRemindersUseCase,
    observePlaces: ObservePlacesUseCase,
    private val restoreReminder: RestoreReminderUseCase,
    private val deleteForever: DeleteRemindersForeverUseCase,
    private val markReminderDone: MarkReminderDoneUseCase,
    private val archiveReminder: ArchiveReminderUseCase,
    private val trashReminder: TrashReminderUseCase,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePlaces().collect { places ->
                val names = places.associate { it.id to it.name }
                _uiState.update { it.copy(placeNames = names) }
            }
        }
        viewModelScope.launch {
            // Un flow por pestaña, filtrado en la base; combine espera un valor de cada uno y
            // vuelve a emitir cuando cambia cualquiera.
            val flows = HistoryTab.entries.map { tab -> observeReminders(tab.bucket) }
            combine(flows) { lists -> HistoryTab.entries.zip(lists).toMap() }
                .collect { byTab ->
                    _uiState.update {
                        it.copy(isLoading = false, remindersByTab = byTab, now = now())
                    }
                }
        }
    }

    fun onTabSelected(tab: HistoryTab) {
        _uiState.update { it.copy(selectedTab = tab, emptyConfirmStep = EmptyConfirmStep.None) }
    }

    /** "Restaurar": vuelve a la Home y reprograma sus avisos. Ofrece deshacer. */
    fun onRestore(id: Long) {
        val from = _uiState.value.selectedTab
        viewModelScope.launch {
            restoreReminder(id)
            _uiState.update { it.copy(restored = RestoredReminder(id, from)) }
        }
    }

    /** "Deshacer" del aviso de restaurado: lo devuelve a la pestaña de donde salió. */
    fun onUndoRestore(id: Long) {
        val restored = _uiState.value.restored?.takeIf { it.id == id } ?: return
        viewModelScope.launch {
            when (restored.from) {
                HistoryTab.Done -> markReminderDone(id)
                HistoryTab.Archived -> archiveReminder(id)
                HistoryTab.Deleted -> trashReminder(id)
            }
        }
    }

    fun onRestoreNoticeDone(id: Long) {
        _uiState.update { state ->
            if (state.restored?.id == id) state.copy(restored = null) else state
        }
    }

    /** "Borrar" en una card: pide confirmación antes de borrarlo para siempre. */
    fun onDeleteForeverClick(id: Long) {
        _uiState.update { it.copy(pendingDeleteId = id) }
    }

    fun onDeleteForeverConfirm() {
        val id = _uiState.value.pendingDeleteId ?: return
        _uiState.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch { deleteForever(listOf(id)) }
    }

    fun onDeleteForeverDismiss() {
        _uiState.update { it.copy(pendingDeleteId = null) }
    }

    /** "Vaciar": abre la primera confirmación. */
    fun onEmptyClick() {
        if (!_uiState.value.canEmptySelectedTab) return
        _uiState.update { it.copy(emptyConfirmStep = EmptyConfirmStep.First) }
    }

    /**
     * Avanza la confirmación: de la primera a la final, y en la final borra todo lo que se ve en
     * la pestaña. Se borra lo visible (no lo que haya en la base en ese momento) para que se
     * borre exactamente lo que el usuario confirmó.
     */
    fun onEmptyConfirm() {
        val state = _uiState.value
        when (state.emptyConfirmStep) {
            EmptyConfirmStep.None -> Unit
            EmptyConfirmStep.First -> _uiState.update { it.copy(emptyConfirmStep = EmptyConfirmStep.Final) }
            EmptyConfirmStep.Final -> {
                val ids = state.visibleReminders.map { it.id }
                _uiState.update { it.copy(emptyConfirmStep = EmptyConfirmStep.None) }
                viewModelScope.launch { deleteForever(ids) }
            }
        }
    }

    fun onEmptyDismiss() {
        _uiState.update { it.copy(emptyConfirmStep = EmptyConfirmStep.None) }
    }

    companion object {
        /** Crea el ViewModel con los casos de uso del [RememberApp.container]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                HistoryViewModel(
                    observeReminders = app.container.observeRemindersUseCase,
                    observePlaces = app.container.observePlacesUseCase,
                    restoreReminder = app.container.restoreReminderUseCase,
                    deleteForever = app.container.deleteRemindersForeverUseCase,
                    markReminderDone = app.container.markReminderDoneUseCase,
                    archiveReminder = app.container.archiveReminderUseCase,
                    trashReminder = app.container.trashReminderUseCase,
                )
            }
        }
    }
}

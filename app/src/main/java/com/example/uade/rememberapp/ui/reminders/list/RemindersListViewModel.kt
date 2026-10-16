package com.example.uade.rememberapp.ui.reminders.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.usecase.ObserveRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.RestoreReminderUseCase
import com.example.uade.rememberapp.domain.usecase.TrashReminderUseCase
import com.example.uade.rememberapp.ui.reminders.create.CreateReminderOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * ViewModel de la Home. Observa los recordatorios activos de la base y los agrupa por fecha
 * ([groupByDate]); cada vez que se guarda uno, Room vuelve a emitir y la lista se actualiza sola.
 *
 * [now] es el reloj: se recibe por constructor para que los tests puedan fijar la hora.
 *
 * TODO: armar las secciones también según los filtros (tipo, grupo, orden).
 */
class RemindersListViewModel(
    private val observeReminders: ObserveRemindersUseCase,
    private val trashReminder: TrashReminderUseCase,
    private val restoreReminder: RestoreReminderUseCase,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersListUiState(userName = "Yu", isLoading = true))
    val uiState: StateFlow<RemindersListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeReminders().collect { reminders ->
                _uiState.update { state ->
                    // Las secciones se rearman, pero cada una conserva si estaba colapsada.
                    val expanded = state.sections.associate { it.key to it.isExpanded }
                    val sections = groupByDate(reminders, now()).map { section ->
                        section.copy(isExpanded = expanded[section.key] ?: true)
                    }
                    state.copy(isLoading = false, sections = sections)
                }
            }
        }
    }

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

    /** Colapsa o expande la sección [key]. */
    fun onSectionToggle(key: ReminderSectionKey) {
        _uiState.update { state ->
            state.copy(
                sections = state.sections.map {
                    if (it.key == key) it.copy(isExpanded = !it.isExpanded) else it
                },
            )
        }
    }


    // Acciones que aparecen al deslizar una card hacia la izquierda.
    // TODO: marcarlo como hecho y archivarlo (con casos de uso, como la papelera).
    fun onReminderDone(id: Long) = Unit
    fun onReminderArchive(id: Long) = Unit

    /**
     * "Papelera": lo manda a la papelera y deja anotado cuál fue, para ofrecer deshacerlo. No hace
     * falta sacarlo de la lista a mano: al cambiar en la base, el Flow de [observeReminders]
     * vuelve a emitir y la Home se rearma sin él.
     */
    fun onReminderTrash(id: Long) {
        viewModelScope.launch {
            trashReminder(id)
            _uiState.update { it.copy(trashedReminderId = id) }
        }
    }

    /** "Deshacer" en el aviso: lo devuelve a la lista. */
    fun onUndoTrash(id: Long) {
        viewModelScope.launch { restoreReminder(id) }
    }

    /**
     * El aviso de [id] terminó (se deshizo, se cerró o venció). Solo lo borra si sigue siendo el
     * último: si mientras tanto se mandó otro a la papelera, el aviso nuevo sigue en pie.
     */
    fun onTrashNoticeDone(id: Long) {
        _uiState.update { state ->
            if (state.trashedReminderId == id) state.copy(trashedReminderId = null) else state
        }
    }

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

    companion object {
        /** Crea el ViewModel con los casos de uso del [RememberApp.container]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                RemindersListViewModel(
                    observeReminders = app.container.observeRemindersUseCase,
                    trashReminder = app.container.trashReminderUseCase,
                    restoreReminder = app.container.restoreReminderUseCase,
                )
            }
        }
    }
}

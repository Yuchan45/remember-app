package com.example.uade.rememberapp.ui.reminders.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.usecase.ArchiveReminderUseCase
import com.example.uade.rememberapp.domain.usecase.GetReminderUseCase
import com.example.uade.rememberapp.domain.usecase.ObservePlacesUseCase
import com.example.uade.rememberapp.domain.usecase.MarkReminderDoneUseCase
import com.example.uade.rememberapp.domain.usecase.ObserveRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.RestoreReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
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
    private val archiveReminder: ArchiveReminderUseCase,
    private val restoreReminder: RestoreReminderUseCase,
    private val markReminderDone: MarkReminderDoneUseCase,
    private val getReminder: GetReminderUseCase,
    private val saveReminder: SaveReminderUseCase,
    observePlaces: ObservePlacesUseCase,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersListUiState(isLoading = true))
    val uiState: StateFlow<RemindersListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePlaces().collect { places ->
                val names = places.associate { it.id to it.name }
                _uiState.update { it.copy(placeNames = names) }
            }
        }
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
    /**
     * Marca el recordatorio como hecho (completado).
     * Si la nota tiene ítems sin tildar no la finaliza directo: pregunta si se marcan todos (ver
     * [onCompleteChecklistAndDone]).
     */
    fun onReminderDone(id: Long) {
        viewModelScope.launch {
            val reminder = getReminder(id) ?: return@launch
            if (reminder.items.any { !it.isChecked }) {
                _uiState.update { it.copy(incompleteChecklistReminderId = id) }
                return@launch
            }
            markReminderDone(id)
            _uiState.update { it.copy(completedReminderId = id) }
        }
    }

    /**
     * "Marcar todo como listo y mover a Hecho": tilda todos los ítems y lo finaliza. Guarda cómo
     * estaban, así el "Deshacer" del aviso lo devuelve a la Home tal cual (ver [onUndoDone]).
     */
    fun onCompleteChecklistAndDone() {
        val id = _uiState.value.incompleteChecklistReminderId ?: return
        _uiState.update { it.copy(incompleteChecklistReminderId = null) }
        viewModelScope.launch {
            val reminder = getReminder(id) ?: return@launch
            itemsBeforeCheckAll[id] = reminder.items
            saveReminder(reminder.copy(items = reminder.items.map { it.copy(isChecked = true) }))
            markReminderDone(id)
            _uiState.update { it.copy(completedReminderId = id) }
        }
    }

    fun onIncompleteChecklistDismiss() {
        _uiState.update { it.copy(incompleteChecklistReminderId = null) }
    }

    /**
     * "+N más" en una card: la expande para ver todo sin abrir el detalle. Hay una sola expandida
     * a la vez, así que expandir otra achica la anterior; tocar "Ver menos" la achica.
     */
    fun onToggleExpanded(id: Long) {
        _uiState.update { it.copy(expandedReminderId = if (it.expandedReminderId == id) null else id) }
    }

    /** Permite tildar o destildar un ítem de una checklist directamente desde la card de la Home. */
    fun onToggleChecklistItem(reminderId: Long, itemId: Long, itemIndex: Int) {
        viewModelScope.launch {
            val reminder = getReminder(reminderId) ?: return@launch
            val updatedItems = reminder.items.mapIndexed { index, item ->
                if ((itemId != 0L && item.id == itemId) || (itemId == 0L && index == itemIndex)) {
                    item.copy(isChecked = !item.isChecked)
                } else {
                    item
                }
            }
            saveReminder(reminder.copy(items = updatedItems))
        }
    }

    /**
     * Cómo estaban los ítems antes de "Marcar todo", por recordatorio, para poder deshacerlo. No
     * va en el UiState: la pantalla no lo dibuja, solo lo usa [onUndoCheckAll].
     */
    private val itemsBeforeCheckAll = mutableMapOf<Long, List<ChecklistItem>>()

    /**
     * "Marcar todo" en la card: no marca todavía, abre el diálogo de confirmación (un toque sin
     * querer no alcanza para tildar toda la lista). Marca recién con [onConfirmCheckAll].
     */
    fun onCheckAllItems(reminderId: Long) {
        _uiState.update { it.copy(pendingCheckAllReminderId = reminderId) }
    }

    fun onCheckAllDismiss() {
        _uiState.update { it.copy(pendingCheckAllReminderId = null) }
    }

    /**
     * "Marcar todo" confirmado en el diálogo: tilda todos los ítems y muestra igual "Lista
     * completa · Deshacer", por si se confirmó sin querer.
     */
    fun onConfirmCheckAll() {
        val reminderId = _uiState.value.pendingCheckAllReminderId ?: return
        _uiState.update { it.copy(pendingCheckAllReminderId = null) }
        viewModelScope.launch {
            val reminder = getReminder(reminderId) ?: return@launch
            if (reminder.items.all { it.isChecked }) return@launch
            itemsBeforeCheckAll[reminderId] = reminder.items
            saveReminder(reminder.copy(items = reminder.items.map { it.copy(isChecked = true) }))
            _uiState.update { it.copy(checkedAllReminderId = reminderId) }
        }
    }

    /** "Deshacer" del aviso: deja los ítems como estaban (los que ya estaban tildados, siguen). */
    fun onUndoCheckAll(reminderId: Long) {
        val previous = itemsBeforeCheckAll.remove(reminderId) ?: return
        viewModelScope.launch {
            val reminder = getReminder(reminderId) ?: return@launch
            saveReminder(reminder.copy(items = previous))
        }
    }

    fun onCheckAllNoticeDone(reminderId: Long) {
        itemsBeforeCheckAll.remove(reminderId)
        _uiState.update { state ->
            if (state.checkedAllReminderId == reminderId) state.copy(checkedAllReminderId = null) else state
        }
    }

    /**
     * "Deshacer" de "Recordatorio completado": vuelve a la Home. Si se completó con "Marcar todo y
     * mover a Hecho", además deja los ítems como estaban antes.
     */
    fun onUndoDone(id: Long) {
        val previousItems = itemsBeforeCheckAll.remove(id)
        viewModelScope.launch {
            restoreReminder(id)
            if (previousItems != null) {
                val reminder = getReminder(id) ?: return@launch
                saveReminder(reminder.copy(items = previousItems))
            }
        }
    }

    fun onDoneNoticeDone(id: Long) {
        itemsBeforeCheckAll.remove(id)
        _uiState.update { state ->
            if (state.completedReminderId == id) state.copy(completedReminderId = null) else state
        }
    }

    /** "Archivar": lo saca de la Home al Historial y ofrece deshacerlo, igual que la papelera. */
    fun onReminderArchive(id: Long) {
        viewModelScope.launch {
            archiveReminder(id)
            _uiState.update { it.copy(archivedReminderId = id) }
        }
    }

    fun onUndoArchive(id: Long) {
        viewModelScope.launch { restoreReminder(id) }
    }

    fun onArchiveNoticeDone(id: Long) {
        _uiState.update { state ->
            if (state.archivedReminderId == id) state.copy(archivedReminderId = null) else state
        }
    }

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
        _uiState.update { it.copy(isQuickCaptureOpen = false, capturedAudioPath = null, capturedPhotoUris = emptyList()) }
    }

    /** El botón "+": abre el menú "Crear" con los tipos de recordatorio. */
    fun onNewReminderClick() {
        _uiState.update { it.copy(isCreateMenuOpen = true) }
    }

    fun onCreateMenuDismiss() {
        _uiState.update { it.copy(isCreateMenuOpen = false) }
    }

    fun onCreateOptionClick(option: CreateReminderOption) {
        when (option) {
            CreateReminderOption.Audio -> {
                _uiState.update { it.copy(isCreateMenuOpen = false, isVoiceCaptureOpen = true) }
            }
            CreateReminderOption.Note -> {
                _uiState.update { it.copy(isCreateMenuOpen = false, isQuickCaptureOpen = true) }
            }
            else -> Unit
        }
    }

    /** Abre el modal de grabación por voz (al tocar el micrófono de la barra rápida). */
    fun onVoiceCaptureClick() {
        _uiState.update { it.copy(isVoiceCaptureOpen = true) }
    }

    fun onVoiceCaptureDismiss() {
        _uiState.update { it.copy(isVoiceCaptureOpen = false) }
    }

    /**
     * Termina la grabación de audio con el switch apagado:
     * Cierra el modal de voz y abre la ventana de configuración rápida con el audio adjunto.
     */
    fun onVoiceRecordedForConfig(audioPath: String) {
        _uiState.update {
            it.copy(
                isVoiceCaptureOpen = false,
                isQuickCaptureOpen = true,
                capturedAudioPath = audioPath,
            )
        }
    }

    /**
     * Se completó el análisis de IA por voz (Groq):
     * Cierra el modal de grabación y abre la pantalla de revisión de recordatorios (03.B / 03.D).
     */
    fun onAiAnalysisReady(result: com.example.uade.rememberapp.domain.model.AiAnalysisResult) {
        _uiState.update {
            it.copy(
                isVoiceCaptureOpen = false,
                aiAnalysisResult = result,
            )
        }
    }

    fun onAiReviewDismiss() {
        _uiState.update { it.copy(aiAnalysisResult = null) }
    }

    fun onAiReviewSuccess() {
        _uiState.update { it.copy(aiAnalysisResult = null) }
    }

    fun onOfflineVoiceSavedPending() {
        _uiState.update {
            it.copy(
                isVoiceCaptureOpen = false,
                offlineVoiceSavedNotification = true,
            )
        }
    }

    fun onDismissOfflineVoiceNotification() {
        _uiState.update { it.copy(offlineVoiceSavedNotification = false) }
    }

    /**
     * 🖼 de la barra plegada: volvió el selector de fotos con [uris]. Abre la nota rápida con esas
     * fotos ya adjuntas, para escribirle un título y guardarla. Sin fotos (canceló), no hace nada.
     */
    fun onPhotosPickedForCapture(uris: List<String>) {
        if (uris.isEmpty()) return
        _uiState.update { it.copy(isQuickCaptureOpen = true, capturedPhotoUris = uris) }
    }

    companion object {
        /** Crea el ViewModel con los casos de uso del [RememberApp.container]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                RemindersListViewModel(
                    observeReminders = app.container.observeRemindersUseCase,
                    trashReminder = app.container.trashReminderUseCase,
                    archiveReminder = app.container.archiveReminderUseCase,
                    restoreReminder = app.container.restoreReminderUseCase,
                    markReminderDone = app.container.markReminderDoneUseCase,
                    getReminder = app.container.getReminderUseCase,
                    saveReminder = app.container.saveReminderUseCase,
                    observePlaces = app.container.observePlacesUseCase,
                )
            }
        }
    }
}

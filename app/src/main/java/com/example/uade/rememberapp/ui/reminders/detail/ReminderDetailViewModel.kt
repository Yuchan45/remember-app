package com.example.uade.rememberapp.ui.reminders.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.usecase.GetReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.ui.navigation.Routes
import com.example.uade.rememberapp.ui.reminders.capture.RepeatOption
import com.example.uade.rememberapp.ui.reminders.capture.TimeShortcut
import com.example.uade.rememberapp.ui.reminders.capture.resolveReminderTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * ViewModel de la pantalla de un recordatorio. Sirve para dos casos:
 *
 * - **Existente** (`reminders/{id}`): lee el recordatorio cuyo id viene en la ruta.
 * - **Nota nueva** (`notes/new`, desde "+" → "Nota"): la ruta no trae id, así que no consulta
 *   la base; arranca vacía y se crea al tocar ✓.
 *
 * El id llega por [SavedStateHandle]: Navigation pone ahí los argumentos de la ruta, y además
 * sobrevive si Android cierra el proceso en segundo plano.
 *
 * Se editan el título y la descripción, y se guardan con ✓ (← vuelve sin guardar).
 *
 * TODO: abrir el sheet de cada opción (etiquetas, fecha y hora, ubicación…). También se va a
 * abrir por deep link desde la notificación.
 */
class ReminderDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val getReminder: GetReminderUseCase,
    private val saveReminder: SaveReminderUseCase,
    tagRepository: TagRepository,
    private val now: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    /** null = nota nueva (la ruta no trae id). */
    private val reminderId: Long? = savedStateHandle[Routes.ARG_REMINDER_ID]

    private val _uiState = MutableStateFlow(
        if (reminderId == null) ReminderDetailUiState(isLoading = false, isNew = true) else ReminderDetailUiState(),
    )
    val uiState: StateFlow<ReminderDetailUiState> = _uiState.asStateFlow()

    init {
        if (reminderId != null) {
            viewModelScope.launch {
                val reminder = getReminder(reminderId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        reminder = reminder,
                        title = reminder?.title.orEmpty(),
                        description = reminder?.description.orEmpty(),
                        tagIds = reminder?.tags.orEmpty().map { tag -> tag.id }.toSet(),
                        alarms = reminder?.alarms.orEmpty().map { alarm -> alarm.at }.sorted(),
                        importance = reminder?.importance ?: Importance.Default,
                    )
                }
            }
        }
        // Todas las etiquetas, al día: para mostrar las asignadas con su nombre y color actual
        // (si se renombra o borra una desde el panel, se ve enseguida).
        viewModelScope.launch {
            tagRepository.observeAll().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
    }

    /** Tocar "Etiquetas" en el panel de opciones abre el modal de etiquetas. */
    fun onTagsClick() {
        _uiState.update { it.copy(tagsEditor = it.tagIds) }
    }

    /** El panel del modal cambió las asignadas: solo cambia la copia del modal. */
    fun onTagsChanged(ids: Set<Long>) {
        _uiState.update { state -> state.tagsEditor?.let { state.copy(tagsEditor = ids) } ?: state }
    }

    /** "Guardar" del modal: lo asignado pasa al borrador. Se persiste recién con ✓. */
    fun onSaveTags() {
        _uiState.update { state ->
            state.tagsEditor?.let { state.copy(tagIds = it, tagsEditor = null) } ?: state
        }
    }

    /** "Cancelar" (o cerrar el modal): descarta lo asignado o quitado en el modal. */
    fun onCancelTags() {
        _uiState.update { it.copy(tagsEditor = null) }
    }

    // --- Modal "Cómo avisar" (importancia) ---------------------------------------------------
    // Mismo patrón que los otros modales: trabaja sobre una copia que "Guardar" pasa al borrador.

    /** Tocar "Comportamiento" abre el modal con el nivel actual marcado. */
    fun onBehaviorClick() {
        _uiState.update { it.copy(importanceEditor = it.importance) }
    }

    /** Elegir un nivel en el modal: solo cambia la copia del modal (siempre hay uno marcado). */
    fun onImportanceSelected(importance: Importance) {
        _uiState.update { state -> state.importanceEditor?.let { state.copy(importanceEditor = importance) } ?: state }
    }

    /** "Guardar": el nivel marcado pasa al borrador. Se persiste recién con ✓. */
    fun onSaveImportance() {
        _uiState.update { state ->
            state.importanceEditor?.let { state.copy(importance = it, importanceEditor = null) } ?: state
        }
    }

    /** "Cancelar" (o cerrar el modal): deja la importancia como estaba. */
    fun onCancelImportance() {
        _uiState.update { it.copy(importanceEditor = null) }
    }

    // --- Modal "Establecer recordatorio" -------------------------------------------------------
    // Trabaja sobre una copia (AlarmsEditor): "Guardar" la pasa al borrador `alarms` y "Cancelar"
    // la descarta. Como el resto, el borrador se guarda en la base recién con ✓.

    /**
     * Tocar "Recordatorio" abre el modal con una tarjeta por aviso. Cada hora ya elegida se
     * muestra como "Elegir fecha…" con su día y hora. Sin avisos, arranca con una tarjeta vacía.
     */
    fun onTimeClick() {
        _uiState.update { state ->
            val items = state.alarms.map { at ->
                val local = at.atZone(zone)
                AlarmDraft(
                    at = at,
                    selectedTime = TimeShortcut.PickDate,
                    pickedDate = local.toLocalDate(),
                    pickedTime = local.toLocalTime(),
                )
            }.ifEmpty { listOf(AlarmDraft()) }
            // Con una sola tarjeta, desplegada; con varias, todas plegadas (se ve el resumen).
            state.copy(alarmsEditor = AlarmsEditor(items = items, expanded = if (items.size == 1) 0 else null))
        }
    }

    /**
     * Tocar un atajo en la tarjeta [index] lo elige y calcula el aviso en ese momento (ej. "En
     * 1 hora" = ahora + 1 h). Tocarlo de nuevo lo deselecciona. "Elegir fecha…" abre el calendario.
     */
    fun onAlarmShortcut(index: Int, shortcut: TimeShortcut) {
        if (shortcut == TimeShortcut.PickDate) {
            updateEditor { it.copy(datePickerFor = index) }
            return
        }
        updateAlarm(index) { draft ->
            if (draft.selectedTime == shortcut) {
                AlarmDraft(selectedRepeat = draft.selectedRepeat)
            } else {
                draft.copy(
                    selectedTime = shortcut,
                    at = resolveAt(shortcut, date = null, time = null),
                )
            }
        }
    }

    /** "Listo" en el calendario: en la tarjeta que lo abrió queda "Elegir fecha…" con ese día y hora. */
    fun onAlarmDatePicked(date: LocalDate, time: LocalTime?) {
        val index = _uiState.value.alarmsEditor?.datePickerFor ?: return
        updateAlarm(index) {
            it.copy(
                selectedTime = TimeShortcut.PickDate,
                pickedDate = date,
                pickedTime = time,
                at = resolveAt(TimeShortcut.PickDate, date, time),
            )
        }
        updateEditor { it.copy(datePickerFor = null) }
    }

    fun onAlarmDatePickerDismiss() {
        updateEditor { it.copy(datePickerFor = null) }
    }

    /** TODO: guardar la repetición cuando exista en el modelo; por ahora solo se marca. */
    fun onAlarmRepeat(index: Int, option: RepeatOption) {
        updateAlarm(index) { it.copy(selectedRepeat = option) }
    }

    /** El chevron de la tarjeta: la despliega (y pliega la otra) o la pliega. */
    fun onToggleAlarmExpanded(index: Int) {
        updateEditor { it.copy(expanded = if (it.expanded == index) null else index) }
    }

    /** "+ Añadir recordatorio": suma una tarjeta vacía al final y la despliega. */
    fun onAddAlarm() {
        updateEditor { it.copy(items = it.items + AlarmDraft(), expanded = it.items.size) }
    }

    /** "Borrar todo": deja una sola tarjeta vacía. Al guardar, la nota queda sin avisos por hora. */
    fun onClearAlarms() {
        updateEditor { AlarmsEditor(items = listOf(AlarmDraft()), expanded = 0) }
    }

    /** "Cancelar" (o cerrar el modal): descarta lo hecho en el modal. */
    fun onCancelAlarms() {
        _uiState.update { it.copy(alarmsEditor = null) }
    }

    /**
     * "Guardar": el borrador pasa a ser lo elegido en las tarjetas, ordenado y sin tarjetas vacías
     * ni horas repetidas. Se guarda en la base recién con ✓.
     */
    fun onSaveAlarms() {
        _uiState.update { state ->
            val editor = state.alarmsEditor ?: return@update state
            state.copy(
                alarms = editor.items.mapNotNull { it.at }.distinct().sorted(),
                alarmsEditor = null,
            )
        }
    }

    private fun updateEditor(transform: (AlarmsEditor) -> AlarmsEditor) {
        _uiState.update { state -> state.copy(alarmsEditor = state.alarmsEditor?.let(transform)) }
    }

    private fun updateAlarm(index: Int, transform: (AlarmDraft) -> AlarmDraft) {
        updateEditor { editor ->
            editor.copy(items = editor.items.mapIndexed { i, draft -> if (i == index) transform(draft) else draft })
        }
    }

    /** El momento del aviso para lo elegido, con la misma regla que la nota rápida. */
    private fun resolveAt(shortcut: TimeShortcut, date: LocalDate?, time: LocalTime?): Instant? =
        resolveReminderTime(shortcut, date, time, now().atZone(zone))?.toInstant()

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    /**
     * ✓: guarda y cierra. Título y descripción vacíos se guardan como null. Las etiquetas y los
     * avisos se guardan como quedaron en sus modales.
     *
     * - Nota nueva: la crea, salvo que no tenga título ni descripción (no se crean notas vacías).
     * - Existente: guarda los cambios, salvo que no haya cambios o que la nota quede sin título,
     *   descripción ni foto (Reminder no lo permite).
     *
     * En los casos que no se guarda, solo se cierra.
     */
    fun onDone() {
        val state = _uiState.value
        val title = state.cleanTitle
        val description = state.cleanDescription
        val reminder = state.reminder

        val toSave = when {
            !state.hasUnsavedChanges -> null
            state.isNew -> Reminder(
                type = ReminderType.Note,
                title = title,
                description = description,
                tags = state.tags,
                alarms = state.alarmsToSave,
                importance = state.importance,
                createdAt = now(),
            )
            reminder != null && (title != null || description != null || reminder.photoPath != null) ->
                // Los avisos por lugar quedan como estaban (copy los conserva).
                reminder.copy(
                    title = title,
                    description = description,
                    tags = state.tags,
                    alarms = state.alarmsToSave,
                    importance = state.importance,
                )
            else -> null
        }

        if (toSave == null) {
            _uiState.update { it.copy(isSaved = true) }
            return
        }
        viewModelScope.launch {
            saveReminder(toSave)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    // TODO: compartir el recordatorio (crear link). Por ahora es maquetado.
    fun onShareClick() = Unit

    companion object {
        /** Crea el ViewModel con el id de la ruta (si hay) y los casos de uso del [RememberApp.container]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                ReminderDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    getReminder = app.container.getReminderUseCase,
                    saveReminder = app.container.saveReminderUseCase,
                    tagRepository = app.container.tagRepository,
                )
            }
        }
    }
}

package com.example.uade.rememberapp.ui.reminders.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.usecase.GetReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

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
        _uiState.update { it.copy(isTagSheetOpen = true) }
    }

    fun onTagSheetDismiss() {
        _uiState.update { it.copy(isTagSheetOpen = false) }
    }

    /** El modal de etiquetas cambió las asignadas. Se guardan recién con ✓, como el resto. */
    fun onTagsChanged(ids: Set<Long>) {
        _uiState.update { it.copy(tagIds = ids) }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    /**
     * ✓: guarda y cierra. Título y descripción vacíos se guardan como null. Las etiquetas se
     * guardan como están asignadas en el modal.
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
                createdAt = now(),
            )
            reminder != null && (title != null || description != null || reminder.photoPath != null) ->
                reminder.copy(title = title, description = description, tags = state.tags)
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

package com.example.uade.rememberapp.ui.tags

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.repository.InvalidTagNameException
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.ui.components.hueOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del panel de etiquetas: el CRUD de etiquetas (contra [TagRepository]) y qué
 * etiquetas quedan asignadas al recordatorio que se está creando.
 *
 * Cada cambio de asignación se aplica al momento (no hace falta un "Hecho" para asignar); el
 * panel se lo avisa a quien lo abrió. Crear, editar y borrar también se guardan en el
 * repositorio al momento.
 */
class TagPickerViewModel(
    private val repository: TagRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagPickerUiState())
    val uiState: StateFlow<TagPickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { tags ->
                _uiState.update { state ->
                    // Si una asignada se borró desde otro lado, deja de estar asignada.
                    val ids = tags.map { it.id }.toSet()
                    state.copy(tags = tags, assignedIds = state.assignedIds intersect ids)
                }
            }
        }
    }

    /** Al abrir el diálogo: parte de lo que ya estaba asignado y con el editor vacío. */
    fun start(initialAssigned: Set<Long>) {
        _uiState.update { state ->
            TagPickerUiState(tags = state.tags, assignedIds = initialAssigned)
        }
    }

    /** Tocar una etiqueta la pasa de Asignadas a Disponibles o al revés. */
    fun onTagClick(id: Long) {
        _uiState.update { state ->
            val assigned = if (id in state.assignedIds) state.assignedIds - id else state.assignedIds + id
            state.copy(assignedIds = assigned)
        }
    }

    /**
     * Mantener presionada una etiqueta la carga en el editor para cambiarle nombre y color, y
     * despliega el editor si estaba plegado.
     */
    fun onTagLongClick(id: Long) {
        val tag = _uiState.value.tags.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                editingId = id,
                editorName = tag.name,
                editorColorArgb = tag.colorArgb,
                editorHue = hueOf(Color(tag.colorArgb)),
                error = null,
                isCreatorExpanded = true,
            )
        }
    }

    /**
     * La flecha de arriba a la derecha: despliega o pliega el editor. Al plegar se descarta lo
     * escrito, igual que con "Cancelar".
     */
    fun onToggleCreator() {
        if (_uiState.value.isCreatorExpanded) {
            onCloseCreator()
        } else {
            _uiState.update { it.copy(isCreatorExpanded = true) }
        }
    }

    /** "Cancelar" del editor: lo pliega y descarta lo escrito. */
    fun onCloseCreator() {
        _uiState.update { it.clearedEditor().copy(isCreatorExpanded = false) }
    }

    fun onNameChange(name: String) {
        if (name.length > TagRepository.MaxNameLength) return
        _uiState.update { it.copy(editorName = name, error = null) }
    }

    fun onHueChange(hue: Float) {
        _uiState.update { it.copy(editorHue = hue, editorColorArgb = tagColorArgbForHue(hue)) }
    }

    /**
     * Crea la etiqueta del editor (y la asigna) o guarda los cambios de la que se está
     * editando. Si el nombre no sirve, muestra el error y no limpia el editor.
     */
    fun onSubmitEditor() {
        viewModelScope.launch { submitEditor() }
    }

    fun onCancelEdit() {
        _uiState.update { it.clearedEditor() }
    }

    /** Borra la etiqueta que se está editando; se puede deshacer por unos segundos. */
    fun onDeleteEditing() {
        val state = _uiState.value
        val tag = state.editingTag ?: return
        viewModelScope.launch {
            repository.delete(tag.id)
            _uiState.update {
                it.clearedEditor().copy(
                    assignedIds = it.assignedIds - tag.id,
                    recentlyDeleted = DeletedTag(tag, wasAssigned = tag.id in state.assignedIds),
                )
            }
        }
    }

    fun onUndoDelete() {
        val deleted = _uiState.value.recentlyDeleted ?: return
        viewModelScope.launch {
            repository.restore(deleted.tag)
            _uiState.update {
                it.copy(
                    assignedIds = if (deleted.wasAssigned) it.assignedIds + deleted.tag.id else it.assignedIds,
                    recentlyDeleted = null,
                )
            }
        }
    }

    /** Se venció el tiempo para deshacer. */
    fun onUndoTimeout() {
        _uiState.update { it.copy(recentlyDeleted = null) }
    }

    /**
     * "Hecho" del editor: si quedó un nombre escrito, crea la etiqueta (o guarda la que se está
     * editando) y pliega el editor. Si el nombre no sirve, muestra el error y no lo pliega.
     */
    fun onEditorDone() {
        viewModelScope.launch {
            val hasPending = _uiState.value.editorName.isNotBlank()
            if (hasPending && !submitEditor()) return@launch
            onCloseCreator()
        }
    }

    /** Crea o actualiza según el editor. Devuelve false si el nombre no pasó la validación. */
    private suspend fun submitEditor(): Boolean {
        val state = _uiState.value
        return try {
            val editing = state.editingTag
            if (editing != null) {
                repository.update(editing.copy(name = state.editorName, colorArgb = state.editorColorArgb))
                _uiState.update { it.clearedEditor() }
            } else {
                val created = repository.create(state.editorName, state.editorColorArgb)
                _uiState.update { it.clearedEditor().copy(assignedIds = it.assignedIds + created.id) }
            }
            true
        } catch (e: InvalidTagNameException) {
            _uiState.update { it.copy(error = e.reason) }
            false
        }
    }

    private fun TagPickerUiState.clearedEditor() = copy(
        editingId = null,
        editorName = "",
        editorHue = DefaultTagHue,
        editorColorArgb = tagColorArgbForHue(DefaultTagHue),
        error = null,
    )

    companion object {
        /** Crea el ViewModel con el repositorio del [RememberApp.container]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                TagPickerViewModel(app.container.tagRepository)
            }
        }
    }
}

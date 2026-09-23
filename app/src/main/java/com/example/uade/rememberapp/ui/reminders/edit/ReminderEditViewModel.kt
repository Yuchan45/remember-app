package com.example.uade.rememberapp.ui.reminders.edit

import androidx.lifecycle.ViewModel
import com.example.uade.rememberapp.domain.model.Trigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReminderEditUiState(
    val text: String = "",
    val photoPath: String? = null,
    val trigger: Trigger = Trigger.None,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
)

/** TODO: recibir CreateReminderUseCase / UpdateReminderUseCase por constructor. */
class ReminderEditViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderEditUiState())
    val uiState: StateFlow<ReminderEditUiState> = _uiState.asStateFlow()

    fun onTextChanged(text: String) = Unit

    fun onTriggerChanged(trigger: Trigger) = Unit

    fun onPhotoTaken(path: String?) = Unit

    fun onSave() = Unit
}

package com.example.uade.rememberapp.ui.reminders.detail

import androidx.lifecycle.ViewModel
import com.example.uade.rememberapp.domain.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReminderDetailUiState(
    val isLoading: Boolean = false,
    val reminder: Reminder? = null,
)

/** Se abre también por deep link desde la notificación. TODO: inyectar casos de uso. */
class ReminderDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderDetailUiState())
    val uiState: StateFlow<ReminderDetailUiState> = _uiState.asStateFlow()

    fun onToggleDone() = Unit

    fun onDeleted() = Unit
}

package com.example.uade.rememberapp.ui.reminders.list

import androidx.lifecycle.ViewModel
import com.example.uade.rememberapp.domain.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado que la pantalla observa. Loading / Empty / Content se derivan de estos campos. */
data class RemindersListUiState(
    val isLoading: Boolean = false,
    val pending: List<Reminder> = emptyList(),
    val done: List<Reminder> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && pending.isEmpty() && done.isEmpty()
}

/**
 * TODO: recibir ObserveRemindersUseCase y CompleteReminderUseCase por constructor,
 * y construirlo con un Factory que los tome del AppContainer.
 */
class RemindersListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersListUiState())
    val uiState: StateFlow<RemindersListUiState> = _uiState.asStateFlow()

    fun onReminderCompleted(id: Long, isDone: Boolean) = Unit

    fun onReminderDeleted(id: Long) = Unit
}

package com.example.uade.rememberapp.ui.reminders.list

import androidx.lifecycle.ViewModel
import com.example.uade.rememberapp.domain.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ReminderTab(val label: String) {
    Pending("Pendientes"),
    Done("Hechos"),
}

/** Estado que la pantalla observa. Loading / Empty / Content se derivan de estos campos. */
data class RemindersListUiState(
    val isLoading: Boolean = false,
    val pending: List<Reminder> = emptyList(),
    val done: List<Reminder> = emptyList(),
    val selectedTab: ReminderTab = ReminderTab.Pending,
) {
    val isEmpty: Boolean get() = !isLoading && pending.isEmpty() && done.isEmpty()

    /** Los recordatorios de la pestaña elegida. */
    val visibleReminders: List<Reminder>
        get() = when (selectedTab) {
            ReminderTab.Pending -> pending
            ReminderTab.Done -> done
        }

    fun countFor(tab: ReminderTab): Int = when (tab) {
        ReminderTab.Pending -> pending.size
        ReminderTab.Done -> done.size
    }
}

/**
 * TODO: recibir ObserveRemindersUseCase y CompleteReminderUseCase por constructor,
 * y construirlo con un Factory que los tome del AppContainer.
 */
class RemindersListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersListUiState())
    val uiState: StateFlow<RemindersListUiState> = _uiState.asStateFlow()

    fun onTabSelected(tab: ReminderTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onReminderCompleted(id: Long, isDone: Boolean) = Unit

    fun onReminderDeleted(id: Long) = Unit
}

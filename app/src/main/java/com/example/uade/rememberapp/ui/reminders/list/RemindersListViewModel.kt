package com.example.uade.rememberapp.ui.reminders.list

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * Guarda el id del texto (R.string.…) y no el texto en sí: el ViewModel no tiene Context para
 * leer recursos, y así el idioma se resuelve recién al dibujar, con stringResource().
 */
enum class ReminderTab(@get:StringRes val labelRes: Int) {
    Pending(R.string.reminders_tab_pending),
    Done(R.string.reminders_tab_done),
}

/**
 * Estado que la pantalla observa. Loading / Empty / Content se derivan de estos campos.
 *
 * Se guarda una sola lista con todos los recordatorios; "pendientes" y "hechos" no son
 * listas aparte sino filtros por [Reminder.isDone]. Así, marcar uno como hecho es solo
 * cambiarle ese campo, y aparece solo en la otra pestaña.
 */
data class RemindersListUiState(
    val isLoading: Boolean = false,
    val reminders: List<Reminder> = emptyList(),
    val selectedTab: ReminderTab = ReminderTab.Pending,
) {
    val pending: List<Reminder> get() = reminders.filter { !it.isDone }
    val done: List<Reminder> get() = reminders.filter { it.isDone }

    val isEmpty: Boolean get() = !isLoading && reminders.isEmpty()

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

    // TODO: reemplazar por los datos del repositorio cuando exista la capa de datos.
    private val _uiState = MutableStateFlow(RemindersListUiState(reminders = sampleReminders()))
    val uiState: StateFlow<RemindersListUiState> = _uiState.asStateFlow()

    fun onTabSelected(tab: ReminderTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /** Marca o desmarca un recordatorio: busca el de ese [id] y le cambia solo `isDone`. */
    fun onReminderCompleted(id: Long, isDone: Boolean) {
        _uiState.update { state ->
            state.copy(
                reminders = state.reminders.map { reminder ->
                    if (reminder.id == id) reminder.copy(isDone = isDone) else reminder
                },
            )
        }
    }

    fun onReminderDeleted(id: Long) = Unit
}

/** Datos inventados para ver la lista funcionando mientras no hay base de datos. */
private fun sampleReminders(): List<Reminder> {
    val now = Instant.now()
    return listOf(
        Reminder(id = 1, text = "Comprar el cargador que vi en la vidriera", photoPath = "sample", createdAt = now),
        Reminder(id = 2, text = "Sacar la pelota de fútbol del baúl", createdAt = now),
        Reminder(id = 3, text = "Pagar la cuota de la facu", createdAt = now),
        Reminder(id = 5, text = "", photoPath = "sample", createdAt = now),
        Reminder(id = 4, text = "Devolver el libro a la biblioteca", isDone = true, createdAt = now),
    )
}

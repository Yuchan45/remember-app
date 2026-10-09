package com.example.uade.rememberapp.ui.reminders.capture

import com.example.uade.rememberapp.ui.reminders.model.ReminderImportance
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.model.RepeatOption
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import com.example.uade.rememberapp.domain.model.Place
import java.time.LocalDate
import java.time.LocalTime

/** Panel de opciones que se despliega en el modal al tocar un chip de aviso. */
enum class CapturePanel {
    Time,
    Place,
    Tags,
    Importance,
}

/**
 * Estado del modal de captura rápida (crear una nota).
 *
 * Las listas de opciones son datos: vienen del ViewModel (por ahora de SampleQuickCapture) y
 * no están fijas en la UI, así mañana los lugares favoritos salen de la base de datos sin
 * tocar las pantallas. La selección arranca vacía; el aviso puede ser por hora, por lugar o
 * por los dos a la vez.
 */
data class QuickCaptureUiState(
    val title: String = "",
    val expandedPanel: CapturePanel? = null,
    /** true si el modal ocupa toda la pantalla (botón ⤢ de la barra de herramientas). */
    val isFullScreen: Boolean = false,

    // Opciones que ofrece el modal.
    val timeShortcuts: List<TimeShortcut> = emptyList(),
    val repeatOptions: List<RepeatOption> = emptyList(),
    val favoritePlaces: List<Place> = emptyList(),
    val placeEvents: List<PlaceEvent> = emptyList(),
    val importanceOptions: List<ReminderImportance> = emptyList(),

    // Lo que eligió el usuario.
    val selectedTime: TimeShortcut? = null,
    val selectedRepeat: RepeatOption = RepeatOption.None,
    val selectedPlaceId: Long? = null,
    val selectedPlaceEvent: PlaceEvent = PlaceEvent.Arrive,
    /** Siempre hay una elegida; arranca en la predeterminada. */
    val selectedImportance: ReminderImportance = ReminderImportance.Default,

    // "Elegir fecha…": el día y la hora (opcional) elegidos en el diálogo del calendario.
    val pickedDate: LocalDate? = null,
    val pickedTime: LocalTime? = null,
    val isDatePickerOpen: Boolean = false,

    // Etiquetas elegidas en el panel de etiquetas; se guardan con la nota.
    val selectedTagIds: Set<Long> = emptySet(),

    /** true mientras se guarda: evita guardar dos veces con un doble toque. */
    val isSaving: Boolean = false,

    /** true cuando la nota ya quedó guardada: el modal lo ve y se cierra. */
    val isSaved: Boolean = false,
) {
    /** Hace falta un título para guardar (por ahora es lo único que se puede escribir). */
    val canSave: Boolean get() = title.isNotBlank() && !isSaving && !isSaved
    val selectedPlace: Place? get() = favoritePlaces.firstOrNull { it.id == selectedPlaceId }
    val hasTime: Boolean get() = selectedTime != null
    val hasPlace: Boolean get() = selectedPlace != null
}

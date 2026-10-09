package com.example.uade.rememberapp.ui.reminders.capture

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Importance
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
 * Atajos para elegir cuándo avisar.
 *
 * TODO: los textos ("Hoy 21:00", "Sáb 10:00") son fijos; calcularlos según la hora actual
 * (ej. no ofrecer "Hoy 21:00" si ya pasaron las 21).
 */
enum class TimeShortcut(
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
) {
    InOneHour(R.drawable.ic_timer, R.string.reminders_capture_time_in_one_hour),
    Tonight(R.drawable.ic_bedtime, R.string.reminders_capture_time_tonight),
    TomorrowMorning(R.drawable.ic_light_mode, R.string.reminders_capture_time_tomorrow_morning),
    Weekend(R.drawable.ic_weekend, R.string.reminders_capture_time_weekend),
    PickDate(R.drawable.ic_calendar_month, R.string.reminders_capture_time_pick_date),
}

/** Cada cuánto se repite el aviso por hora. */
enum class RepeatOption(@get:StringRes val label: Int) {
    None(R.string.reminders_capture_repeat_none),
    Daily(R.string.reminders_capture_repeat_daily),
    Weekdays(R.string.reminders_capture_repeat_weekdays),
    Weekly(R.string.reminders_capture_repeat_weekly),
}

/**
 * Cómo se muestra cada [Importance] del dominio: ícono, nombre y descripción. El dominio no
 * puede tener recursos de Android, por eso este enum vive en la UI y apunta a su valor del
 * dominio con [importance].
 *
 * TODO: la importancia se guarda pero todavía no cambia el aviso (canal de notificación).
 */
enum class ReminderImportance(
    val importance: Importance,
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
    @get:StringRes val description: Int,
) {
    Low(
        Importance.Low,
        R.drawable.ic_notifications_off,
        R.string.reminders_capture_importance_low,
        R.string.reminders_capture_importance_low_desc,
    ),
    Default(
        Importance.Default,
        R.drawable.ic_notifications,
        R.string.reminders_capture_importance_default,
        R.string.reminders_capture_importance_default_desc,
    ),
    High(
        Importance.High,
        R.drawable.ic_notifications_active,
        R.string.reminders_capture_importance_high,
        R.string.reminders_capture_importance_high_desc,
    ),
    Critical(
        Importance.Critical,
        R.drawable.ic_alarm,
        R.string.reminders_capture_importance_critical,
        R.string.reminders_capture_importance_critical_desc,
    ),
    ;

    companion object {
        /** El de la UI para un valor del dominio. */
        fun of(importance: Importance): ReminderImportance = entries.first { it.importance == importance }
    }
}

/** Si el aviso por lugar salta al llegar o al salir. */
enum class PlaceEvent(
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
) {
    Arrive(R.drawable.ic_login, R.string.reminders_capture_place_arrive),
    Leave(R.drawable.ic_logout, R.string.reminders_capture_place_leave),
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

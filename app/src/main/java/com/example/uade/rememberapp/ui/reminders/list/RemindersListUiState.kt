package com.example.uade.rememberapp.ui.reminders.list

import androidx.annotation.StringRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder

// Los enums guardan el id del texto (R.string.…) y no el texto en sí: el ViewModel no tiene
// Context para leer recursos, y así el idioma se resuelve recién al dibujar, con stringResource().

/** Filtro "Tipo: …". */
enum class ReminderTypeFilter(@get:StringRes val label: Int) {
    All(R.string.reminders_filter_type_all),
    Notes(R.string.reminders_filter_type_notes),
    Lists(R.string.reminders_filter_type_lists),
}

/** Filtro "Grupo: …": por qué se arman las secciones. */
enum class ReminderGrouping(@get:StringRes val label: Int) {
    Date(R.string.reminders_filter_group_date),
    Label(R.string.reminders_filter_group_label_value),
    Place(R.string.reminders_filter_group_place),
}

/** Filtro "Ordenar: …". */
enum class ReminderSort(@get:StringRes val label: Int) {
    Upcoming(R.string.reminders_filter_sort_upcoming),
    Recent(R.string.reminders_filter_sort_recent),
    Title(R.string.reminders_filter_sort_title),
}

/** Secciones de la lista cuando se agrupa por fecha. */
enum class ReminderSectionKey(@get:StringRes val title: Int) {
    Today(R.string.reminders_section_today),
    Tomorrow(R.string.reminders_section_tomorrow),
    Upcoming(R.string.reminders_section_upcoming),
    NoDate(R.string.reminders_section_no_date),
}

/** Un grupo de la lista, ej. "Hoy 2", que se puede colapsar. */
data class ReminderSection(
    val key: ReminderSectionKey,
    val reminders: List<Reminder>,
    val isExpanded: Boolean = true,
)

/**
 * Estado que la Home observa.
 *
 * Por ahora las secciones vienen armadas desde los mocks; cuando se implementen los filtros,
 * el ViewModel las va a calcular a partir de todos los recordatorios y de
 * [typeFilter] / [grouping] / [sort].
 */
data class RemindersListUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val typeFilter: ReminderTypeFilter = ReminderTypeFilter.All,
    val grouping: ReminderGrouping = ReminderGrouping.Date,
    val sort: ReminderSort = ReminderSort.Upcoming,
    val isGridLayout: Boolean = false,
    val sections: List<ReminderSection> = emptyList(),
    /** Nombre de cada lugar por id, para mostrar "Al llegar a Casa" (el Trigger solo guarda el id). */
    val placeNames: Map<Long, String> = emptyMap(),
    /** Si el modal de captura rápida está abierto. Lo abre la barra "Toma una nota rápida…". */
    val isQuickCaptureOpen: Boolean = false,
    /** Si el menú "Crear" está abierto. Lo abre el botón "+" de la barra inferior. */
    val isCreateMenuOpen: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && sections.all { it.reminders.isEmpty() }
}

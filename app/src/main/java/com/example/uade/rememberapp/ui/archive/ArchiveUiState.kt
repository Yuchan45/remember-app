package com.example.uade.rememberapp.ui.archive

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder

/**
 * Las dos listas de la sección Archivo. Cada una guarda su texto, su ícono y el mensaje para
 * cuando está vacía (el id del recurso, no el texto: el ViewModel no tiene Context).
 */
enum class ArchiveTab(
    @get:StringRes val label: Int,
    @get:DrawableRes val icon: Int,
    @get:StringRes val emptyMessage: Int,
) {
    Archived(R.string.archive_tab_archived, R.drawable.ic_archive, R.string.archive_empty_archived),
    Deleted(R.string.archive_tab_deleted, R.drawable.ic_delete, R.string.archive_empty_deleted),
}

/**
 * Estado de la sección Archivo: qué pestaña se ve y los recordatorios de cada una.
 *
 * Las listas vienen del ViewModel (por ahora vacías); se filtran por [Reminder.status].
 */
data class ArchiveUiState(
    val isLoading: Boolean = false,
    val selectedTab: ArchiveTab = ArchiveTab.Archived,
    val archived: List<Reminder> = emptyList(),
    val deleted: List<Reminder> = emptyList(),
    /** Nombre de cada lugar por id, para mostrar "Al llegar a Casa" en las cards. */
    val placeNames: Map<Long, String> = emptyMap(),
) {
    /** Los recordatorios de la pestaña elegida. */
    val visibleReminders: List<Reminder>
        get() = when (selectedTab) {
            ArchiveTab.Archived -> archived
            ArchiveTab.Deleted -> deleted
        }
}

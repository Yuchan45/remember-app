package com.example.uade.rememberapp.ui.history

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderBucket
import java.time.Duration
import java.time.Instant

/**
 * Las tres listas del Historial. Cada una guarda su sección del dominio, su texto, su ícono, el
 * mensaje para cuando está vacía y el que explica cuánto duran ahí (el id del recurso, no el
 * texto: el ViewModel no tiene Context).
 */
enum class HistoryTab(
    val bucket: ReminderBucket,
    @get:StringRes val label: Int,
    @get:DrawableRes val icon: Int,
    @get:StringRes val emptyMessage: Int,
    @get:StringRes val retentionMessage: Int,
) {
    Done(
        ReminderBucket.Done,
        R.string.history_tab_done,
        R.drawable.ic_check_circle,
        R.string.history_empty_done,
        R.string.history_retention_done,
    ),
    Archived(
        ReminderBucket.Archived,
        R.string.history_tab_archived,
        R.drawable.ic_archive,
        R.string.history_empty_archived,
        R.string.history_retention_archived,
    ),
    Deleted(
        ReminderBucket.Deleted,
        R.string.history_tab_deleted,
        R.drawable.ic_delete,
        R.string.history_empty_deleted,
        R.string.history_retention_deleted,
    ),
    ;

    /** Solo las pestañas que se vacían solas se pueden vaciar a mano: lo archivado se cuida. */
    val canEmpty: Boolean get() = bucket.retention != null
}

/**
 * En qué paso está "Vaciar". Son dos confirmaciones a propósito: borrar todo no se puede deshacer,
 * y un solo toque de más no debería alcanzar.
 */
enum class EmptyConfirmStep { None, First, Final }

/** El último restaurado, para que "Deshacer" lo devuelva a la pestaña de donde salió. */
data class RestoredReminder(val id: Long, val from: HistoryTab)

/**
 * Estado del Historial: qué pestaña se ve, los recordatorios de cada una y los diálogos abiertos.
 *
 * Las listas vienen del ViewModel, ya filtradas por la base, y se actualizan solas.
 */
data class HistoryUiState(
    val isLoading: Boolean = false,
    val selectedTab: HistoryTab = HistoryTab.Done,
    val remindersByTab: Map<HistoryTab, List<Reminder>> = emptyMap(),
    /** Nombre de cada lugar por id, para mostrar "Al llegar a Casa" en las cards. */
    val placeNames: Map<Long, String> = emptyMap(),
    /** El momento de la última actualización: decide qué ya venció y cuántos días faltan. */
    val now: Instant = Instant.now(),
    val emptyConfirmStep: EmptyConfirmStep = EmptyConfirmStep.None,
    /** El recordatorio que se pidió borrar para siempre (con su diálogo abierto), o null. */
    val pendingDeleteId: Long? = null,
    /** El último restaurado, mientras se muestra "Restaurado · Deshacer". */
    val restored: RestoredReminder? = null,
) {
    /**
     * Los recordatorios de la pestaña elegida, sin los que ya vencieron: el worker los borra una
     * vez por día, y hasta entonces no deberían verse como si todavía se pudieran recuperar.
     */
    val visibleReminders: List<Reminder>
        get() = remindersByTab[selectedTab].orEmpty().filter { reminder ->
            reminder.expiresAt?.isAfter(now) ?: true
        }

    /** Si se muestra el botón "Vaciar": pestaña que se vacía sola y con algo para borrar. */
    val canEmptySelectedTab: Boolean
        get() = selectedTab.canEmpty && visibleReminders.isNotEmpty()

    /** Días que le quedan a [reminder] antes de borrarse solo (0 = hoy), o null si no vence. */
    fun daysUntilExpiry(reminder: Reminder): Long? {
        val expiresAt = reminder.expiresAt ?: return null
        val left = Duration.between(now, expiresAt)
        // Días completos que faltan: con 6 días y 3 horas, "Se borra en 6 días"; con menos de un
        // día, "Se borra hoy".
        return if (left.isNegative) 0 else left.toDays()
    }
}

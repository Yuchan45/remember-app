package com.example.uade.rememberapp.ui.reminders.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Importance

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

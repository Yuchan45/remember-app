package com.example.uade.rememberapp.ui.reminders.model

import androidx.annotation.StringRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Visibility

/**
 * Cómo se muestra cada [Visibility] del dominio: nombre y descripción. Igual que
 * [ReminderImportance], vive en la UI porque el dominio no puede tener recursos de Android.
 */
enum class ReminderVisibility(
    val visibility: Visibility,
    @get:StringRes val label: Int,
    @get:StringRes val description: Int,
) {
    Default(
        Visibility.Default,
        R.string.reminders_behavior_visibility_default,
        R.string.reminders_behavior_visibility_default_desc,
    ),
    Private(
        Visibility.Private,
        R.string.reminders_behavior_visibility_private,
        R.string.reminders_behavior_visibility_private_desc,
    ),
    Secret(
        Visibility.Secret,
        R.string.reminders_behavior_visibility_secret,
        R.string.reminders_behavior_visibility_secret_desc,
    ),
    ;

    companion object {
        /** El de la UI para un valor del dominio. */
        fun of(visibility: Visibility): ReminderVisibility = entries.first { it.visibility == visibility }
    }
}

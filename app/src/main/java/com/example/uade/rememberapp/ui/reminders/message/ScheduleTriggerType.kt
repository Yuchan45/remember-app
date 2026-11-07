package com.example.uade.rememberapp.ui.reminders.message

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.uade.rememberapp.R

/**
 * Tipo de disparador para programar el mensaje: por hora o por ubicación.
 */
enum class ScheduleTriggerType(
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
) {
    Time(
        icon = R.drawable.ic_alarm,
        label = R.string.scheduled_message_trigger_time,
    ),
    Location(
        icon = R.drawable.ic_location_on,
        label = R.string.scheduled_message_trigger_location,
    ),
}

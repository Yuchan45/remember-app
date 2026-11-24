package com.example.uade.rememberapp.ui.reminders.model

import androidx.annotation.StringRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AlarmRepeat

/**
 * Cada cuánto se repite el aviso por hora, como se ofrece en la UI: el texto y la [repeat] del
 * dominio que se guarda.
 */
enum class RepeatOption(@get:StringRes val label: Int, val repeat: AlarmRepeat) {
    None(R.string.reminders_capture_repeat_none, AlarmRepeat.None),
    Daily(R.string.reminders_capture_repeat_daily, AlarmRepeat.Daily),
    Weekdays(R.string.reminders_capture_repeat_weekdays, AlarmRepeat.Weekdays),
    Weekly(R.string.reminders_capture_repeat_weekly, AlarmRepeat.Weekly),
    ;

    companion object {
        /** La opción que corresponde a una repetición guardada. */
        fun of(repeat: AlarmRepeat): RepeatOption = entries.first { it.repeat == repeat }
    }
}

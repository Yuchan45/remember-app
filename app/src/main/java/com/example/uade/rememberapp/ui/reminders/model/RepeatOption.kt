package com.example.uade.rememberapp.ui.reminders.model

import androidx.annotation.StringRes
import com.example.uade.rememberapp.R

/** Cada cuánto se repite el aviso por hora. */
enum class RepeatOption(@get:StringRes val label: Int) {
    None(R.string.reminders_capture_repeat_none),
    Daily(R.string.reminders_capture_repeat_daily),
    Weekdays(R.string.reminders_capture_repeat_weekdays),
    Weekly(R.string.reminders_capture_repeat_weekly),
}

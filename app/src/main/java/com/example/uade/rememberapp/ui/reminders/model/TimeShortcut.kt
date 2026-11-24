package com.example.uade.rememberapp.ui.reminders.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.uade.rememberapp.R

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

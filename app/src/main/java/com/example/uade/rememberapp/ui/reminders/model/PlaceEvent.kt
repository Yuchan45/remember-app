package com.example.uade.rememberapp.ui.reminders.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent

/** Si el aviso por lugar salta al llegar o al salir. */
enum class PlaceEvent(
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
) {
    Arrive(R.drawable.ic_login, R.string.reminders_capture_place_arrive),
    Leave(R.drawable.ic_logout, R.string.reminders_capture_place_leave),
}

fun PlaceEvent.toDomain(): PlaceAlertEvent = when (this) {
    PlaceEvent.Arrive -> PlaceAlertEvent.Arrive
    PlaceEvent.Leave -> PlaceAlertEvent.Leave
}

fun PlaceAlertEvent.toUi(): PlaceEvent = when (this) {
    PlaceAlertEvent.Arrive -> PlaceEvent.Arrive
    PlaceAlertEvent.Leave -> PlaceEvent.Leave
}

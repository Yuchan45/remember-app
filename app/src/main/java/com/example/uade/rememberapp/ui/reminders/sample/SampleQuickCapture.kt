package com.example.uade.rememberapp.ui.reminders.sample

import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceKind
import com.example.uade.rememberapp.ui.reminders.capture.CapturePanel
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.capture.QuickCaptureUiState
import com.example.uade.rememberapp.ui.reminders.model.ReminderImportance
import com.example.uade.rememberapp.ui.reminders.model.RepeatOption
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut

/**
 * Opciones de ejemplo del modal de captura rápida, mientras no hay base de datos. Las usan
 * el ViewModel (temporalmente) y las previews.
 */
object SampleQuickCapture {

    /** Lugares favoritos de ejemplo (coordenadas aproximadas de CABA). */
    val favoritePlaces: List<Place> = listOf(
        Place(id = 1, name = "Casa", latitude = -34.588, longitude = -58.430, kind = PlaceKind.Home),
        Place(id = 2, name = "Trabajo", latitude = -34.603, longitude = -58.381, kind = PlaceKind.Work),
        Place(id = 3, name = "Facultad", latitude = -34.617, longitude = -58.381, kind = PlaceKind.Study),
        Place(id = 4, name = "Estacionamiento", latitude = -34.600, longitude = -58.385, kind = PlaceKind.Parking),
    )

    /** Estado con que abre el modal: todas las opciones cargadas y nada elegido. */
    fun uiState(): QuickCaptureUiState = QuickCaptureUiState(
        timeShortcuts = TimeShortcut.entries,
        repeatOptions = RepeatOption.entries,
        favoritePlaces = favoritePlaces,
        placeEvents = PlaceEvent.entries,
        importanceOptions = ReminderImportance.entries,
    )

    /** Para previews: panel de hora abierto, con "Mañana 9:00" elegido. */
    fun timePanelState(): QuickCaptureUiState = uiState().copy(
        title = "Llamar al plomero",
        expandedPanel = CapturePanel.Time,
        selectedTime = TimeShortcut.TomorrowMorning,
    )

    /** Para previews: panel de lugar abierto, "Al llegar" a Facultad. */
    fun placePanelState(): QuickCaptureUiState = uiState().copy(
        title = "Imprimir el TP de Diseño",
        expandedPanel = CapturePanel.Place,
        selectedPlaceId = 3,
    )
}

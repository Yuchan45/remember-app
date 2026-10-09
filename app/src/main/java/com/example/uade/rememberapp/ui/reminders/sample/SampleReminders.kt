package com.example.uade.rememberapp.ui.reminders.sample

import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.ui.reminders.list.ReminderSection
import com.example.uade.rememberapp.ui.reminders.list.ReminderSectionKey
import com.example.uade.rememberapp.ui.reminders.list.RemindersListUiState
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Datos inventados para ver la Home funcionando mientras no hay base de datos. Los usan el
 * ViewModel (temporalmente) y las previews.
 *
 * Las horas son relativas a `now`, así el "!" de aviso próximo y el "hoy / otro día" se ven
 * igual sin importar cuándo se abra la app.
 */
object SampleReminders {

    private val health = Tag(id = 1, name = "Salud", colorArgb = 0xFF6FCF97)
    private val home = Tag(id = 2, name = "Casa", colorArgb = 0xFF56CCF2)
    private val barbecue = Tag(id = 3, name = "Asado", colorArgb = 0xFF9B7BEA)

    private const val HOME_PLACE_ID = 1L

    val placeNames: Map<Long, String> = mapOf(HOME_PLACE_ID to "Casa")

    /** El estado inicial de la Home, igual a la captura del Figma. */
    fun uiState(now: Instant = Instant.now()): RemindersListUiState = RemindersListUiState(
        userName = "Yu",
        sections = listOf(
            ReminderSection(
                key = ReminderSectionKey.Today,
                reminders = listOf(vaccine(now), batteries(now)),
            ),
            ReminderSection(
                key = ReminderSectionKey.Tomorrow,
                reminders = listOf(barbecueList(now)),
            ),
        ),
        placeNames = placeNames,
    )

    /** Todas las variantes de card, para previsualizarlas juntas. */
    fun previewReminders(now: Instant = Instant.now()): List<Reminder> = listOf(
        vaccine(now),
        batteries(now),
        barbecueList(now),
        Reminder(id = 4, title = "Sacar turno en el banco", createdAt = now),
        Reminder(id = 5, photoPath = "sample", createdAt = now),
        Reminder(
            id = 6,
            type = ReminderType.Checklist,
            title = "Llevar al viaje",
            items = listOf(
                ChecklistItem(id = 1, text = "Cargador"),
                ChecklistItem(id = 2, text = "DNI", isChecked = true),
            ),
            alarms = listOf(Alarm(now.plus(Duration.ofDays(4)))),
            createdAt = now,
        ),
    )

    // Nota con foto y aviso en menos de una hora: muestra el "!".
    private fun vaccine(now: Instant) = Reminder(
        id = 1,
        title = "Vacuna de Tomi",
        description = "Llevar libreta sanitaria y DNI",
        photoPath = "sample",
        tags = listOf(health),
        alarms = listOf(Alarm(now.plus(Duration.ofMinutes(45)))),
        createdAt = now,
    )

    // Nota con aviso por lugar.
    private fun batteries(now: Instant) = Reminder(
        id = 2,
        title = "Comprar pilas AA",
        description = "Para el control del aire",
        tags = listOf(home),
        places = listOf(PlaceAlert(HOME_PLACE_ID)),
        createdAt = now,
    )

    // Lista con más ítems de los que entran en la card: muestra "+ 3 más".
    private fun barbecueList(now: Instant) = Reminder(
        id = 3,
        type = ReminderType.Checklist,
        title = "Compras asado",
        items = listOf(
            ChecklistItem(id = 1, text = "Vacío 5kg", note = "En el Coto"),
            ChecklistItem(id = 2, text = "Asado 3kg", note = "En Res"),
            ChecklistItem(id = 3, text = "Carbón"),
            ChecklistItem(id = 4, text = "Pan"),
            ChecklistItem(id = 5, text = "Hielo"),
        ),
        tags = listOf(barbecue),
        alarms = listOf(Alarm(tomorrowAt(now, LocalTime.of(11, 0)))),
        createdAt = now,
    )

    private fun tomorrowAt(now: Instant, time: LocalTime): Instant {
        val zone = ZoneId.systemDefault()
        return now.atZone(zone).toLocalDate().plusDays(1).atTime(time).atZone(zone).toInstant()
    }
}

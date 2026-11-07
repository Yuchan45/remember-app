package com.example.uade.rememberapp.ui.reminders.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.isDueSoon
import com.example.uade.rememberapp.domain.model.nextAlarm
import com.example.uade.rememberapp.ui.theme.DueSoonAmber
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.TriggerTimeColor
import com.example.uade.rememberapp.ui.theme.rememberAppLocale
import java.time.Duration
import java.time.Instant

/**
 * Cuándo vuelve el recordatorio, para el pie de la card:
 * - por hora, hoy:          ⏰ 18:00   (+ "!" si falta poco o ya pasó)
 * - por hora, otro día:     📅 Sáb 11:00
 * - con varias horas:       ⏰ 18:00 +2   (la próxima, y cuántas más hay)
 * - por lugar:              ➤ Al llegar a Casa / Al salir de Casa
 * Cada uno es un chip con fondo de color: lila la hora (ámbar si falta poco), celeste el lugar.
 * - hora y lugar:           los dos, uno al lado del otro
 * - sin aviso:              no dibuja nada
 *
 * [placeName] es el nombre del primer lugar: PlaceAlert solo guarda el id. Con [onTimeClick] y
 * [onPlaceClick] cada chip se puede tocar (ej. para abrir el detalle con ese modal).
 */
@Composable
fun ReminderTriggerInfo(
    reminder: Reminder,
    placeName: String?,
    now: Instant,
    modifier: Modifier = Modifier,
    onTimeClick: (() -> Unit)? = null,
    onPlaceClick: (() -> Unit)? = null,
) {
    val next = reminder.nextAlarm(now)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (next != null) {
            val locale = rememberAppLocale()
            val icon = if (isSameDay(next.at, now)) R.drawable.ic_alarm else R.drawable.ic_calendar_month
            val extra = reminder.alarms.size - 1
            val time = formatReminderTime(next.at, now, locale)
            // Cerca de la hora (o ya pasada) el chip pasa a ámbar y suma el "!", para que se note.
            val dueSoon = next.isDueSoon(now)
            TriggerChip(
                icon = icon,
                text = if (extra > 0) stringResource(R.string.reminders_trigger_more_alarms, time, extra) else time,
                color = if (dueSoon) DueSoonAmber else TriggerTimeColor,
                trailingIcon = if (dueSoon) R.drawable.ic_priority_high else null,
                trailingDescription = stringResource(R.string.reminders_trigger_due_soon),
                onClick = onTimeClick,
            )
        }
        // Sin nombre (lugar borrado o todavía sin cargar) no hay nada útil para mostrar.
        if (reminder.places.isNotEmpty() && placeName != null) {
            val isLeave = reminder.places.firstOrNull()?.event == PlaceAlertEvent.Leave
            val textRes = if (isLeave) {
                R.string.reminders_trigger_on_departure
            } else {
                R.string.reminders_trigger_on_arrival
            }
            TriggerChip(
                icon = R.drawable.ic_near_me,
                text = stringResource(textRes, placeName),
                color = MaterialTheme.colorScheme.primary,
                onClick = onPlaceClick,
            )
        }
    }
}

/** true si [ReminderTriggerInfo] va a dibujar algo con estos datos. Evita reservar lugar vacío. */
internal fun hasVisibleTrigger(reminder: Reminder, placeName: String?): Boolean =
    reminder.alarms.isNotEmpty() || (reminder.places.isNotEmpty() && placeName != null)

/**
 * Una pastilla con ícono y texto en [color], sobre un fondo del mismo color casi transparente:
 * así resalta sobre la card (y sobre una foto de portada) sin competir con el título.
 */
@Composable
private fun TriggerChip(
    @DrawableRes icon: Int,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIcon: Int? = null,
    trailingDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
        if (trailingIcon != null) {
            Icon(
                painter = painterResource(trailingIcon),
                contentDescription = trailingDescription,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun ReminderTriggerInfoPreview() {
    val now = Instant.now()
    fun note(alarms: List<Instant> = emptyList(), places: List<PlaceAlert> = emptyList()) =
        Reminder(title = "Nota", alarms = alarms.map { Alarm(it) }, places = places, createdAt = now)
    RememberAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReminderTriggerInfo(note(listOf(now.plus(Duration.ofMinutes(30)))), null, now)
            ReminderTriggerInfo(note(listOf(now.plus(Duration.ofDays(3)), now.plus(Duration.ofDays(4)))), null, now)
            ReminderTriggerInfo(note(places = listOf(PlaceAlert(placeId = 1, event = PlaceAlertEvent.Arrive))), "Casa", now)
            ReminderTriggerInfo(note(places = listOf(PlaceAlert(placeId = 1, event = PlaceAlertEvent.Leave))), "Kiosco", now)
        }
    }
}

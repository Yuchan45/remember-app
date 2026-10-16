package com.example.uade.rememberapp.ui.reminders.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.ui.theme.rememberAppLocale
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.isDueSoon
import com.example.uade.rememberapp.domain.model.nextAlarm
import com.example.uade.rememberapp.ui.theme.DueSoonAmber
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.time.Duration
import java.time.Instant

/**
 * Cuándo vuelve el recordatorio, para el pie de la card:
 * - por hora, hoy:          ⏰ 18:00   (+ "!" si falta poco o ya pasó)
 * - por hora, otro día:     📅 Sáb 11:00
 * - con varias horas:       ⏰ 18:00 +2   (la próxima, y cuántas más hay)
 * - por lugar:              ➤ Al llegar a Casa   (en color de acento)
 * - hora y lugar:           los dos, uno al lado del otro
 * - sin aviso:              no dibuja nada
 *
 * [placeName] es el nombre del primer lugar: PlaceAlert solo guarda el id.
 */
@Composable
fun ReminderTriggerInfo(
    reminder: Reminder,
    placeName: String?,
    now: Instant,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val next = reminder.nextAlarm(now)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (next != null) {
            val locale = rememberAppLocale()
            val icon = if (isSameDay(next.at, now)) R.drawable.ic_alarm else R.drawable.ic_calendar_month
            val extra = reminder.alarms.size - 1
            val time = formatReminderTime(next.at, now, locale)
            IconAndText(
                icon = icon,
                text = if (extra > 0) stringResource(R.string.reminders_trigger_more_alarms, time, extra) else time,
                color = contentColor,
            )
            if (next.isDueSoon(now)) {
                Icon(
                    painter = painterResource(R.drawable.ic_priority_high),
                    contentDescription = stringResource(R.string.reminders_trigger_due_soon),
                    tint = DueSoonAmber,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        // Sin nombre (lugar borrado o todavía sin cargar) no hay nada útil para mostrar.
        if (reminder.places.isNotEmpty() && placeName != null) {
            IconAndText(
                icon = R.drawable.ic_near_me,
                text = stringResource(R.string.reminders_trigger_on_arrival, placeName),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** true si [ReminderTriggerInfo] va a dibujar algo con estos datos. Evita reservar lugar vacío. */
internal fun hasVisibleTrigger(reminder: Reminder, placeName: String?): Boolean =
    reminder.alarms.isNotEmpty() || (reminder.places.isNotEmpty() && placeName != null)

@Composable
private fun IconAndText(
    @DrawableRes icon: Int,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
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
            fontWeight = FontWeight.Medium,
            color = color,
        )
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
            ReminderTriggerInfo(note(places = listOf(PlaceAlert(placeId = 1))), "Casa", now)
        }
    }
}

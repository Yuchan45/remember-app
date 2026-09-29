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
import com.example.uade.rememberapp.domain.model.Trigger
import com.example.uade.rememberapp.domain.model.isDueSoon
import com.example.uade.rememberapp.ui.theme.DueSoonAmber
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.time.Duration
import java.time.Instant

/**
 * Cuándo vuelve el recordatorio, para el pie de la card:
 * - por hora, hoy:          ⏰ 18:00   (+ "!" si falta poco o ya pasó)
 * - por hora, otro día:     📅 Sáb 11:00
 * - por lugar:              ➤ Al llegar a Casa   (en color de acento)
 * - sin aviso:              no dibuja nada
 *
 * [placeName] hace falta porque [Trigger.AtPlace] solo guarda el id del lugar.
 */
@Composable
fun ReminderTriggerInfo(
    trigger: Trigger,
    placeName: String?,
    now: Instant,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    when (trigger) {
        Trigger.None -> Unit

        is Trigger.AtTime -> {
            val locale = rememberAppLocale()
            val icon = if (isSameDay(trigger.at, now)) R.drawable.ic_alarm else R.drawable.ic_calendar_month
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconAndText(
                    icon = icon,
                    text = formatReminderTime(trigger.at, now, locale),
                    color = contentColor,
                )
                if (trigger.isDueSoon(now)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_priority_high),
                        contentDescription = stringResource(R.string.reminders_trigger_due_soon),
                        tint = DueSoonAmber,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        is Trigger.AtPlace -> {
            // Sin nombre (lugar borrado o todavía sin cargar) no hay nada útil para mostrar.
            if (placeName != null) {
                IconAndText(
                    icon = R.drawable.ic_near_me,
                    text = stringResource(R.string.reminders_trigger_on_arrival, placeName),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = modifier,
                )
            }
        }
    }
}

/** true si [ReminderTriggerInfo] va a dibujar algo con estos datos. Evita reservar lugar vacío. */
internal fun hasVisibleTrigger(trigger: Trigger, placeName: String?): Boolean = when (trigger) {
    Trigger.None -> false
    is Trigger.AtTime -> true
    is Trigger.AtPlace -> placeName != null
}

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
    RememberAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReminderTriggerInfo(Trigger.AtTime(now.plus(Duration.ofMinutes(30))), null, now)
            ReminderTriggerInfo(Trigger.AtTime(now.plus(Duration.ofDays(3))), null, now)
            ReminderTriggerInfo(Trigger.AtPlace(1), "Casa", now)
        }
    }
}

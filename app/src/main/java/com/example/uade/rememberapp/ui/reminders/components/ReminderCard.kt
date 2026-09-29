package com.example.uade.rememberapp.ui.reminders.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.Trigger
import com.example.uade.rememberapp.ui.components.LabelChip
import com.example.uade.rememberapp.ui.reminders.sample.SampleReminders
import com.example.uade.rememberapp.ui.theme.PhotoBackground
import com.example.uade.rememberapp.ui.theme.PhotoScrim
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.time.Instant

/**
 * Card de un recordatorio, para notas y listas.
 *
 * Es un solo componente porque los dos tipos comparten casi todo; lo único que cambia es el
 * cuerpo:
 * ```
 * ┃ [ícono] Título
 * ┃ Nota  → descripción            Lista → ☐ ítem / ☐ ítem / + N más
 * ┃ [● Etiqueta]              ⏰ 18:00 !
 * ```
 * - La barra de la izquierda (┃) toma el color de la primera etiqueta; sin etiquetas no se dibuja.
 * - Con foto, la foto va de fondo con un velo oscuro para que el texto se siga leyendo.
 *
 * No guarda estado: [now] entra por parámetro para que "hoy" y el "!" sean previsualizables.
 */
@Composable
fun ReminderCard(
    reminder: Reminder,
    placeName: String?,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = reminder.labels.firstOrNull()?.let { Color(it.colorArgb) }
    val hasPhoto = reminder.photoPath != null

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (hasPhoto) {
                ReminderPhotoBackground(modifier = Modifier.matchParentSize())
            }
            Row {
                if (accentColor != null) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(accentColor),
                    )
                }
                ReminderCardContent(
                    reminder = reminder,
                    placeName = placeName,
                    now = now,
                    modifier = Modifier
                        .weight(1f)
                        // Una card de solo foto no tiene texto: se le da alto para que la foto se vea.
                        .heightIn(min = if (hasPhoto) 112.dp else 0.dp)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ReminderCardContent(
    reminder: Reminder,
    placeName: String?,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(reminder.typeIcon()),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            reminder.title?.takeIf { it.isNotBlank() }?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        reminder.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (reminder.type == ReminderType.Checklist && reminder.items.isNotEmpty()) {
            ChecklistPreview(
                items = reminder.items,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (reminder.labels.isNotEmpty() || hasVisibleTrigger(reminder.trigger, placeName)) {
            Spacer(Modifier.weight(1f))
            ReminderCardFooter(reminder = reminder, placeName = placeName, now = now)
        }
    }
}

@Composable
private fun ReminderCardFooter(
    reminder: Reminder,
    placeName: String?,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            reminder.labels.forEach { label ->
                LabelChip(name = label.name, color = Color(label.colorArgb))
            }
        }
        ReminderTriggerInfo(
            trigger = reminder.trigger,
            placeName = placeName,
            now = now,
        )
    }
}

/**
 * Placeholder de la foto de fondo: un degradé marrón y, encima, un velo que oscurece el lado
 * del texto (la izquierda).
 *
 * TODO: cargar la foto real desde photoPath (con Coil) debajo del velo.
 */
@Composable
private fun ReminderPhotoBackground(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.reminders_photo_description)
    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .background(
                Brush.linearGradient(listOf(PhotoBackground.copy(alpha = 0.6f), PhotoBackground)),
            )
            .background(Brush.horizontalGradient(listOf(PhotoScrim, Color.Transparent))),
    )
}

/** Ícono junto al título: lista, nota por lugar (pin) o nota común. */
@DrawableRes
private fun Reminder.typeIcon(): Int = when {
    type == ReminderType.Checklist -> R.drawable.ic_checklist
    trigger is Trigger.AtPlace -> R.drawable.ic_location_on
    else -> R.drawable.ic_description
}

@Preview(showBackground = true, backgroundColor = 0xFF0B141B, heightDp = 900)
@Composable
private fun ReminderCardPreview() {
    val now = Instant.now()
    RememberAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SampleReminders.previewReminders(now).forEach { reminder ->
                ReminderCard(
                    reminder = reminder,
                    placeName = SampleReminders.placeNames[(reminder.trigger as? Trigger.AtPlace)?.placeId],
                    now = now,
                    onClick = {},
                )
            }
        }
    }
}

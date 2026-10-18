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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
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
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.ui.components.TagChip
import com.example.uade.rememberapp.ui.reminders.sample.SampleReminders
import com.example.uade.rememberapp.ui.theme.PhotoBackground
import com.example.uade.rememberapp.ui.theme.PhotoScrim
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import coil3.compose.AsyncImage
import java.io.File
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
 * - La barra de la izquierda (┃) toma el color de la etiqueta; con varias, un degradé de arriba
 *   abajo entre sus colores (ver [accentBrush]). Sin etiquetas no se dibuja.
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
    onToggleChecklistItem: ((itemId: Long, index: Int) -> Unit)? = null,
) {
    val accentBrush = remember(reminder.tags) { accentBrush(reminder.tags) }
    val hasPhoto = reminder.photoPath != null

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (hasPhoto) {
                ReminderPhotoBackground(photoPath = reminder.photoPath, modifier = Modifier.matchParentSize())
            }
            Row {
                if (accentBrush != null) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(accentBrush),
                    )
                }
                ReminderCardContent(
                    reminder = reminder,
                    placeName = placeName,
                    now = now,
                    onToggleChecklistItem = onToggleChecklistItem,
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
    onToggleChecklistItem: ((itemId: Long, index: Int) -> Unit)? = null,
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
                onToggleItem = onToggleChecklistItem,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (reminder.tags.isNotEmpty() || hasVisibleTrigger(reminder, placeName)) {
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
            reminder.tags.forEach { tag ->
                TagChip(name = tag.name, color = Color(tag.colorArgb))
            }
        }
        ReminderTriggerInfo(
            reminder = reminder,
            placeName = placeName,
            now = now,
        )
    }
}

/**
 * La portada del recordatorio ([photoPath], ya recortada), cubriendo todo el espacio y centrada.
 *
 * Debajo va un degradé marrón: se ve mientras carga y si el archivo no se puede leer (ej. los
 * datos de ejemplo de las previews). Con [scrim], encima va un velo que oscurece la izquierda
 * para que el texto de la card se siga leyendo.
 */
@Composable
internal fun ReminderPhotoBackground(
    photoPath: String?,
    modifier: Modifier = Modifier,
    scrim: Boolean = true,
) {
    val description = stringResource(R.string.reminders_photo_description)
    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .background(
                Brush.linearGradient(listOf(PhotoBackground.copy(alpha = 0.6f), PhotoBackground)),
            ),
    ) {
        if (photoPath != null) {
            AsyncImage(
                // File y no el String: así Coil no intenta interpretarlo como URL.
                model = File(photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        if (scrim) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.horizontalGradient(listOf(PhotoScrim, Color.Transparent))),
            )
        }
    }
}

/**
 * El relleno de la barra de la izquierda: nada sin etiquetas, el color de la etiqueta si hay una,
 * o un degradé vertical con los colores de todas, en el orden en que se muestran los chips (así
 * arriba queda el de la primera, igual que antes).
 */
private fun accentBrush(tags: List<Tag>): Brush? {
    val colors = tags.map { Color(it.colorArgb) }
    return when (colors.size) {
        0 -> null
        1 -> SolidColor(colors.single())
        else -> Brush.verticalGradient(colors)
    }
}

/** Ícono junto al título: lista, nota solo por lugar (pin) o nota común. */
@DrawableRes
internal fun Reminder.typeIcon(): Int = when {
    type == ReminderType.Checklist -> R.drawable.ic_checklist
    alarms.isEmpty() && places.isNotEmpty() -> R.drawable.ic_location_on
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
                    placeName = SampleReminders.placeNames[reminder.places.firstOrNull()?.placeId],
                    now = now,
                    onClick = {},
                )
            }
        }
    }
}

@Preview(name = "Varias etiquetas (barra en degradé)", showBackground = true, backgroundColor = 0xFF0B141B)
@Composable
private fun ReminderCardManyTagsPreview() {
    RememberAppTheme {
        ReminderCard(
            reminder = Reminder(
                title = "Comprar para el asado",
                description = "Carbón, chorizos y pan",
                tags = listOf(
                    Tag(id = 1, name = "Salud", colorArgb = 0xFF6FCF97),
                    Tag(id = 2, name = "Casa", colorArgb = 0xFF56CCF2),
                    Tag(id = 3, name = "Asado", colorArgb = 0xFF9B7BEA),
                ),
                createdAt = Instant.now(),
            ),
            placeName = null,
            now = Instant.now(),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

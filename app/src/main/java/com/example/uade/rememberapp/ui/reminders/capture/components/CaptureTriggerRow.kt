package com.example.uade.rememberapp.ui.reminders.capture.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.ui.components.OptionChip
import com.example.uade.rememberapp.ui.reminders.capture.CapturePanel
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.model.ReminderImportance
import com.example.uade.rememberapp.ui.reminders.sample.SampleQuickCapture
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

// Chips más compactos que los de los paneles, para que la fila aproveche el ancho.
private val CompactChipPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
private val CompactIconSpacing = 6.dp
private val ChipGap = 6.dp

/**
 * Fila de chips para elegir el aviso: [🕒 Fecha y hora] [📍 Ubicación] [🏷 Etiqueta] [🔔 Importancia].
 *
 * Como la barra inferior de la app, un chip seleccionado muestra ícono y texto, y los demás
 * solo el ícono: así entran los cuatro. Si aun así no entran, la fila se desliza de costado.
 *
 * Un chip se ve seleccionado si ya tiene algo elegido o si su panel está abierto. Hora y lugar
 * se pueden combinar. Algunos textos cambian con lo elegido: el de lugar pasa a "Al llegar a
 * Facultad", el de etiquetas a "2 etiquetas" y el de importancia al nivel (ej. "Alta"; con la
 * predeterminada sigue diciendo "Importancia" y no se marca).
 */
@Composable
fun CaptureTriggerRow(
    expandedPanel: CapturePanel?,
    hasTime: Boolean,
    selectedPlace: Place?,
    placeEvent: PlaceEvent,
    onPanelClick: (CapturePanel) -> Unit,
    modifier: Modifier = Modifier,
    tagCount: Int = 0,
    importance: ReminderImportance = ReminderImportance.Default,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val placeText = when {
        selectedPlace == null -> stringResource(R.string.reminders_capture_trigger_place)
        placeEvent == PlaceEvent.Arrive ->
            stringResource(R.string.reminders_capture_trigger_arrive_at, selectedPlace.name)
        else -> stringResource(R.string.reminders_capture_trigger_leave_from, selectedPlace.name)
    }
    val tagText = if (tagCount > 0) {
        pluralStringResource(R.plurals.reminders_capture_trigger_tags, tagCount, tagCount)
    } else {
        stringResource(R.string.reminders_capture_trigger_tag)
    }
    val isDefaultImportance = importance == ReminderImportance.Default
    val importanceText = if (isDefaultImportance) {
        stringResource(R.string.reminders_capture_trigger_importance)
    } else {
        stringResource(importance.label)
    }

    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        item {
            TriggerChip(
                text = stringResource(R.string.reminders_capture_trigger_time),
                icon = R.drawable.ic_schedule,
                selected = hasTime || expandedPanel == CapturePanel.Time,
                onClick = { onPanelClick(CapturePanel.Time) },
            )
        }
        item {
            TriggerChip(
                text = placeText,
                icon = R.drawable.ic_location_on,
                selected = selectedPlace != null || expandedPanel == CapturePanel.Place,
                onClick = { onPanelClick(CapturePanel.Place) },
            )
        }
        item {
            TriggerChip(
                text = tagText,
                icon = R.drawable.ic_tag,
                selected = tagCount > 0 || expandedPanel == CapturePanel.Tags,
                onClick = { onPanelClick(CapturePanel.Tags) },
            )
        }
        item {
            TriggerChip(
                text = importanceText,
                icon = importance.icon,
                selected = !isDefaultImportance || expandedPanel == CapturePanel.Importance,
                onClick = { onPanelClick(CapturePanel.Importance) },
            )
        }
    }
}

/** Chip compacto de la fila: con texto solo si está seleccionado. */
@Composable
private fun TriggerChip(
    text: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OptionChip(
        text = text,
        icon = icon,
        selected = selected,
        onClick = onClick,
        contentPadding = CompactChipPadding,
        iconSpacing = CompactIconSpacing,
        showText = selected,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun CaptureTriggerRowPreview() {
    RememberAppTheme {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Nada elegido: solo íconos.
            CaptureTriggerRow(
                expandedPanel = null,
                hasTime = false,
                selectedPlace = null,
                placeEvent = PlaceEvent.Arrive,
                onPanelClick = {},
                contentPadding = PaddingValues(horizontal = 16.dp),
            )
            // Con lugar e importancia alta: esos dos se expanden.
            CaptureTriggerRow(
                expandedPanel = null,
                hasTime = false,
                selectedPlace = SampleQuickCapture.favoritePlaces[2],
                placeEvent = PlaceEvent.Arrive,
                onPanelClick = {},
                importance = ReminderImportance.High,
                contentPadding = PaddingValues(horizontal = 16.dp),
            )
        }
    }
}

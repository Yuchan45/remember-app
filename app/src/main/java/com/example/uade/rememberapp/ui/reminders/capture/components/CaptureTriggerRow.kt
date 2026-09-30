package com.example.uade.rememberapp.ui.reminders.capture.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import com.example.uade.rememberapp.ui.reminders.capture.PlaceEvent
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

// Chips más compactos que los de los paneles: así los tres entran en el ancho de un teléfono.
private val CompactChipPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
private val CompactIconSpacing = 6.dp
private val ChipGap = 6.dp

/**
 * Fila de chips para elegir el aviso: [🕒 Fecha y hora] [📍 Ubicación] [🏷 Etiqueta].
 *
 * Hora y lugar se pueden combinar. Un chip se ve seleccionado si ya tiene algo elegido o si
 * su panel está abierto. El de hora dice siempre "Fecha y hora"; el de lugar pasa a
 * "Al llegar a Facultad" cuando hay un lugar elegido.
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
    contentPadding: PaddingValues = PaddingValues(),
) {
    val placeText = when {
        selectedPlace == null -> stringResource(R.string.reminders_capture_trigger_place)
        placeEvent == PlaceEvent.Arrive ->
            stringResource(R.string.reminders_capture_trigger_arrive_at, selectedPlace.name)
        else -> stringResource(R.string.reminders_capture_trigger_leave_from, selectedPlace.name)
    }

    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        item {
            OptionChip(
                text = stringResource(R.string.reminders_capture_trigger_time),
                icon = R.drawable.ic_schedule,
                selected = hasTime || expandedPanel == CapturePanel.Time,
                onClick = { onPanelClick(CapturePanel.Time) },
                contentPadding = CompactChipPadding,
                iconSpacing = CompactIconSpacing,
            )
        }
        item {
            OptionChip(
                text = placeText,
                icon = R.drawable.ic_location_on,
                selected = selectedPlace != null || expandedPanel == CapturePanel.Place,
                onClick = { onPanelClick(CapturePanel.Place) },
                contentPadding = CompactChipPadding,
                iconSpacing = CompactIconSpacing,
            )
        }
        item {
            OptionChip(
                text = if (tagCount > 0) {
                    pluralStringResource(R.plurals.reminders_capture_trigger_tags, tagCount, tagCount)
                } else {
                    stringResource(R.string.reminders_capture_trigger_tag)
                },
                icon = R.drawable.ic_tag,
                selected = tagCount > 0 || expandedPanel == CapturePanel.Tags,
                onClick = { onPanelClick(CapturePanel.Tags) },
                contentPadding = CompactChipPadding,
                iconSpacing = CompactIconSpacing,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun CaptureTriggerRowPreview() {
    RememberAppTheme {
        CaptureTriggerRow(
            expandedPanel = null,
            hasTime = false,
            selectedPlace = null,
            placeEvent = PlaceEvent.Arrive,
            onPanelClick = {},
            contentPadding = PaddingValues(16.dp),
        )
    }
}

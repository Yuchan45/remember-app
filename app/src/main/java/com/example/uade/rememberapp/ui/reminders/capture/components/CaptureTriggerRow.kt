package com.example.uade.rememberapp.ui.reminders.capture.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.ui.components.OptionChip
import com.example.uade.rememberapp.ui.reminders.capture.CapturePanel
import com.example.uade.rememberapp.ui.reminders.capture.PlaceEvent
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

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
    onLabelClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OptionChip(
                text = stringResource(R.string.reminders_capture_trigger_time),
                icon = R.drawable.ic_schedule,
                selected = hasTime || expandedPanel == CapturePanel.Time,
                onClick = { onPanelClick(CapturePanel.Time) },
            )
        }
        item {
            OptionChip(
                text = placeText,
                icon = R.drawable.ic_location_on,
                selected = selectedPlace != null || expandedPanel == CapturePanel.Place,
                onClick = { onPanelClick(CapturePanel.Place) },
            )
        }
        item {
            OptionChip(
                text = stringResource(R.string.reminders_capture_trigger_label),
                icon = R.drawable.ic_label,
                selected = false,
                onClick = onLabelClick,
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
            onLabelClick = {},
            contentPadding = PaddingValues(16.dp),
        )
    }
}

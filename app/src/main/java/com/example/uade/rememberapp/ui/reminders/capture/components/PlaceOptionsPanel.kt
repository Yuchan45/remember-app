package com.example.uade.rememberapp.ui.reminders.capture.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.ui.components.OptionChip
import com.example.uade.rememberapp.ui.components.OptionChipStyle
import com.example.uade.rememberapp.ui.components.SectionLabel
import com.example.uade.rememberapp.ui.components.SegmentedSelector
import com.example.uade.rememberapp.ui.places.components.iconRes
import com.example.uade.rememberapp.ui.reminders.capture.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.sample.SampleQuickCapture
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Panel que se despliega al tocar "Ubicación":
 * ```
 * LUGARES FAVORITOS  [⌂ Casa] [💼 Trabajo] [🎓 Facultad] [P Estacionamiento] [🔍 Buscar dirección…]
 * AVISAR             [→ Al llegar | ↦ Al salir]
 * ```
 * El lugar elegido se marca solo con borde ([OptionChipStyle.Outlined]), igual que las opciones
 * de "Fecha y hora": son opciones secundarias frente a los chips de aviso de abajo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceOptionsPanel(
    places: List<Place>,
    selectedPlaceId: Long?,
    events: List<PlaceEvent>,
    selectedEvent: PlaceEvent,
    onPlaceClick: (Long) -> Unit,
    onSearchAddress: () -> Unit,
    onEventClick: (PlaceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel(text = stringResource(R.string.reminders_capture_section_favorite_places))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            places.forEach { place ->
                OptionChip(
                    text = place.name,
                    icon = place.kind.iconRes(),
                    selected = place.id == selectedPlaceId,
                    onClick = { onPlaceClick(place.id) },
                    style = OptionChipStyle.Outlined,
                )
            }
            OptionChip(
                text = stringResource(R.string.reminders_capture_place_search),
                icon = R.drawable.ic_search,
                selected = false,
                onClick = onSearchAddress,
                style = OptionChipStyle.Outlined,
            )
        }

        SectionLabel(
            text = stringResource(R.string.reminders_capture_section_notify),
            modifier = Modifier.padding(top = 4.dp),
        )
        SegmentedSelector(
            options = events,
            selected = selectedEvent,
            onSelected = onEventClick,
            label = { stringResource(it.label) },
            icon = { it.icon },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun PlaceOptionsPanelPreview() {
    RememberAppTheme {
        PlaceOptionsPanel(
            places = SampleQuickCapture.favoritePlaces,
            selectedPlaceId = 3,
            events = PlaceEvent.entries,
            selectedEvent = PlaceEvent.Arrive,
            onPlaceClick = {},
            onSearchAddress = {},
            onEventClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

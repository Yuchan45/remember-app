package com.example.uade.rememberapp.ui.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton
import com.example.uade.rememberapp.ui.reminders.detail.PlaceEditor
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent

/**
 * Modal "Ubicación": selector de lugares guardados con opciones Al llegar / Al salir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSheet(
    places: List<Place>,
    editor: PlaceEditor,
    onPlaceClick: (Long) -> Unit,
    onSearchAddress: () -> Unit = {},
    onEventClick: (PlaceEvent) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PlaceOptionsPanel(
                places = places,
                selectedPlaceId = editor.selectedPlaceId,
                events = PlaceEvent.entries,
                selectedEvent = editor.event,
                onPlaceClick = onPlaceClick,
                onSearchAddress = onSearchAddress,
                onEventClick = onEventClick,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = onClose)
                DialogFilledConfirmButton(text = stringResource(R.string.reminder_alarms_save), onClick = onSave)
            }
        }
    }
}

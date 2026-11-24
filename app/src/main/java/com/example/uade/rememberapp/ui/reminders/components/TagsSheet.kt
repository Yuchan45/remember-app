package com.example.uade.rememberapp.ui.reminders.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton
import com.example.uade.rememberapp.ui.tags.TagPickerPanel

/**
 * Modal "Etiquetas": asignación y gestión de etiquetas con botones Cancelar / Guardar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsSheet(
    assignedIds: Set<Long>,
    onAssignedChange: (Set<Long>) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    var isEditorOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TagPickerPanel(
                initialAssigned = assignedIds,
                onAssignedChange = onAssignedChange,
                onCreatorExpandedChange = { isEditorOpen = it },
            )
            AnimatedVisibility(visible = !isEditorOpen, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = onCancel)
                    DialogFilledConfirmButton(text = stringResource(R.string.reminder_alarms_save), onClick = onSave)
                }
            }
        }
    }
}

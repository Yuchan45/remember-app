package com.example.uade.rememberapp.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.uade.rememberapp.R
import java.time.LocalTime

/**
 * Diálogo para elegir una hora con el reloj de Material ("Selecciona una hora"), en formato de
 * 24 horas como el resto de la app. El botón de teclado de abajo a la izquierda cambia entre el
 * reloj y escribir la hora a mano. Usa el estilo común de diálogos ([AppDialogDefaults]).
 *
 * Avisa la hora elegida por [onConfirm] ("Aceptar"); "Cancelar", atrás o tocar afuera llaman a
 * [onDismiss]. [initialTime] es la hora con la que abre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockTimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }

    val colors = AppDialogDefaults.timePickerColors()

    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.time_picker_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            DialogConfirmButton(
                text = stringResource(R.string.common_accept),
                onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) },
            )
        },
        dismissButton = {
            DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = onDismiss)
        },
        modeToggleButton = {
            TimePickerDialogDefaults.DisplayModeToggle(
                onDisplayModeChange = {
                    displayMode = if (displayMode == TimePickerDisplayMode.Picker) {
                        TimePickerDisplayMode.Input
                    } else {
                        TimePickerDisplayMode.Picker
                    }
                },
                displayMode = displayMode,
            )
        },
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
    ) {
        if (displayMode == TimePickerDisplayMode.Picker) {
            TimePicker(state = state, colors = colors)
        } else {
            TimeInput(state = state, colors = colors)
        }
    }
}

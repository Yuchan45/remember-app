package com.example.uade.rememberapp.ui.reminders.capture.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.AppDialogDefaults
import com.example.uade.rememberapp.ui.components.ClockTimePickerDialog
import com.example.uade.rememberapp.ui.components.DialogConfirmButton
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.MonthCalendar
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Hora con la que abre el reloj si todavía no se eligió ninguna. */
private val DefaultTime = LocalTime.NOON

/**
 * Diálogo de "Elegir fecha…":
 * ```
 *  <   Septiembre de 2026   >
 *  (calendario del mes)
 *  ─────────────────────────
 *  🕒 Establecer hora        ← abre el reloj; si ya hay hora, la muestra ("19:00")
 *  ─────────────────────────
 *               Cancelar  Listo
 * ```
 * Lo que se va eligiendo (día, mes que se ve, hora) queda en el diálogo hasta tocar "Listo",
 * que lo avisa por [onConfirm]. "Cancelar", atrás o tocar afuera lo descartan ([onDismiss]).
 * No se pueden elegir días anteriores a hoy.
 *
 * Si todavía no se eligió nada, abre en el día de **dentro de una hora** (ej. a las 23:30
 * propone mañana) y sin hora ("Establecer hora"). Al abrir el reloj, este propone las 12:00.
 */
@Composable
fun ReminderDateDialog(
    initialDate: LocalDate?,
    initialTime: LocalTime?,
    onConfirm: (date: LocalDate, time: LocalTime?) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val inOneHour = remember { LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES) }
    var date by remember { mutableStateOf(initialDate ?: inOneHour.toLocalDate()) }
    // Sin hora hasta que el usuario la elija: la fila muestra "Establecer hora".
    var time by remember { mutableStateOf(initialTime) }
    var month by remember { mutableStateOf(YearMonth.from(date)) }
    var isTimePickerOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        ReminderDateDialogContent(
            month = month,
            date = date,
            time = time,
            minDate = today,
            onMonthChange = { month = it },
            onDayClick = { date = it },
            onSetTimeClick = { isTimePickerOpen = true },
            onCancel = onDismiss,
            onDone = { onConfirm(date, time) },
        )
    }

    if (isTimePickerOpen) {
        ClockTimePickerDialog(
            // El reloj propone las 12:00 si todavía no se eligió ninguna hora.
            initialTime = time ?: DefaultTime,
            onConfirm = {
                time = it
                isTimePickerOpen = false
            },
            onDismiss = { isTimePickerOpen = false },
        )
    }
}

@Composable
private fun ReminderDateDialogContent(
    month: YearMonth,
    date: LocalDate,
    time: LocalTime?,
    minDate: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onSetTimeClick: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 360.dp),
        shape = AppDialogDefaults.shape,
        color = AppDialogDefaults.containerColor,
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            MonthCalendar(
                month = month,
                selected = date,
                onMonthChange = onMonthChange,
                onDayClick = onDayClick,
                minDate = minDate,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.outline,
            )
            SetTimeRow(time = time, onClick = onSetTimeClick)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = onCancel)
                DialogConfirmButton(text = stringResource(R.string.common_done), onClick = onDone)
            }
        }
    }
}

/** Fila "🕒 Establecer hora", o "🕒 19:00" si ya se eligió una. */
@Composable
private fun SetTimeRow(
    time: LocalTime?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_schedule),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = time?.format(DateTimeFormatter.ofPattern("HH:mm"))
                ?: stringResource(R.string.reminders_capture_date_set_time),
            style = MaterialTheme.typography.bodyLarge,
            color = if (time != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
private fun ReminderDateDialogContentPreview() {
    RememberAppTheme {
        ReminderDateDialogContent(
            month = YearMonth.of(2026, 9),
            date = LocalDate.of(2026, 9, 29),
            time = null,
            minDate = LocalDate.of(2026, 9, 1),
            onMonthChange = {},
            onDayClick = {},
            onSetTimeClick = {},
            onCancel = {},
            onDone = {},
        )
    }
}

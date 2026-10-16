package com.example.uade.rememberapp.ui.reminders.components

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
import com.example.uade.rememberapp.ui.theme.rememberAppLocale
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.OptionChip
import com.example.uade.rememberapp.ui.components.OptionChipStyle
import com.example.uade.rememberapp.ui.components.SectionLabel
import com.example.uade.rememberapp.ui.reminders.model.DefaultPickedTime
import com.example.uade.rememberapp.ui.reminders.model.RepeatOption
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Panel que se despliega al tocar "Fecha y hora":
 * ```
 * CUÁNDO   [⏱ En 1 hora] [☾ Hoy 21:00] [☀ Mañana 9:00] [Sáb 10:00] [📅 Elegir fecha…]
 * REPETIR  [No] [Diario] [Lun a Vie] [Semanal]
 * ```
 * Las opciones elegidas se marcan solo con borde ([OptionChipStyle.Outlined]): son opciones
 * secundarias, a diferencia de los chips de aviso de abajo, que se marcan con fondo.
 * Las opciones entran por parámetro (vienen del UiState) y los chips pasan a la línea
 * siguiente si no entran. Si ya se eligió un día en el calendario ([pickedDate]), el chip de
 * "Elegir fecha…" lo muestra en su lugar, siempre con hora, ej. "Mar 29 sept · 19:00".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeOptionsPanel(
    shortcuts: List<TimeShortcut>,
    selectedShortcut: TimeShortcut?,
    repeatOptions: List<RepeatOption>,
    selectedRepeat: RepeatOption,
    onShortcutClick: (TimeShortcut) -> Unit,
    onRepeatClick: (RepeatOption) -> Unit,
    modifier: Modifier = Modifier,
    pickedDate: LocalDate? = null,
    pickedTime: LocalTime? = null,
) {
    val locale = rememberAppLocale()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel(text = stringResource(R.string.reminders_capture_section_when))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shortcuts.forEach { shortcut ->
                OptionChip(
                    text = if (shortcut == TimeShortcut.PickDate && pickedDate != null) {
                        formatPickedDate(pickedDate, pickedTime, locale)
                    } else {
                        stringResource(shortcut.label)
                    },
                    icon = shortcut.icon,
                    selected = shortcut == selectedShortcut,
                    onClick = { onShortcutClick(shortcut) },
                    style = OptionChipStyle.Outlined,
                )
            }
        }

        SectionLabel(
            text = stringResource(R.string.reminders_capture_section_repeat),
            modifier = Modifier.padding(top = 4.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeatOptions.forEach { option ->
                OptionChip(
                    text = stringResource(option.label),
                    selected = option == selectedRepeat,
                    onClick = { onRepeatClick(option) },
                    style = OptionChipStyle.Outlined,
                )
            }
        }
    }
}

/**
 * Día y hora elegidos para el chip, ej. "Mar 29 sept · 19:00". Si no se eligió hora muestra la
 * que se va a usar al guardar ([DefaultPickedTime]), así el usuario sabe a qué hora va a sonar.
 */
private fun formatPickedDate(date: LocalDate, time: LocalTime?, locale: Locale): String {
    val day = date.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
        .replace(".", "")
        .replaceFirstChar { it.titlecase(locale) }
    val hour = (time ?: DefaultPickedTime).format(DateTimeFormatter.ofPattern("HH:mm"))
    return "$day · $hour"
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun TimeOptionsPanelPreview() {
    RememberAppTheme {
        TimeOptionsPanel(
            shortcuts = TimeShortcut.entries,
            selectedShortcut = TimeShortcut.TomorrowMorning,
            repeatOptions = RepeatOption.entries,
            selectedRepeat = RepeatOption.None,
            onShortcutClick = {},
            onRepeatClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

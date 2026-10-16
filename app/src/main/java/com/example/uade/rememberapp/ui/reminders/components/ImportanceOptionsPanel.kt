package com.example.uade.rememberapp.ui.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.OptionChip
import com.example.uade.rememberapp.ui.components.OptionChipStyle
import com.example.uade.rememberapp.ui.components.SectionLabel
import com.example.uade.rememberapp.ui.reminders.model.ReminderImportance
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Panel que se despliega al tocar "Importancia":
 * ```
 * CÓMO AVISAR
 * ┌ 🔕 Baja ──────────────────────────────┐
 * │    Silenciosa: sin sonido, vibración… │
 * └───────────────────────────────────────┘
 * ┌ 🔔 Predeterminada ────────────────────┐   ← la elegida, con borde de acento
 * …
 * ```
 * Hay siempre una sola elegida (como un radio button). Usa el mismo [OptionChip] que los otros
 * paneles, a lo ancho y con descripción, porque los niveles necesitan explicarse.
 *
 * TODO: por ahora es solo maquetado; elegir un nivel no cambia el aviso.
 */
@Composable
fun ImportanceOptionsPanel(
    options: List<ReminderImportance>,
    selected: ReminderImportance,
    onSelect: (ReminderImportance) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel(text = stringResource(R.string.reminders_capture_section_importance))
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { importance ->
                OptionChip(
                    text = stringResource(importance.label),
                    description = stringResource(importance.description),
                    icon = importance.icon,
                    selected = importance == selected,
                    onClick = { onSelect(importance) },
                    style = OptionChipStyle.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun ImportanceOptionsPanelPreview() {
    RememberAppTheme {
        ImportanceOptionsPanel(
            options = ReminderImportance.entries,
            selected = ReminderImportance.High,
            onSelect = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

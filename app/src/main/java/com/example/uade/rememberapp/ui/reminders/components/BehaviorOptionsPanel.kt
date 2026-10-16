package com.example.uade.rememberapp.ui.reminders.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.AppDialogDefaults
import com.example.uade.rememberapp.ui.components.DialogConfirmButton
import com.example.uade.rememberapp.ui.components.SectionLabel
import com.example.uade.rememberapp.ui.reminders.model.ReminderImportance
import com.example.uade.rememberapp.ui.reminders.model.ReminderVisibility
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Las opciones de "Comportamiento", en dos secciones:
 * ```
 * VISIBILIDAD                               (?)
 * ◉ Predeterminado
 *   Mostrar título y contenido.
 * ○ Privado …
 * ─────────────────────────────────────────────
 * IMPORTANCIA                               (?)
 * ○ Baja …
 * ```
 * Cada sección tiene siempre una opción elegida (radio). El "?" abre una explicación corta.
 * Es el mismo panel en la nota rápida y en el modal del detalle.
 */
@Composable
fun BehaviorOptionsPanel(
    visibility: ReminderVisibility,
    importance: ReminderImportance,
    onVisibilitySelect: (ReminderVisibility) -> Unit,
    onImportanceSelect: (ReminderImportance) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BehaviorSection(
            title = R.string.reminders_behavior_section_visibility,
            help = R.string.reminders_behavior_visibility_help,
            options = ReminderVisibility.entries,
            selected = visibility,
            label = { it.label },
            description = { it.description },
            onSelect = onVisibilitySelect,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        BehaviorSection(
            title = R.string.reminders_behavior_section_importance,
            help = R.string.reminders_behavior_importance_help,
            options = ReminderImportance.entries,
            selected = importance,
            label = { it.label },
            description = { it.description },
            onSelect = onImportanceSelect,
        )
    }
}

/** Una sección: título con su "?" y la lista de opciones con radio. */
@Composable
private fun <T> BehaviorSection(
    @StringRes title: Int,
    @StringRes help: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    description: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    // Solo visual: si la explicación está abierta. rememberSaveable la mantiene al girar la pantalla.
    var isHelpOpen by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(text = stringResource(title), modifier = Modifier.weight(1f))
            HelpButton(onClick = { isHelpOpen = true })
        }
        Column(modifier = Modifier.selectableGroup()) {
            options.forEach { option ->
                RadioOption(
                    label = stringResource(label(option)),
                    description = stringResource(description(option)),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                )
            }
        }
    }

    if (isHelpOpen) {
        AlertDialog(
            onDismissRequest = { isHelpOpen = false },
            confirmButton = {
                DialogConfirmButton(text = stringResource(R.string.common_accept), onClick = { isHelpOpen = false })
            },
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(help)) },
            shape = AppDialogDefaults.shape,
            containerColor = AppDialogDefaults.containerColor,
        )
    }
}

/** El "?" redondo a la derecha del título de la sección. */
@Composable
private fun HelpButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(32.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_question_mark),
            contentDescription = stringResource(R.string.reminders_behavior_help),
            modifier = Modifier.padding(8.dp),
        )
    }
}

/**
 * Una opción: radio, nombre y descripción. Toda la fila se puede tocar; el radio no tiene su
 * propio onClick para que TalkBack la lea como un solo elemento.
 */
@Composable
private fun RadioOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36, heightDp = 720)
@Composable
private fun BehaviorOptionsPanelPreview() {
    RememberAppTheme {
        BehaviorOptionsPanel(
            visibility = ReminderVisibility.Default,
            importance = ReminderImportance.High,
            onVisibilitySelect = {},
            onImportanceSelect = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

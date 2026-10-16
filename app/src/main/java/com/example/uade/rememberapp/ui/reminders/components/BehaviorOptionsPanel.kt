package com.example.uade.rememberapp.ui.reminders.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.OptionChip
import com.example.uade.rememberapp.ui.components.OptionChipStyle
import com.example.uade.rememberapp.ui.components.SectionLabel
import com.example.uade.rememberapp.ui.reminders.model.ReminderImportance
import com.example.uade.rememberapp.ui.reminders.model.ReminderVisibility
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import kotlinx.coroutines.launch

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
 * Cada sección tiene siempre una opción elegida. El "?" abre una explicación corta.
 *
 * Con [compact] (la nota rápida, donde el modal crece con su contenido) las opciones van como
 * chips en una fila y solo se explica la elegida:
 * ```
 * VISIBILIDAD                               (?)
 * [Predeterminado] [Privado] [Secreto]
 * Mostrar título, ocultar contenido.
 * ```
 * Sin [compact] (el modal del detalle) van todas con radio y su descripción.
 */
@Composable
fun BehaviorOptionsPanel(
    visibility: ReminderVisibility,
    importance: ReminderImportance,
    onVisibilitySelect: (ReminderVisibility) -> Unit,
    onImportanceSelect: (ReminderImportance) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 12.dp),
    ) {
        BehaviorSection(
            title = R.string.reminders_behavior_section_visibility,
            help = R.string.reminders_behavior_visibility_help,
            options = ReminderVisibility.entries,
            selected = visibility,
            label = { it.label },
            description = { it.description },
            onSelect = onVisibilitySelect,
            compact = compact,
        )
        // En compacto alcanza con el espacio entre secciones.
        if (!compact) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        BehaviorSection(
            title = R.string.reminders_behavior_section_importance,
            help = R.string.reminders_behavior_importance_help,
            options = ReminderImportance.entries,
            selected = importance,
            label = { it.label },
            description = { it.description },
            onSelect = onImportanceSelect,
            compact = compact,
        )
    }
}

/** Una sección: título con su "?" y las opciones (con radio, o chips si es [compact]). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> BehaviorSection(
    @StringRes title: Int,
    @StringRes help: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    description: (T) -> Int,
    onSelect: (T) -> Unit,
    compact: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(text = stringResource(title), modifier = Modifier.weight(1f))
            HelpButton(title = stringResource(title), help = stringResource(help))
        }
        if (compact) {
            // Mismos chips que "Repetir" en "Fecha y hora": solo borde, con padding chico para
            // que las 4 importancias entren en una fila.
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                options.forEach { option ->
                    OptionChip(
                        text = stringResource(label(option)),
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        style = OptionChipStyle.Outlined,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            // Solo la explicación de la elegida: el resto está en el "?" y en el modal del detalle.
            Text(
                text = stringResource(description(selected)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
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
    }
}

/**
 * El "?" redondo a la derecha del título de la sección. Tocarlo muestra un globo de ayuda
 * anclado al botón, con flechita:
 * ```
 * ┌──────────────────────────────┐
 * │ Visibilidad                  │
 * │ Qué se ve de la notificación │
 * │ con el teléfono bloqueado…   │
 * └─────────────────────────▾────┘
 *                          (?)
 * ```
 * Es más liviano que un diálogo: no tapa la pantalla ni necesita botón; se cierra tocando
 * en cualquier otro lado. El TooltipState guarda si está abierto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpButton(title: String, help: String) {
    // isPersistent: queda abierto hasta tocar afuera (si no, se cierra solo a los pocos segundos).
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            RichTooltip(
                title = { Text(text = title, fontWeight = FontWeight.SemiBold) },
                caretShape = TooltipDefaults.caretShape(),
                maxWidth = 260.dp,
                shape = RoundedCornerShape(14.dp),
                colors = TooltipDefaults.richTooltipColors(
                    containerColor = colors.surfaceContainerHighest,
                    contentColor = colors.onSurfaceVariant,
                    titleContentColor = colors.onSurface,
                ),
            ) {
                Text(text = help, style = MaterialTheme.typography.bodySmall)
            }
        },
        state = tooltipState,
    ) {
        Surface(
            onClick = { scope.launch { tooltipState.show() } },
            shape = CircleShape,
            color = colors.surfaceContainerHighest,
            contentColor = colors.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_question_mark),
                contentDescription = stringResource(R.string.reminders_behavior_help),
                modifier = Modifier.padding(7.dp),
            )
        }
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

@Preview(name = "Compacto (nota rápida)", showBackground = true, backgroundColor = 0xFF1E2C36, widthDp = 360)
@Composable
private fun BehaviorOptionsPanelCompactPreview() {
    RememberAppTheme {
        BehaviorOptionsPanel(
            visibility = ReminderVisibility.Private,
            importance = ReminderImportance.High,
            onVisibilitySelect = {},
            onImportanceSelect = {},
            modifier = Modifier.padding(16.dp),
            compact = true,
        )
    }
}

@Preview(name = "Completo (detalle)", showBackground = true, backgroundColor = 0xFF1E2C36, heightDp = 720)
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

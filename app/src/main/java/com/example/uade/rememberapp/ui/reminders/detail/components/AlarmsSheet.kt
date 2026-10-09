package com.example.uade.rememberapp.ui.reminders.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton
import com.example.uade.rememberapp.ui.reminders.capture.RepeatOption
import com.example.uade.rememberapp.ui.reminders.capture.TimeShortcut
import com.example.uade.rememberapp.ui.reminders.capture.components.TimeOptionsPanel
import com.example.uade.rememberapp.ui.reminders.components.formatReminderTime
import com.example.uade.rememberapp.ui.reminders.detail.AlarmDraft
import com.example.uade.rememberapp.ui.reminders.detail.AlarmsEditor
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.rememberAppLocale
import java.time.Duration
import java.time.Instant

private val CardShape = RoundedCornerShape(16.dp)

/** Todo lo que se puede hacer en el modal "Establecer recordatorio" (ver [AlarmsSheet]). */
data class AlarmsSheetActions(
    val onShortcut: (index: Int, TimeShortcut) -> Unit = { _, _ -> },
    val onRepeat: (index: Int, RepeatOption) -> Unit = { _, _ -> },
    val onToggleExpanded: (index: Int) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onSave: () -> Unit = {},
)

/**
 * Modal "Establecer recordatorio": los avisos por hora de un recordatorio, uno por tarjeta.
 * Cerrarlo deslizando o tocando afuera es lo mismo que "Cancelar".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsSheet(
    editor: AlarmsEditor,
    now: Instant,
    actions: AlarmsSheetActions,
) {
    ModalBottomSheet(
        onDismissRequest = actions.onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        AlarmsSheetContent(
            editor = editor,
            now = now,
            actions = actions,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

/**
 * ```
 * Establecer recordatorio
 * ┌ ∨ Recordatorio 1 ─────────────────────┐
 * │ CUÁNDO   [En 1 hora] [Hoy 21:00] …     │  ← los mismos chips que la nota rápida
 * │ REPETIR  [No] [Diario] …               │
 * └───────────────────────────────────────┘
 * ┌ > Recordatorio 2          Sáb 10:00    ┐  ← plegada: solo el resumen
 * ┌ ─ ─ ─  + Añadir recordatorio  ─ ─ ─ ─ ┐
 *              Borrar todo  Cancelar  (Guardar)
 * ```
 * Una tarjeta desplegada a la vez. Si las tarjetas no entran, la lista se desplaza y los botones
 * quedan siempre a la vista.
 */
@Composable
fun AlarmsSheetContent(
    editor: AlarmsEditor,
    now: Instant,
    actions: AlarmsSheetActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.reminder_alarms_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            editor.items.forEachIndexed { index, draft ->
                AlarmCard(
                    number = index + 1,
                    draft = draft,
                    isExpanded = editor.expanded == index,
                    now = now,
                    onToggleExpanded = { actions.onToggleExpanded(index) },
                    onShortcut = { actions.onShortcut(index, it) },
                    onRepeat = { actions.onRepeat(index, it) },
                )
            }
            AddAlarmButton(onClick = actions.onAdd)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DialogDismissButton(text = stringResource(R.string.reminder_alarms_clear), onClick = actions.onClear)
            Spacer(Modifier.weight(1f))
            DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = actions.onCancel)
            DialogFilledConfirmButton(text = stringResource(R.string.reminder_alarms_save), onClick = actions.onSave)
        }
    }
}

/**
 * Un aviso: encabezado "Recordatorio N" con chevron y, desplegada, los chips de la nota rápida.
 * Plegada muestra un resumen ("Sáb 10:00" o "Sin elegir").
 */
@Composable
private fun AlarmCard(
    number: Int,
    draft: AlarmDraft,
    isExpanded: Boolean,
    now: Instant,
    onToggleExpanded: () -> Unit,
    onShortcut: (TimeShortcut) -> Unit,
    onRepeat: (RepeatOption) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(if (isExpanded) R.drawable.ic_expand_more else R.drawable.ic_chevron_right),
                    contentDescription = stringResource(
                        if (isExpanded) R.string.reminder_alarms_collapse else R.string.reminder_alarms_expand,
                    ),
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.reminder_alarms_item, number),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    modifier = Modifier.weight(1f),
                )
                if (!isExpanded) {
                    Text(
                        text = draft.at?.let { formatReminderTime(it, now, rememberAppLocale()) }
                            ?: stringResource(R.string.reminder_alarms_item_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (isExpanded) {
                TimeOptionsPanel(
                    shortcuts = TimeShortcut.entries,
                    selectedShortcut = draft.selectedTime,
                    repeatOptions = RepeatOption.entries,
                    selectedRepeat = draft.selectedRepeat,
                    onShortcutClick = onShortcut,
                    onRepeatClick = onRepeat,
                    pickedDate = draft.pickedDate,
                    pickedTime = draft.pickedTime,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

/** "+ Añadir recordatorio" con borde punteado, del mismo ancho que las tarjetas. */
@Composable
private fun AddAlarmButton(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = colors.outline,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    ),
                )
            }
            .clip(CardShape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.reminder_alarms_add),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36, heightDp = 640)
@Composable
private fun AlarmsSheetContentPreview() {
    val now = Instant.now()
    RememberAppTheme {
        AlarmsSheetContent(
            editor = AlarmsEditor(
                items = listOf(
                    AlarmDraft(at = now.plus(Duration.ofHours(1)), selectedTime = TimeShortcut.InOneHour),
                    AlarmDraft(at = now.plus(Duration.ofDays(3)), selectedTime = TimeShortcut.Weekend),
                ),
                expanded = 0,
            ),
            now = now,
            actions = AlarmsSheetActions(),
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

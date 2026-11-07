package com.example.uade.rememberapp.ui.reminders.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Vista previa de una lista dentro de la card: los primeros [maxVisible] ítems y un
 * "+ N más" con el resto. Con [onToggleExpanded], "+ N más" se puede tocar para ver todos
 * ([isExpanded]) y entonces pasa a "Ver menos".
 *
 * Permite marcar ítems como terminados directamente desde la card. "Marcar todo" no va acá: está
 * arriba a la derecha de la card ([CheckAllChip]), lejos de los checkbox.
 */
@Composable
fun ChecklistPreview(
    items: List<ChecklistItem>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 2,
    onToggleItem: ((itemId: Long, index: Int) -> Unit)? = null,
    isExpanded: Boolean = false,
    onToggleExpanded: (() -> Unit)? = null,
) {
    val visibleCount = if (isExpanded) items.size else maxVisible
    val hidden = items.size - visibleCount
    val canCollapse = isExpanded && items.size > maxVisible && onToggleExpanded != null
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.take(visibleCount).forEachIndexed { index, item ->
            ChecklistPreviewItem(
                item = item,
                onToggle = if (onToggleItem != null) { { onToggleItem(item.id, index) } } else null,
            )
        }
        if (hidden > 0 || canCollapse) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hidden > 0 || canCollapse) {
                    ExpandToggle(
                        text = if (canCollapse) {
                            stringResource(R.string.reminders_card_show_less)
                        } else {
                            stringResource(R.string.reminders_checklist_more_items, hidden)
                        },
                        onClick = onToggleExpanded,
                    )
                }
            }
        }
    }
}

/**
 * "+ N más" / "Ver menos" / "Ver más". Sin [onClick] es solo informativo (ej. en el Historial).
 * El padding vertical agranda la zona de toque, que si no sería del alto de una línea chica.
 */
@Composable
internal fun ExpandToggle(text: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (onClick != null) FontWeight.SemiBold else FontWeight.Normal,
        color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
    )
}

/**
 * "✓ Marcar todo": pastilla chica, arriba a la derecha de la card, para que no se confunda con un
 * checkbox. Quien la usa pide confirmación antes de marcar (ver MainTabs).
 */
@Composable
internal fun CheckAllChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_done_all),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.reminders_checklist_check_all),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * Un ítem: checkbox y texto (y la nota debajo, si tiene).
 *
 * El checkbox va en una caja del alto de la primera línea del texto y centrado en ella: así queda
 * alineado con el texto aunque el ítem tenga nota (dos líneas). Con `Alignment.Top` solo, el ícono
 * de 20dp quedaba más arriba que la línea de 24sp del texto.
 */
@Composable
private fun ChecklistPreviewItem(
    item: ChecklistItem,
    modifier: Modifier = Modifier,
    onToggle: (() -> Unit)? = null,
) {
    val textStyle = MaterialTheme.typography.bodyLarge
    val firstLineHeight = with(LocalDensity.current) { textStyle.lineHeight.toDp() }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .height(firstLineHeight)
                .then(
                    if (onToggle != null) {
                        Modifier.clickable(role = Role.Checkbox, onClick = onToggle)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (item.isChecked) R.drawable.ic_check_box else R.drawable.ic_check_box_outline_blank,
                ),
                contentDescription = when {
                    onToggle == null -> null
                    item.isChecked -> stringResource(R.string.reminders_checklist_unmark)
                    else -> stringResource(R.string.reminders_checklist_mark_done)
                },
                tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = item.text,
                style = textStyle.copy(
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun ChecklistPreviewPreview() {
    RememberAppTheme {
        ChecklistPreview(
            items = listOf(
                ChecklistItem(id = 1, text = "Vacío 5kg", note = "En el Coto"),
                ChecklistItem(id = 2, text = "Asado 3kg", note = "En Res", isChecked = true),
                ChecklistItem(id = 3, text = "Carbón"),
                ChecklistItem(id = 4, text = "Pan"),
            ),
            onToggleItem = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}

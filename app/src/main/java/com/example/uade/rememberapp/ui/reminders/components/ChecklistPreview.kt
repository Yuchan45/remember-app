package com.example.uade.rememberapp.ui.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextDecoration

/**
 * Vista previa de una lista dentro de la card: los primeros [maxVisible] ítems y un
 * "+ N más" con el resto.
 *
 * Permite marcar ítems como terminados directamente desde la card.
 */
@Composable
fun ChecklistPreview(
    items: List<ChecklistItem>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 2,
    onToggleItem: ((itemId: Long, index: Int) -> Unit)? = null,
) {
    val hidden = items.size - maxVisible
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.take(maxVisible).forEachIndexed { index, item ->
            ChecklistPreviewItem(
                item = item,
                onToggle = if (onToggleItem != null) { { onToggleItem(item.id, index) } } else null,
            )
        }
        if (hidden > 0) {
            Text(
                text = stringResource(R.string.reminders_checklist_more_items, hidden),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChecklistPreviewItem(
    item: ChecklistItem,
    modifier: Modifier = Modifier,
    onToggle: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onToggle != null) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (item.isChecked) R.drawable.ic_check_box else R.drawable.ic_check_box_outline_blank,
                    ),
                    contentDescription = if (item.isChecked) "Marcar como pendiente" else "Marcar como terminado",
                    tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Icon(
                painter = painterResource(
                    if (item.isChecked) R.drawable.ic_check_box else R.drawable.ic_check_box_outline_blank,
                ),
                contentDescription = null,
                tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge.copy(
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
            modifier = Modifier.padding(16.dp),
        )
    }
}

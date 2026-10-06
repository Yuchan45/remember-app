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

/**
 * Vista previa de una lista dentro de la card: los primeros [maxVisible] ítems y un
 * "+ N más" con el resto.
 *
 * Los tildes son solo ícono, no checkboxes: marcar ítems se hace desde el detalle de la
 * lista, así un toque en la card siempre abre el recordatorio.
 */
@Composable
fun ChecklistPreview(
    items: List<ChecklistItem>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 2,
) {
    val hidden = items.size - maxVisible
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.take(maxVisible).forEach { item ->
            ChecklistPreviewItem(item)
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
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(
                if (item.isChecked) R.drawable.ic_check_box else R.drawable.ic_check_box_outline_blank,
            ),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

package com.example.uade.rememberapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Selector segmentado, ej. "Pendientes · 3 | Hechos · 5".
 *
 * No guarda estado: la opción elegida entra por [selected] y los toques salen por [onSelected],
 * así quien lo usa (normalmente el ViewModel de la pantalla) decide qué hacer con el cambio.
 */
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        options.forEach { option ->
            Segment(
                text = label(option),
                selected = option == selected,
                onClick = { onSelected(option) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Segment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .selectable(selected = selected, onClick = onClick, role = Role.Tab),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedSelectorPreview() {
    RememberAppTheme {
        SegmentedSelector(
            options = listOf("Pendientes · 3", "Hechos · 5"),
            selected = "Pendientes · 3",
            onSelected = {},
            label = { it },
            modifier = Modifier.padding(16.dp),
        )
    }
}

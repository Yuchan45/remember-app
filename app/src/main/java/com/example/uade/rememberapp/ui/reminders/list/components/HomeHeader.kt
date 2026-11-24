package com.example.uade.rememberapp.ui.reminders.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.CircleIconButton
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/** Encabezado de la Home: título y los botones de vista, colapsar y buscar. */
@Composable
fun HomeHeader(
    onToggleLayout: () -> Unit,
    onCollapseAll: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    isGridLayout: Boolean = false,
    areAllCollapsed: Boolean = false,
    isSearchActive: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.reminders_list_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircleIconButton(
                icon = if (isGridLayout) R.drawable.ic_notes else R.drawable.ic_grid_view,
                contentDescription = stringResource(
                    if (isGridLayout) R.string.reminders_home_action_list_layout else R.string.reminders_home_action_grid_layout,
                ),
                onClick = onToggleLayout,
                containerColor = if (isGridLayout) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isGridLayout) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            CircleIconButton(
                icon = if (areAllCollapsed) R.drawable.ic_unfold_more else R.drawable.ic_unfold_less,
                contentDescription = stringResource(
                    if (areAllCollapsed) R.string.reminders_home_action_expand_all else R.string.reminders_home_action_collapse_all,
                ),
                onClick = onCollapseAll,
            )
            CircleIconButton(
                icon = R.drawable.ic_search,
                contentDescription = stringResource(R.string.reminders_home_action_search),
                onClick = onSearch,
                containerColor = if (isSearchActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isSearchActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B141B)
@Composable
private fun HomeHeaderPreview() {
    RememberAppTheme {
        HomeHeader(
            onToggleLayout = {},
            onCollapseAll = {},
            onSearch = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

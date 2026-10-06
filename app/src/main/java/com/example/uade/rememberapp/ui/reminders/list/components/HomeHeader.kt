package com.example.uade.rememberapp.ui.reminders.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

/** Encabezado de la Home: saludo, título y los botones de vista, colapsar y buscar. */
@Composable
fun HomeHeader(
    userName: String,
    onToggleLayout: () -> Unit,
    onCollapseAll: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.reminders_home_greeting, userName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.reminders_list_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircleIconButton(
                icon = R.drawable.ic_grid_view,
                contentDescription = stringResource(R.string.reminders_home_action_toggle_layout),
                onClick = onToggleLayout,
            )
            CircleIconButton(
                icon = R.drawable.ic_unfold_less,
                contentDescription = stringResource(R.string.reminders_home_action_collapse_all),
                onClick = onCollapseAll,
            )
            CircleIconButton(
                icon = R.drawable.ic_search,
                contentDescription = stringResource(R.string.reminders_home_action_search),
                onClick = onSearch,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B141B)
@Composable
private fun HomeHeaderPreview() {
    RememberAppTheme {
        HomeHeader(
            userName = "Yu",
            onToggleLayout = {},
            onCollapseAll = {},
            onSearch = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

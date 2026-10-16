package com.example.uade.rememberapp.ui.reminders.list.components

import androidx.annotation.DrawableRes
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
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.reminders_list_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderButton(R.drawable.ic_grid_view, stringResource(R.string.reminders_home_action_toggle_layout), onToggleLayout)
            HeaderButton(R.drawable.ic_unfold_less, stringResource(R.string.reminders_home_action_collapse_all), onCollapseAll)
            HeaderButton(R.drawable.ic_search, stringResource(R.string.reminders_home_action_search), onSearch)
        }
    }
}

/** Botones del encabezado: más chicos que el [CircleIconButton] por defecto, para no competir con el título. */
@Composable
private fun HeaderButton(@DrawableRes icon: Int, contentDescription: String, onClick: () -> Unit) {
    CircleIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        size = 38.dp,
        iconSize = 19.dp,
    )
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

package com.example.uade.rememberapp.ui.reminders.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.DropdownFilterChip
import com.example.uade.rememberapp.ui.reminders.list.ReminderGrouping
import com.example.uade.rememberapp.ui.reminders.list.ReminderSort
import com.example.uade.rememberapp.ui.reminders.list.ReminderTypeFilter
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Fila de filtros de la Home ("Tipo: Todo", "Grupo: Fecha", "Ordenar: Próximos"). Se desplaza
 * de costado porque no entra entera en pantallas angostas.
 *
 * [contentPadding] deja que el primer chip quede alineado con el resto de la pantalla y que
 * al desplazar los chips lleguen hasta el borde.
 */
@Composable
fun ReminderFilterBar(
    typeFilter: ReminderTypeFilter,
    grouping: ReminderGrouping,
    sort: ReminderSort,
    onTypeFilterClick: () -> Unit,
    onGroupingClick: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            DropdownFilterChip(
                text = stringResource(R.string.reminders_filter_type_label, stringResource(typeFilter.label)),
                onClick = onTypeFilterClick,
            )
        }
        item {
            DropdownFilterChip(
                text = stringResource(R.string.reminders_filter_group_label, stringResource(grouping.label)),
                onClick = onGroupingClick,
            )
        }
        item {
            DropdownFilterChip(
                text = stringResource(R.string.reminders_filter_sort_label, stringResource(sort.label)),
                onClick = onSortClick,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B141B)
@Composable
private fun ReminderFilterBarPreview() {
    RememberAppTheme {
        ReminderFilterBar(
            typeFilter = ReminderTypeFilter.All,
            grouping = ReminderGrouping.Date,
            sort = ReminderSort.Upcoming,
            onTypeFilterClick = {},
            onGroupingClick = {},
            onSortClick = {},
            contentPadding = PaddingValues(16.dp),
        )
    }
}

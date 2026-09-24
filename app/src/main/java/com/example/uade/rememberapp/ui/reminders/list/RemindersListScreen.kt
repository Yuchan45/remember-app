package com.example.uade.rememberapp.ui.reminders.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.ui.components.SegmentedSelector
import com.example.uade.rememberapp.ui.reminders.components.ReminderList
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.time.Instant

/**
 * Pantalla de inicio: la lista de recordatorios.
 *
 * Esta función es la "con estado": lo único que hace es conseguir el ViewModel y leer su
 * UiState. No dibuja nada por su cuenta, para que el diseño quede en [RemindersListContent],
 * que se puede previsualizar y testear con estados inventados.
 */
@Composable
fun RemindersListScreen(
    viewModel: RemindersListViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RemindersListContent(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onReminderCompleted = viewModel::onReminderCompleted,
    )
}

/**
 * Recibe el estado ya resuelto y devuelve UI. Regla: no toca el ViewModel ni el dominio;
 * todo lo que necesite entra por parámetros, y lo que el usuario hace sale por lambdas
 * (ej. `onAddReminder: () -> Unit`), que se irán agregando a medida que haga falta.
 */
@Composable
private fun RemindersListContent(
    uiState: RemindersListUiState,
    onTabSelected: (ReminderTab) -> Unit,
    onReminderCompleted: (id: Long, isDone: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.reminders_list_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 16.dp),
            )

            Spacer(Modifier.height(16.dp))

            SegmentedSelector(
                options = ReminderTab.entries,
                selected = uiState.selectedTab,
                onSelected = onTabSelected,
                label = { tab ->
                    stringResource(
                        R.string.reminders_tab_with_count,
                        stringResource(tab.labelRes),
                        uiState.countFor(tab),
                    )
                },
            )

            Spacer(Modifier.height(16.dp))

            // TODO: botón para agregar y estado uiState.isLoading.
            ReminderList(
                reminders = uiState.visibleReminders,
                onReminderCheckedChange = onReminderCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RemindersListContentPreview() {
    RememberAppTheme {
        RemindersListContent(
            uiState = RemindersListUiState(
                reminders = listOf(
                    Reminder(id = 1, text = "Comprar el cargador", createdAt = Instant.now()),
                    Reminder(id = 2, text = "Pagar la cuota de la facu", createdAt = Instant.now()),
                ),
            ),
            onTabSelected = {},
            onReminderCompleted = { _, _ -> },
        )
    }
}

package com.example.uade.rememberapp.ui.reminders.list

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.ui.components.SegmentedSelector
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

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
                text = "Remember",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 16.dp),
            )

            Spacer(Modifier.height(16.dp))

            SegmentedSelector(
                options = ReminderTab.entries,
                selected = uiState.selectedTab,
                onSelected = onTabSelected,
                label = { tab -> "${tab.label} · ${uiState.countFor(tab)}" },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                // TODO: lista de recordatorios (uiState.visibleReminders) y botón para agregar.
                // Estados a cubrir: uiState.isLoading, uiState.isEmpty y el contenido.
                Text(
                    text = "Pantalla de inicio",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RemindersListContentPreview() {
    RememberAppTheme {
        RemindersListContent(
            uiState = RemindersListUiState(),
            onTabSelected = {},
        )
    }
}

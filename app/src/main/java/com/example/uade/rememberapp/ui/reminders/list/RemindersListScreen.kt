package com.example.uade.rememberapp.ui.reminders.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Trigger
import com.example.uade.rememberapp.ui.reminders.components.ReminderCard
import com.example.uade.rememberapp.ui.reminders.list.components.HomeHeader
import com.example.uade.rememberapp.ui.reminders.list.components.QuickCaptureBar
import com.example.uade.rememberapp.ui.reminders.list.components.ReminderFilterBar
import com.example.uade.rememberapp.ui.reminders.list.components.ReminderSectionHeader
import com.example.uade.rememberapp.ui.reminders.sample.SampleReminders
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.appBackground
import java.time.Instant

/** Margen lateral de la pantalla; los chips de filtro lo usan como padding para desplazarse hasta el borde. */
private val ScreenPadding = 16.dp

/**
 * Pantalla de inicio: la lista de recordatorios agrupada en secciones.
 *
 * Esta función es la "con estado": lo único que hace es conseguir el ViewModel y leer su
 * UiState. No dibuja nada por su cuenta, para que el diseño quede en [RemindersListContent],
 * que se puede previsualizar y testear con estados inventados.
 */
@Composable
fun RemindersListScreen(
    viewModel: RemindersListViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // "Ahora" fijo mientras la pantalla está abierta: decide qué es "hoy" y cuándo mostrar el "!".
    // TODO: que lo provea el ViewModel y se actualice con el paso del tiempo.
    val now = remember { Instant.now() }

    RemindersListContent(
        uiState = uiState,
        now = now,
        contentPadding = contentPadding,
        actions = RemindersListActions(
            onToggleLayout = viewModel::onToggleLayout,
            onCollapseAll = viewModel::onCollapseAll,
            onSearch = viewModel::onSearchClick,
            onTypeFilterClick = viewModel::onTypeFilterClick,
            onGroupingClick = viewModel::onGroupingClick,
            onSortClick = viewModel::onSortClick,
            onSectionToggle = viewModel::onSectionToggle,
            onReminderClick = viewModel::onReminderClick,
            onQuickCaptureClick = viewModel::onQuickCaptureClick,
            onNewReminder = viewModel::onNewReminderClick,
            onVoiceCapture = viewModel::onVoiceCaptureClick,
            onPhotoCapture = viewModel::onPhotoCaptureClick,
        ),
    )
}

/**
 * Todo lo que el usuario puede hacer en la Home. Se agrupan en una clase porque son muchas y,
 * pasadas una por una, la firma de [RemindersListContent] se vuelve difícil de leer.
 */
data class RemindersListActions(
    val onToggleLayout: () -> Unit = {},
    val onCollapseAll: () -> Unit = {},
    val onSearch: () -> Unit = {},
    val onTypeFilterClick: () -> Unit = {},
    val onGroupingClick: () -> Unit = {},
    val onSortClick: () -> Unit = {},
    val onSectionToggle: (ReminderSectionKey) -> Unit = {},
    val onReminderClick: (id: Long) -> Unit = {},
    val onQuickCaptureClick: () -> Unit = {},
    val onNewReminder: () -> Unit = {},
    val onVoiceCapture: () -> Unit = {},
    val onPhotoCapture: () -> Unit = {},
)

/**
 * Recibe el estado ya resuelto y devuelve UI. Regla: no toca el ViewModel ni el dominio;
 * todo lo que necesite entra por parámetros, y lo que el usuario hace sale por [actions].
 *
 * [contentPadding] es el espacio que ocupan la barra de estado y la barra de navegación de la
 * app (ver AppNavHost). La barra de captura se apoya justo encima de la de navegación.
 */
@Composable
private fun RemindersListContent(
    uiState: RemindersListUiState,
    now: Instant,
    actions: RemindersListActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .appBackground(),
        // Transparente para que se vea el degradé de fondo.
        containerColor = Color.Transparent,
        // Los insets del sistema ya vienen en contentPadding; si no, se sumarían dos veces.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            QuickCaptureBar(
                onClick = actions.onQuickCaptureClick,
                onNewReminder = actions.onNewReminder,
                onVoice = actions.onVoiceCapture,
                onAddPhoto = actions.onPhotoCapture,
                modifier = Modifier.padding(
                    start = ScreenPadding,
                    end = ScreenPadding,
                    bottom = contentPadding.calculateBottomPadding() + 12.dp,
                ),
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        // innerPadding.bottom = alto de la barra de captura (que ya incluye la de navegación).
        // Se usa como contentPadding y no como padding, así la lista pasa por detrás de las
        // barras y el último ítem igual queda visible al final del scroll.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                HomeHeader(
                    userName = uiState.userName,
                    onToggleLayout = actions.onToggleLayout,
                    onCollapseAll = actions.onCollapseAll,
                    onSearch = actions.onSearch,
                    modifier = Modifier.padding(horizontal = ScreenPadding),
                )
            }

            item(key = "filters") {
                ReminderFilterBar(
                    typeFilter = uiState.typeFilter,
                    grouping = uiState.grouping,
                    sort = uiState.sort,
                    onTypeFilterClick = actions.onTypeFilterClick,
                    onGroupingClick = actions.onGroupingClick,
                    onSortClick = actions.onSortClick,
                    contentPadding = PaddingValues(horizontal = ScreenPadding),
                )
            }

            if (uiState.isEmpty) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.reminders_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ScreenPadding),
                    )
                }
            }

            uiState.sections.forEach { section ->
                item(key = "section-${section.key}") {
                    ReminderSectionHeader(
                        title = stringResource(section.key.title),
                        count = section.reminders.size,
                        isExpanded = section.isExpanded,
                        onToggle = { actions.onSectionToggle(section.key) },
                        modifier = Modifier.padding(horizontal = ScreenPadding),
                    )
                }
                if (section.isExpanded) {
                    items(
                        items = section.reminders,
                        // Con la sección en la key, un recordatorio puede estar en dos secciones (ej. agrupado
                        // por etiqueta) sin repetir keys, que en un LazyColumn es un crash.
                        key = { "${section.key}-${it.id}" },
                    ) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            placeName = (reminder.trigger as? Trigger.AtPlace)
                                ?.let { uiState.placeNames[it.placeId] },
                            now = now,
                            onClick = { actions.onReminderClick(reminder.id) },
                            modifier = Modifier.padding(horizontal = ScreenPadding),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun RemindersListContentPreview() {
    val now = Instant.now()
    RememberAppTheme {
        RemindersListContent(
            uiState = SampleReminders.uiState(now),
            now = now,
            actions = RemindersListActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 500)
@Composable
private fun RemindersListContentEmptyPreview() {
    RememberAppTheme {
        RemindersListContent(
            uiState = RemindersListUiState(userName = "Yu"),
            now = Instant.now(),
            actions = RemindersListActions(),
        )
    }
}

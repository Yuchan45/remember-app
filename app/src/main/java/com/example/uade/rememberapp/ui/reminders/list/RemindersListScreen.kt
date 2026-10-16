package com.example.uade.rememberapp.ui.reminders.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.reminders.capture.QuickCaptureSheet
import com.example.uade.rememberapp.ui.reminders.components.SwipeableReminderCard
import com.example.uade.rememberapp.ui.reminders.components.closeSwipeOnTapOutside
import com.example.uade.rememberapp.ui.reminders.components.SwipeRevealGroupState
import com.example.uade.rememberapp.ui.reminders.components.rememberSwipeRevealGroupState
import com.example.uade.rememberapp.ui.reminders.list.components.HomeHeader
import com.example.uade.rememberapp.ui.reminders.list.components.ReminderFilterBar
import com.example.uade.rememberapp.ui.reminders.list.components.ReminderSectionHeader
import com.example.uade.rememberapp.ui.reminders.sample.SampleReminders
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.appBackground
import java.time.Instant

/** Margen lateral de la pantalla; los chips de filtro lo usan como padding para desplazarse hasta el borde. */
private val ScreenPadding = 16.dp

/** Alto del desvanecido de la lista bajo el encabezado fijo. */
private val TopFadeHeight = 28.dp

/**
 * Pantalla de inicio: la lista de recordatorios agrupada en secciones.
 *
 * Esta función es la "con estado": lo único que hace es conseguir el ViewModel y leer su
 * UiState. No dibuja nada por su cuenta, para que el diseño quede en [RemindersListContent],
 * que se puede previsualizar y testear con estados inventados.
 */
@Composable
fun RemindersListScreen(
    viewModel: RemindersListViewModel = viewModel(factory = RemindersListViewModel.Factory),
    contentPadding: PaddingValues = PaddingValues(),
    onReminderClick: (id: Long) -> Unit = {},
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
            onReminderClick = onReminderClick,
            onReminderDone = viewModel::onReminderDone,
            onReminderArchive = viewModel::onReminderArchive,
            onReminderTrash = viewModel::onReminderTrash,
        ),
    )

    if (uiState.isQuickCaptureOpen) {
        QuickCaptureSheet(onDismiss = viewModel::onQuickCaptureDismiss)
    }
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
    val onReminderDone: (id: Long) -> Unit = {},
    val onReminderArchive: (id: Long) -> Unit = {},
    val onReminderTrash: (id: Long) -> Unit = {},
)

/**
 * Recibe el estado ya resuelto y devuelve UI. Regla: no toca el ViewModel ni el dominio;
 * todo lo que necesite entra por parámetros, y lo que el usuario hace sale por [actions].
 *
 * [contentPadding] es el espacio que ocupan la barra de estado y la barra inferior de la app
 * ("Nota rápida…" y la navegación, ver MainTabs).
 */
@Composable
private fun RemindersListContent(
    uiState: RemindersListUiState,
    now: Instant,
    actions: RemindersListActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    // Qué card está deslizada mostrando sus acciones (una a la vez). Es estado visual: vive acá.
    val revealGroup = rememberSwipeRevealGroupState()
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = contentPadding.calculateStartPadding(layoutDirection)
    val endPadding = contentPadding.calculateEndPadding(layoutDirection)

    Column(
        modifier = modifier
            .fillMaxSize()
            // Tocar en cualquier lado que no sea la card abierta la cierra.
            .closeSwipeOnTapOutside(revealGroup)
            .appBackground(),
    ) {
        // El encabezado va afuera de la lista: queda fijo arriba y la lista se desplaza
        // debajo (se recorta en su borde superior). Los filtros sí son parte de la lista.
        HomeHeader(
            onToggleLayout = actions.onToggleLayout,
            onCollapseAll = actions.onCollapseAll,
            onSearch = actions.onSearch,
            modifier = Modifier
                .padding(top = contentPadding.calculateTopPadding() + 8.dp, bottom = 8.dp)
                .padding(start = startPadding, end = endPadding)
                .padding(horizontal = ScreenPadding),
        )

        // contentPadding.bottom = alto de la barra inferior de la app. Se usa como contentPadding
        // y no como padding, así la lista pasa por detrás de la barra (se ve desenfocada) y el
        // último ítem igual queda visible al final del scroll.
        RemindersList(
            uiState = uiState,
            now = now,
            actions = actions,
            revealGroup = revealGroup,
            contentPadding = PaddingValues(
                top = 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
                start = startPadding,
                end = endPadding,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

/** Los filtros, las secciones y sus cards: todo lo que se desplaza debajo del encabezado. */
@Composable
private fun RemindersList(
    uiState: RemindersListUiState,
    now: Instant,
    actions: RemindersListActions,
    revealGroup: SwipeRevealGroupState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // El desvanecido de arriba aparece recién cuando hay algo escondido detrás del encabezado:
    // sin scroll, los filtros se ven enteros. Animado para que no aparezca de golpe.
    val topFade by animateFloatAsState(
        targetValue = if (listState.canScrollBackward) 1f else 0f,
        label = "topFade",
    )

    LazyColumn(
        state = listState,
        modifier = modifier.topFadingEdge(height = TopFadeHeight, strength = { topFade }),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                    SwipeableReminderCard(
                        reminder = reminder,
                        placeName = reminder.places.firstOrNull()
                            ?.let { uiState.placeNames[it.placeId] },
                        now = now,
                        onClick = { actions.onReminderClick(reminder.id) },
                        onDone = { actions.onReminderDone(reminder.id) },
                        onArchive = { actions.onReminderArchive(reminder.id) },
                        onTrash = { actions.onReminderTrash(reminder.id) },
                        modifier = Modifier.padding(horizontal = ScreenPadding),
                        revealGroup = revealGroup,
                    )
                }
            }
        }
    }
}

/**
 * Desvanece el borde de arriba: el contenido se vuelve transparente de a poco en los primeros
 * [height], así lo que se desplaza debajo del encabezado no se corta en seco. [strength] va de 0
 * (sin desvanecido) a 1 (arriba de todo, totalmente transparente).
 *
 * Cómo funciona: se dibuja el contenido y encima una franja con un degradé en modo
 * [BlendMode.DstIn], que deja del contenido solo lo que coincide con la opacidad del degradé
 * (como una máscara). Offscreen hace que la máscara afecte solo a esta capa y no al fondo de la
 * pantalla. [strength] se lee al dibujar, así animarlo no recompone la lista.
 */
private fun Modifier.topFadingEdge(height: Dp, strength: () -> Float): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = strength()
        if (fade > 0f) {
            val heightPx = height.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 1f - fade), Color.Black),
                    startY = 0f,
                    endY = heightPx,
                ),
                size = Size(size.width, heightPx),
                blendMode = BlendMode.DstIn,
            )
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
            uiState = RemindersListUiState(),
            now = Instant.now(),
            actions = RemindersListActions(),
        )
    }
}

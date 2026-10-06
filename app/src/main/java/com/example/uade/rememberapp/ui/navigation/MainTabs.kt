package com.example.uade.rememberapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.archive.ArchiveScreen
import com.example.uade.rememberapp.ui.components.CircleIconButton
import com.example.uade.rememberapp.ui.components.CookieShape
import com.example.uade.rememberapp.ui.places.list.PlacesListScreen
import com.example.uade.rememberapp.ui.reminders.create.CreateReminderSheet
import com.example.uade.rememberapp.ui.reminders.list.RemindersListScreen
import com.example.uade.rememberapp.ui.reminders.list.RemindersListViewModel
import com.example.uade.rememberapp.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * Las cuatro pantallas principales como páginas de un pager, con la barra inferior encima.
 *
 * - Solo se cambia de pestaña tocando la barra (deslizar con el dedo está desactivado); el
 *   cambio se anima deslizando hasta esa página.
 * - "Atrás" desde otra pestaña vuelve a Inicio; desde Inicio sale de la app.
 *
 * El Scaffold calcula cuánto ocupan la barra de estado y la barra inferior y se lo pasa a
 * cada pantalla como `contentPadding`, así el contenido pasa por detrás de la barra flotante.
 *
 * Al lado de la barra está el "+" de nuevo recordatorio, en todas las pestañas. Abre el menú
 * "Crear", que se dibuja acá. Usa el mismo [RemindersListViewModel] que la pantalla de Inicio:
 * `viewModel()` devuelve la misma instancia porque las dos viven en el mismo destino de
 * navegación.
 */
@Composable
fun MainTabs(
    modifier: Modifier = Modifier,
    remindersViewModel: RemindersListViewModel = viewModel(factory = RemindersListViewModel.Factory),
) {
    val remindersState by remindersViewModel.uiState.collectAsStateWithLifecycle()
    val destinations = AppDestination.entries
    val pagerState = rememberPagerState(pageCount = { destinations.size })
    val scope = rememberCoroutineScope()

    // targetPage cambia apenas se toca un destino, así la barra no espera a que termine la
    // animación del cambio de página.
    val selected = destinations[pagerState.targetPage]

    BackHandler(enabled = pagerState.currentPage != AppDestination.Home.ordinal) {
        scope.launch { pagerState.animateScrollToPage(AppDestination.Home.ordinal) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    TrashedReminderSnackbar(
        trashedReminderId = remindersState.trashedReminderId,
        snackbarHostState = snackbarHostState,
        onUndo = remindersViewModel::onUndoTrash,
        onDone = remindersViewModel::onTrashNoticeDone,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Cada pantalla dibuja su propio fondo.
        containerColor = Color.Transparent,
        // El Scaffold lo ubica justo encima de la barra inferior.
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Barra y "+" juntos, centrados como grupo (no repartidos a los extremos).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppBottomBar(
                    selected = selected,
                    onSelected = { destination ->
                        scope.launch { pagerState.animateScrollToPage(destination.ordinal) }
                    },
                )
                CircleIconButton(
                    icon = R.drawable.ic_add,
                    contentDescription = stringResource(R.string.reminders_quick_capture_new),
                    onClick = remindersViewModel::onNewReminderClick,
                    // Mismo alto que la barra (48dp de destino + 4dp de margen arriba y abajo).
                    size = 56.dp,
                    shape = CookieShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { contentPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Solo se cambia de pestaña con la barra: deslizar de costado no hace nada.
            userScrollEnabled = false,
            key = { destinations[it].name },
        ) { page ->
            MainTabPage(
                destination = destinations[page],
                contentPadding = contentPadding,
            )
        }
    }

    // Va acá y no en una pestaña: el pager solo compone la página visible, y el menú se tiene
    // que poder abrir desde cualquiera.
    if (remindersState.isCreateMenuOpen) {
        CreateReminderSheet(
            onOptionClick = remindersViewModel::onCreateOptionClick,
            onDismiss = remindersViewModel::onCreateMenuDismiss,
        )
    }
}

/**
 * Muestra "1 movido a la papelera · Deshacer ✕" unos segundos cada vez que [trashedReminderId]
 * pasa a tener un recordatorio. "Deshacer" avisa por [onUndo]; al terminar el aviso (por deshacer, cerrar con
 * la ✕ o vencerse) avisa por [onDone] para que el ViewModel lo olvide.
 *
 * Si se manda otro a la papelera mientras se ve el aviso, el LaunchedEffect se reinicia con el
 * id nuevo: el aviso anterior se cierra y el nuevo ofrece deshacer el último.
 */
@Composable
private fun TrashedReminderSnackbar(
    trashedReminderId: Long?,
    snackbarHostState: SnackbarHostState,
    onUndo: (Long) -> Unit,
    onDone: (Long) -> Unit,
) {
    val message = pluralStringResource(R.plurals.reminders_trashed, 1, 1)
    val undoLabel = stringResource(R.string.reminders_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDone by rememberUpdatedState(onDone)

    LaunchedEffect(trashedReminderId) {
        val id = trashedReminderId ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            withDismissAction = true,
            // Corto (4s): es informativo, para deshacer rápido un error.
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnUndo(id)
        currentOnDone(id)
    }
}

@Composable
private fun MainTabPage(
    destination: AppDestination,
    contentPadding: PaddingValues,
) {
    when (destination) {
        AppDestination.Home -> RemindersListScreen(contentPadding = contentPadding)
        AppDestination.Archive -> ArchiveScreen(contentPadding = contentPadding)
        AppDestination.Places -> PlacesListScreen(contentPadding = contentPadding)
        AppDestination.Settings -> SettingsScreen(contentPadding = contentPadding)
    }
}

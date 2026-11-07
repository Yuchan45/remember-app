package com.example.uade.rememberapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.AppDialogDefaults
import com.example.uade.rememberapp.ui.components.CircleIconButton
import com.example.uade.rememberapp.ui.components.CookieShape
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton
import com.example.uade.rememberapp.ui.components.NavigationBarFadeColor
import com.example.uade.rememberapp.ui.components.UndoSnackbarEffect
import com.example.uade.rememberapp.ui.history.HistoryScreen
import com.example.uade.rememberapp.ui.places.list.PlacesListScreen
import com.example.uade.rememberapp.ui.reminders.create.CreateReminderOption
import com.example.uade.rememberapp.ui.reminders.model.ReminderDetailSection
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
    onReminderClick: (id: Long) -> Unit = {},
    /** Tocar un chip de una card (hora, lugar, etiquetas): el detalle con ese modal abierto. */
    onReminderSectionClick: (id: Long, section: ReminderDetailSection) -> Unit = { _, _ -> },
    /** Abre el editor de una nota nueva; con `withList`, ya con una lista para escribir. */
    onCreateNote: (withList: Boolean, title: String?) -> Unit = { _, _ -> },
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

    // Uno solo para toda la app: lo usan la Home y el Historial, y el Scaffold lo dibuja encima
    // de la barra inferior.
    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.reminders_undo)
    UndoSnackbarEffect(
        targetId = remindersState.trashedReminderId,
        message = pluralStringResource(R.plurals.reminders_trashed, 1, 1),
        actionLabel = undoLabel,
        snackbarHostState = snackbarHostState,
        onAction = remindersViewModel::onUndoTrash,
        onDone = remindersViewModel::onTrashNoticeDone,
    )
    UndoSnackbarEffect(
        targetId = remindersState.archivedReminderId,
        message = stringResource(R.string.reminders_archived),
        actionLabel = undoLabel,
        snackbarHostState = snackbarHostState,
        onAction = remindersViewModel::onUndoArchive,
        onDone = remindersViewModel::onArchiveNoticeDone,
    )
    UndoSnackbarEffect(
        targetId = remindersState.checkedAllReminderId,
        message = stringResource(R.string.reminders_checklist_all_checked),
        actionLabel = undoLabel,
        snackbarHostState = snackbarHostState,
        onAction = remindersViewModel::onUndoCheckAll,
        onDone = remindersViewModel::onCheckAllNoticeDone,
    )
    UndoSnackbarEffect(
        targetId = remindersState.completedReminderId,
        message = stringResource(R.string.reminder_completed),
        actionLabel = undoLabel,
        snackbarHostState = snackbarHostState,
        onAction = remindersViewModel::onUndoDone,
        onDone = remindersViewModel::onDoneNoticeDone,
    )
    if (remindersState.incompleteChecklistReminderId != null) {
        IncompleteChecklistDialog(
            onCompleteAndDone = remindersViewModel::onCompleteChecklistAndDone,
            onDismiss = remindersViewModel::onIncompleteChecklistDismiss,
        )
    }
    if (remindersState.pendingCheckAllReminderId != null) {
        CheckAllConfirmDialog(
            onConfirm = remindersViewModel::onConfirmCheckAll,
            onDismiss = remindersViewModel::onCheckAllDismiss,
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Cada pantalla dibuja su propio fondo.
        containerColor = Color.Transparent,
        // El Scaffold lo ubica justo encima de la barra inferior.
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Barra y "+" juntos, centrados como grupo (no repartidos a los extremos).
            // Detrás, un degradé de transparente a negro translúcido: el contenido que pasa por
            // debajo se va oscureciendo, y abajo empalma con el velo de la barra del sistema
            // (NavigationBarScrim, en MainActivity), que sigue oscureciendo hasta el borde.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Primero el inset: el degradé termina justo arriba de la barra del sistema.
                    .navigationBarsPadding()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, NavigationBarFadeColor)))
                    .padding(top = 20.dp, bottom = 8.dp),
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
                snackbarHostState = snackbarHostState,
                onReminderClick = onReminderClick,
                onReminderSectionClick = onReminderSectionClick,
                onCreateNote = onCreateNote,
            )
        }
    }

    // Va acá y no en una pestaña: el pager solo compone la página visible, y el menú se tiene
    // que poder abrir desde cualquiera.
    if (remindersState.isCreateMenuOpen) {
        CreateReminderSheet(
            // Llega cuando el modal ya terminó de bajar: se saca y, si es "Nota", se navega.
            // Navegar es cosa de la UI; el resto de los tipos todavía es maquetado y lo recibe
            // el ViewModel.
            onOptionClick = { option ->
                remindersViewModel.onCreateMenuDismiss()
                when (option) {
                    CreateReminderOption.Note -> onCreateNote(false, null)
                    else -> remindersViewModel.onCreateOptionClick(option)
                }
            },
            onDismiss = remindersViewModel::onCreateMenuDismiss,
        )
    }
}

/**
 * Al marcar como hecho una nota con ítems sin tildar:
 * ```
 * La lista no está completa
 * Para finalizar el recordatorio se debe marcar toda la checklist.
 *              [Cancelar]  [Marcar todo y mover a Hecho]
 * ```
 * Va en un diálogo (y no en un snackbar) porque ofrece una acción que cambia los datos.
 */
@Composable
private fun IncompleteChecklistDialog(
    onCompleteAndDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        icon = { Icon(painterResource(R.drawable.ic_done_all), contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.checklist_incomplete_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.checklist_incomplete_for_done),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            DialogFilledConfirmButton(
                text = stringResource(R.string.checklist_incomplete_complete_and_done),
                onClick = onCompleteAndDone,
            )
        },
        dismissButton = {
            DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = onDismiss)
        },
    )
}

@Composable
private fun MainTabPage(
    destination: AppDestination,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onReminderClick: (id: Long) -> Unit,
    onReminderSectionClick: (id: Long, section: ReminderDetailSection) -> Unit,
    onCreateNote: (withList: Boolean, title: String?) -> Unit,
) {
    when (destination) {
        AppDestination.Home -> RemindersListScreen(
            contentPadding = contentPadding,
            onReminderClick = onReminderClick,
            onReminderSectionClick = onReminderSectionClick,
            onOpenListEditor = { title -> onCreateNote(true, title) },
        )
        AppDestination.History -> HistoryScreen(
            contentPadding = contentPadding,
            snackbarHostState = snackbarHostState,
            onReminderClick = onReminderClick,
        )
        AppDestination.Places -> PlacesListScreen(contentPadding = contentPadding)
        AppDestination.Settings -> SettingsScreen(contentPadding = contentPadding)
    }
}

/**
 * Confirmación de "Marcar todo" en una card:
 * ```
 * ¿Marcar toda la lista?
 * Se van a tildar todos los ítems de este recordatorio.
 *                              [Cancelar]  [Marcar todo]
 * ```
 * Igual queda el "Deshacer" del aviso después, por si se confirmó sin querer.
 */
@Composable
private fun CheckAllConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        icon = { Icon(painterResource(R.drawable.ic_done_all), contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.reminders_checklist_check_all_confirm_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.reminders_checklist_check_all_confirm_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            DialogFilledConfirmButton(
                text = stringResource(R.string.reminders_checklist_check_all),
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = onDismiss)
        },
    )
}

package com.example.uade.rememberapp.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.ui.components.AppDialogDefaults
import com.example.uade.rememberapp.ui.components.CircleIconButton
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton
import com.example.uade.rememberapp.ui.components.PlaceholderMessage
import com.example.uade.rememberapp.ui.components.SegmentedSelector
import com.example.uade.rememberapp.ui.components.UndoSnackbarEffect
import com.example.uade.rememberapp.ui.reminders.components.SwipeCardAction
import com.example.uade.rememberapp.ui.reminders.components.SwipeableReminderCard
import com.example.uade.rememberapp.ui.reminders.components.closeSwipeOnTapOutside
import com.example.uade.rememberapp.ui.reminders.components.rememberSwipeRevealGroupState
import com.example.uade.rememberapp.ui.reminders.sample.SampleReminders
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.SwipeRestoreColor
import com.example.uade.rememberapp.ui.theme.SwipeTrashColor
import com.example.uade.rememberapp.ui.theme.appBackground
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * Historial: los recordatorios hechos, archivados y eliminados, en tres pestañas.
 *
 * Esta función es la "con estado": consigue el ViewModel, lee su UiState y muestra el aviso de
 * "Restaurado · Deshacer" en el [snackbarHostState] de MainTabs. El diseño está en
 * [HistoryContent].
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
    contentPadding: PaddingValues = PaddingValues(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    /** Tocar una card: el tipo decide la pantalla (un mensaje programado tiene la suya). */
    onReminderClick: (id: Long, type: ReminderType) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UndoSnackbarEffect(
        targetId = uiState.restored?.id,
        message = stringResource(R.string.history_restored),
        actionLabel = stringResource(R.string.reminders_undo),
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onUndoRestore,
        onDone = viewModel::onRestoreNoticeDone,
    )

    HistoryContent(
        uiState = uiState,
        actions = HistoryActions(
            onTabSelected = viewModel::onTabSelected,
            onReminderClick = onReminderClick,
            onRestore = viewModel::onRestore,
            onDeleteForever = viewModel::onDeleteForeverClick,
            onEmptyClick = viewModel::onEmptyClick,
        ),
        contentPadding = contentPadding,
    )

    if (uiState.emptyConfirmStep != EmptyConfirmStep.None) {
        EmptyTabDialog(
            step = uiState.emptyConfirmStep,
            tab = uiState.selectedTab,
            count = uiState.visibleReminders.size,
            onConfirm = viewModel::onEmptyConfirm,
            onDismiss = viewModel::onEmptyDismiss,
        )
    }
    if (uiState.pendingDeleteId != null) {
        DeleteForeverDialog(
            onConfirm = viewModel::onDeleteForeverConfirm,
            onDismiss = viewModel::onDeleteForeverDismiss,
        )
    }
}

/** Los eventos del Historial, agrupados para no pasar una lambda por parámetro. */
data class HistoryActions(
    val onTabSelected: (HistoryTab) -> Unit = {},
    val onReminderClick: (id: Long, type: ReminderType) -> Unit = { _, _ -> },
    val onRestore: (id: Long) -> Unit = {},
    val onDeleteForever: (id: Long) -> Unit = {},
    val onEmptyClick: () -> Unit = {},
)

/**
 * ```
 * Historial                                  [🧹]
 * [ ✓ Hechos | ▣ Archivados | 🗑 Eliminados ]
 * Se borran solos 7 días después de completarse.
 * (cards: tocar abre el detalle; deslizar → Restaurar · Borrar)
 *   Se borra en 3 días
 * ```
 */
@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    actions: HistoryActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val revealGroup = rememberSwipeRevealGroupState()
    val restoreLabel = stringResource(R.string.history_action_restore)
    val deleteLabel = stringResource(R.string.history_action_delete_forever)

    Column(
        modifier = modifier
            .fillMaxSize()
            .appBackground()
            .closeSwipeOnTapOutside(revealGroup)
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (uiState.canEmptySelectedTab) {
                CircleIconButton(
                    icon = R.drawable.ic_delete_sweep,
                    contentDescription = stringResource(R.string.history_action_empty),
                    onClick = actions.onEmptyClick,
                    contentColor = MaterialTheme.colorScheme.error,
                )
            }
        }

        SegmentedSelector(
            options = HistoryTab.entries,
            selected = uiState.selectedTab,
            onSelected = actions.onTabSelected,
            label = { stringResource(it.label) },
            icon = { it.icon },
        )

        Text(
            text = stringResource(uiState.selectedTab.retentionMessage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val reminders = uiState.visibleReminders
        if (reminders.isEmpty()) {
            PlaceholderMessage(
                icon = uiState.selectedTab.icon,
                message = stringResource(uiState.selectedTab.emptyMessage),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = reminders, key = { it.id }) { reminder ->
                    Column(
                        modifier = Modifier.animateItem(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SwipeableReminderCard(
                            reminder = reminder,
                            placeName = reminder.places.firstOrNull()
                                ?.let { uiState.placeNames[it.placeId] },
                            now = uiState.now,
                            onClick = { actions.onReminderClick(reminder.id, reminder.type) },
                            actions = listOf(
                                SwipeCardAction(
                                    R.drawable.ic_restore_from_trash,
                                    restoreLabel,
                                    SwipeRestoreColor,
                                ) { actions.onRestore(reminder.id) },
                                SwipeCardAction(
                                    R.drawable.ic_delete_forever,
                                    deleteLabel,
                                    SwipeTrashColor,
                                ) { actions.onDeleteForever(reminder.id) },
                            ),
                            revealGroup = revealGroup,
                        )
                        uiState.daysUntilExpiry(reminder)?.let { days ->
                            ExpiryLabel(days = days, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

/** "Se borra en 3 días" / "Se borra hoy", chiquito debajo de la card. */
@Composable
private fun ExpiryLabel(days: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_history),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = if (days == 0L) {
                stringResource(R.string.history_expires_today)
            } else {
                pluralStringResource(R.plurals.history_expires_in, days.toInt(), days.toInt())
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * La doble confirmación de "Vaciar":
 * 1. "¿Vaciar Eliminados? Se van a borrar N recordatorios…"  [Cancelar] [Continuar]
 * 2. "¿Seguro? Esta acción no se puede deshacer."            [Cancelar] [Borrar N]
 *
 * En el paso final, "Borrar N" está deshabilitado un segundo: así un doble toque apurado sobre
 * "Continuar" no confirma también el segundo diálogo.
 */
@Composable
private fun EmptyTabDialog(
    step: EmptyConfirmStep,
    tab: HistoryTab,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var finalEnabled by remember(step) { mutableStateOf(step != EmptyConfirmStep.Final) }
    LaunchedEffect(step) {
        if (step == EmptyConfirmStep.Final) {
            delay(Duration.ofSeconds(1).toMillis())
            finalEnabled = true
        }
    }

    val isFinal = step == EmptyConfirmStep.Final
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_delete_sweep),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = if (isFinal) {
                    stringResource(R.string.history_empty_final_title)
                } else {
                    stringResource(R.string.history_empty_confirm_title, stringResource(tab.label))
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = if (isFinal) {
                    stringResource(R.string.history_empty_final_message)
                } else {
                    pluralStringResource(R.plurals.history_empty_confirm_message, count, count)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            if (isFinal) {
                DestructiveButton(
                    text = pluralStringResource(R.plurals.history_empty_final_confirm, count, count),
                    enabled = finalEnabled,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm()
                    },
                )
            } else {
                DialogFilledConfirmButton(
                    text = stringResource(R.string.history_empty_confirm_continue),
                    onClick = onConfirm,
                )
            }
        },
        dismissButton = {
            DialogDismissButton(text = stringResource(R.string.history_cancel), onClick = onDismiss)
        },
    )
}

/** "¿Borrar para siempre?" de una sola card. */
@Composable
private fun DeleteForeverDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        title = {
            Text(
                text = stringResource(R.string.history_delete_one_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.history_delete_one_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            DestructiveButton(
                text = stringResource(R.string.history_action_delete_forever),
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogDismissButton(text = stringResource(R.string.history_cancel), onClick = onDismiss)
        },
    )
}

/** Botón relleno en rojo, para lo que no se puede deshacer. */
@Composable
private fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(name = "Hechos vacío")
@Composable
private fun HistoryContentEmptyPreview() {
    RememberAppTheme {
        HistoryContent(uiState = HistoryUiState(), actions = HistoryActions())
    }
}

@Preview(name = "Eliminados con recordatorios", heightDp = 800)
@Composable
private fun HistoryContentDeletedPreview() {
    val now = Instant.now()
    val deleted: List<Reminder> = SampleReminders.previewReminders(now).mapIndexed { i, reminder ->
        reminder.copy(
            status = ReminderStatus.Deleted,
            statusChangedAt = now.minus(Duration.ofDays(i * 4L)),
        )
    }
    RememberAppTheme {
        HistoryContent(
            uiState = HistoryUiState(
                selectedTab = HistoryTab.Deleted,
                remindersByTab = mapOf(HistoryTab.Deleted to deleted),
                placeNames = SampleReminders.placeNames,
                now = now,
            ),
            actions = HistoryActions(),
        )
    }
}

@Preview(name = "Vaciar: confirmación final")
@Composable
private fun EmptyTabDialogPreview() {
    RememberAppTheme {
        EmptyTabDialog(
            step = EmptyConfirmStep.Final,
            tab = HistoryTab.Deleted,
            count = 4,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

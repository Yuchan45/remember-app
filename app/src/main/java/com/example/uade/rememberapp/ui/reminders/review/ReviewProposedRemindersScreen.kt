package com.example.uade.rememberapp.ui.reminders.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.ui.components.AppDialogDefaults
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton
import com.example.uade.rememberapp.ui.permissions.rememberTriggerPermissionsCoordinator
import com.example.uade.rememberapp.ui.reminders.detail.ReminderDetailScreen
import com.example.uade.rememberapp.ui.theme.LocalIsDarkTheme

/**
 * Pantalla completa de revisión de recordatorios propuestos por la IA.
 * Reproduce fielmente los mockups:
 * - 03.B: Split automático en múltiples recordatorios.
 * - 03.C: Sugerencia modal ¿Los unimos?
 * - 03.D: Texto o audio largo a checklist con items sugeridos aparte.
 */
@Composable
fun ReviewProposedRemindersScreen(
    analysisResult: AiAnalysisResult,
    onDismiss: () -> Unit,
    onSuccessSaved: () -> Unit,
    viewModel: ReviewProposedRemindersViewModel = viewModel(factory = ReviewProposedRemindersViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionsCoordinator = rememberTriggerPermissionsCoordinator()

    LaunchedEffect(analysisResult) {
        viewModel.initialize(analysisResult)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSuccessSaved()
        }
    }

    // Cerrar (con ✕ o el "atrás" del sistema) descarta lo que armó la IA a partir del audio, y
    // eso no se puede recuperar: se pregunta antes. Sin propuestas no hay nada que perder.
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val requestClose = {
        if (uiState.proposals.isNotEmpty() && !uiState.isSaved) showDiscardDialog = true else onDismiss()
    }

    Dialog(
        onDismissRequest = requestClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            // Un toque sin querer afuera no tiene que cerrar (ni preguntar): solo ✕ o "atrás".
            dismissOnClickOutside = false,
        ),
    ) {
        ReviewProposedRemindersContent(
            uiState = uiState,
            onDismiss = requestClose,
            onToggleProposal = viewModel::onToggleProposalSelection,
            onToggleChecklistItem = viewModel::onToggleChecklistItem,
            onSetChecklistView = viewModel::onSetChecklistView,
            onAddNewProposal = viewModel::onAddNewProposal,
            onStartEdit = viewModel::onStartEdit,
            onDismissEdit = viewModel::onDismissEdit,
            onMergeAllIntoOne = viewModel::onMergeAllIntoOne,
            onUnmergeSelected = viewModel::onUnmergeSelected,
            onUnmergeProposal = viewModel::onUnmergeProposal,
            onCreateClick = {
                val selected = uiState.proposals.filter { it.isSelected }
                val needsTime = selected.any { proposal ->
                    val hasDraftAlarms = proposal.draft?.alarms?.isNotEmpty() == true
                    val hasProposedAlarms = proposal.alarms.isNotEmpty() ||
                        (!proposal.triggerDisplayTime.isNullOrBlank() &&
                            !proposal.triggerDisplayTime.equals("Sin fecha", ignoreCase = true))
                    hasDraftAlarms || (proposal.draft == null && hasProposedAlarms)
                }
                val needsPlace = selected.any { it.draft?.places?.isNotEmpty() == true }
                permissionsCoordinator.runWithPermissions(
                    needsTimeAlarm = needsTime,
                    needsPlaceAlert = needsPlace,
                ) {
                    viewModel.onCreateClick()
                }
            },
        )

        // ✏️ de una propuesta: el mismo editor que cualquier recordatorio (no un modal aparte).
        // Trabaja sobre un borrador: ✓ vuelve a la revisión con los cambios, sin guardar todavía.
        val editing = uiState.editingProposal
        if (editing != null) {
            val draft = remember(editing.tempId) { viewModel.draftFor(editing) }
            Dialog(
                onDismissRequest = viewModel::onDismissEdit,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                    dismissOnClickOutside = false,
                ),
            ) {
                ReminderDetailScreen(
                    onBack = viewModel::onDismissEdit,
                    draft = draft,
                    onDraftSaved = { edited -> viewModel.onDraftEdited(editing.tempId, edited) },
                )
            }
        }

        if (showDiscardDialog) {
            DiscardProposalsDialog(
                count = uiState.proposals.size,
                onDiscard = {
                    showDiscardDialog = false
                    onDismiss()
                },
                onKeepReviewing = { showDiscardDialog = false },
            )
        }
    }
}

/**
 * Confirmación antes de cerrar la revisión:
 * ```
 * ¿Descartar los recordatorios?
 * Se van a perder los 2 recordatorios que armó la IA con tu audio.
 *                              [Seguir revisando]  [Descartar]
 * ```
 * "Seguir revisando" es el botón destacado y también lo que pasa al tocar afuera o "atrás": la
 * opción segura es no perder nada.
 */
@Composable
private fun DiscardProposalsDialog(
    count: Int,
    onDiscard: () -> Unit,
    onKeepReviewing: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepReviewing,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        title = {
            Text(
                text = stringResource(R.string.review_discard_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = pluralStringResource(R.plurals.review_discard_message, count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            DialogFilledConfirmButton(
                text = stringResource(R.string.review_discard_keep),
                onClick = onKeepReviewing,
            )
        },
        dismissButton = {
            DialogDismissButton(text = stringResource(R.string.review_discard_confirm), onClick = onDiscard)
        },
    )
}

@Composable
private fun ReviewProposedRemindersContent(
    uiState: ReviewProposedRemindersUiState,
    onDismiss: () -> Unit,
    onToggleProposal: (tempId: String) -> Unit,
    onToggleChecklistItem: (proposalId: String, itemIndex: Int) -> Unit,
    onSetChecklistView: (Boolean) -> Unit,
    onAddNewProposal: () -> Unit,
    onStartEdit: (ProposedReminder) -> Unit,
    onDismissEdit: () -> Unit,
    onMergeAllIntoOne: () -> Unit,
    onUnmergeSelected: () -> Unit,
    onUnmergeProposal: (tempId: String) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val badgeGoldBg = if (isDark) Color(0xFF2C2415) else Color(0xFFFFF6DC)
    val badgeGoldBorder = if (isDark) Color(0xFF6B5120) else Color(0xFFD4A72C)
    val badgeGoldText = if (isDark) Color(0xFFFFD54F) else Color(0xFF7A5200)
    val secondaryActionBg = MaterialTheme.colorScheme.surfaceContainerHighest

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Barra superior con botón cerrar y título
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isChecklistCandidate) "Desde texto largo" else "Revisar recordatorios",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // 2. Tarjeta "Lo que dijiste" con cita textual
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_mic),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (uiState.isChecklistCandidate) "TEXTO DICTADO" else "Lo que dijiste",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Text(
                            text = "“${uiState.rawTranscript}”",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }

            // 3. Banner dorado con aviso de la IA (✨)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, badgeGoldBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = badgeGoldBg,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_auto_awesome),
                            contentDescription = null,
                            tint = badgeGoldText,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = uiState.summaryBadge,
                            style = MaterialTheme.typography.bodySmall,
                            color = badgeGoldText,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            // 4. Selector toggle Checklist vs Separados (para 03.D)
            if (uiState.isChecklistCandidate) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (uiState.isChecklistView) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onSetChecklistView(true) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_checklist),
                                    contentDescription = null,
                                    tint = if (uiState.isChecklistView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Checklist",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (uiState.isChecklistView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (!uiState.isChecklistView) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onSetChecklistView(false) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_notes),
                                    contentDescription = null,
                                    tint = if (!uiState.isChecklistView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Separados",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (!uiState.isChecklistView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // 5. Lista de tarjetas de recordatorios propuestos
            items(uiState.proposals, key = { it.tempId }) { proposal ->
                if (proposal.isSeparateItem) {
                    // Recordatorio aparte (ej. "Avisarle al encargado · Viernes 09:00" en 03.D)
                    SeparateReminderProposalCard(
                        proposal = proposal,
                        onToggle = { onToggleProposal(proposal.tempId) },
                        onEdit = { onStartEdit(proposal) },
                    )
                } else {
                    ProposedReminderCard(
                        proposal = proposal,
                        onToggle = { onToggleProposal(proposal.tempId) },
                        onToggleChecklistItem = { itemIdx -> onToggleChecklistItem(proposal.tempId, itemIdx) },
                        onEdit = { onStartEdit(proposal) },
                        onUnmerge = { onUnmergeProposal(proposal.tempId) },
                    )
                }
            }

            // 6. Botón "+ Agregar otro"
            item {
                TextButton(
                    onClick = onAddNewProposal,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Agregar otro",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // 7. Barra inferior flotante de acciones: "Unir en uno", "Desunir" y "✓ Crear N"
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Botón "Unir en uno" (si hay al menos 2 recordatorios seleccionados)
                if (uiState.canMerge) {
                    Button(
                        onClick = onMergeAllIntoOne,
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryActionBg),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Unir en uno",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }

                // Botón "Desunir" (si hay alguna propuesta unida o checklist con múltiples tareas seleccionada)
                if (uiState.canUnmerge) {
                    Button(
                        onClick = onUnmergeSelected,
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryActionBg),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Desunir",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }

                // Botón principal "✓ Crear N"
                Button(
                    onClick = onCreateClick,
                    enabled = uiState.selectedCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(
                        // Cuenta los recordatorios que se van a crear (no los ítems de una lista).
                        text = pluralStringResource(R.plurals.review_create_button, uiState.selectedCount, uiState.selectedCount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }

    }
}

/**
 * Card de un recordatorio propuesto con checkbox, título, chips de tiempo/lugar/prioridad y lápiz de edición.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProposedReminderCard(
    proposal: ProposedReminder,
    onToggle: () -> Unit,
    onToggleChecklistItem: (Int) -> Unit,
    onEdit: () -> Unit,
    onUnmerge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (proposal.isSelected) outlineColor else outlineColor.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Fila superior: Checkbox + Título + Desunir + Lápiz
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = proposal.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.size(24.dp),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = proposal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (proposal.isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val canUnmergeThis = !proposal.originalProposals.isNullOrEmpty() ||
                    (proposal.type == ReminderType.Checklist && proposal.items.isNotEmpty())

                if (canUnmergeThis) {
                    IconButton(
                        onClick = onUnmerge,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notes),
                            contentDescription = "Desunir tareas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Si tiene descripción / notas adicionales
            if (!proposal.description.isNullOrBlank() && proposal.description != proposal.title) {
                Text(
                    text = proposal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Si es checklist: lista de tareas
            if (proposal.type == ReminderType.Checklist && proposal.items.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp, top = 2.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${proposal.items.size} tareas agrupadas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Desunir",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onUnmerge() },
                        )
                    }
                    proposal.items.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleChecklistItem(index) },
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (item.isChecked) R.drawable.ic_check_box else R.drawable.ic_check_box_outline_blank,
                                ),
                                contentDescription = null,
                                tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Fila de chips (Hora, Lugar, Prioridad, Lista)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Chip de Hora
                val timeLabel = proposal.triggerDisplayTime ?: "Sin fecha"
                val hasTime = timeLabel != "Sin fecha"
                val timeChipBg = when {
                    !hasTime -> MaterialTheme.colorScheme.surfaceVariant
                    isDark -> Color(0xFF162D45)
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                val timeChipBorder = when {
                    !hasTime -> MaterialTheme.colorScheme.outlineVariant
                    isDark -> Color(0xFF2A5380)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                }
                val timeChipTint = when {
                    !hasTime -> MaterialTheme.colorScheme.onSurfaceVariant
                    isDark -> Color(0xFF90CAF9)
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = timeChipBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, timeChipBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_schedule),
                            contentDescription = null,
                            tint = timeChipTint,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = timeChipTint,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Chip de Lugar
                if (!proposal.placeName.isNullOrBlank()) {
                    val placeChipBg = if (isDark) Color(0xFF2E2211) else Color(0xFFFFF4E0)
                    val placeChipBorder = if (isDark) Color(0xFF664D24) else Color(0xFFD4A72C)
                    val placeChipTint = if (isDark) Color(0xFFFFCC80) else Color(0xFF8A5200)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = placeChipBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, placeChipBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (proposal.placeName.contains("Casa", ignoreCase = true)) R.drawable.ic_home else R.drawable.ic_location_on,
                                ),
                                contentDescription = null,
                                tint = placeChipTint,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = proposal.placeName,
                                style = MaterialTheme.typography.labelSmall,
                                color = placeChipTint,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                // Chip de Prioridad
                if (!proposal.priorityName.isNullOrBlank()) {
                    val priorityChipBg = if (isDark) Color(0xFF331A1A) else MaterialTheme.colorScheme.errorContainer
                    val priorityChipBorder = if (isDark) Color(0xFF662929) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    val priorityChipTint = if (isDark) Color(0xFFFF8A80) else MaterialTheme.colorScheme.onErrorContainer
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityChipBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, priorityChipBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_alarm),
                                contentDescription = null,
                                tint = priorityChipTint,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = proposal.priorityName,
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityChipTint,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                // Chip de Lista
                if (!proposal.listName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_checklist),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "Lista: ${proposal.listName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card para recordatorio sugerido aparte (03.D).
 */
@Composable
private fun SeparateReminderProposalCard(
    proposal: ProposedReminder,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val badgeBg = if (isDark) Color(0xFF2C2415) else Color(0xFFFFF4DC)
    val badgeTint = if (isDark) Color(0xFFFFD54F) else Color(0xFF8A5200)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(badgeBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar_month),
                        contentDescription = null,
                        tint = badgeTint,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Column {
                    Text(
                        text = proposal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Recordatorio aparte · ${proposal.triggerDisplayTime ?: "Con fecha propia"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Switch(
                checked = proposal.isSelected,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}


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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import com.example.uade.rememberapp.ui.theme.CardSurface
import com.example.uade.rememberapp.ui.theme.Outline
import com.example.uade.rememberapp.ui.theme.Primary
import com.example.uade.rememberapp.ui.theme.SurfaceVariant

private val DarkButtonBg = Color(0xFF243642)
private val BadgeGoldBg = Color(0xFF2C2415)
private val BadgeGoldBorder = Color(0xFF6B5120)
private val BadgeGoldText = Color(0xFFFFD54F)

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

    LaunchedEffect(analysisResult) {
        viewModel.initialize(analysisResult)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSuccessSaved()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ReviewProposedRemindersContent(
            uiState = uiState,
            onDismiss = onDismiss,
            onToggleProposal = viewModel::onToggleProposalSelection,
            onToggleChecklistItem = viewModel::onToggleChecklistItem,
            onSetChecklistView = viewModel::onSetChecklistView,
            onAddNewProposal = viewModel::onAddNewProposal,
            onStartEdit = viewModel::onStartEdit,
            onSaveEdit = viewModel::onSaveEdit,
            onDismissEdit = viewModel::onDismissEdit,
            onMergeAllIntoOne = viewModel::onMergeAllIntoOne,
            onCreateClick = viewModel::onCreateClick,
        )

        // Modal 03.C: ¿Los unimos?
        val mergeProposal = uiState.similarMergeProposal
        if (mergeProposal != null) {
            MergeSimilarDialog(
                proposal = mergeProposal,
                onConfirmMerge = viewModel::onConfirmMergeSimilar,
                onKeepSeparate = viewModel::onKeepSeparateSimilar,
                onDismiss = viewModel::onKeepSeparateSimilar,
            )
        }
    }
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
    onSaveEdit: (ProposedReminder) -> Unit,
    onDismissEdit: () -> Unit,
    onMergeAllIntoOne: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        .border(1.dp, Outline, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = CardSurface,
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
                                tint = Primary,
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
                        .border(1.dp, BadgeGoldBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = BadgeGoldBg,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_auto_awesome),
                            contentDescription = null,
                            tint = BadgeGoldText,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = uiState.summaryBadge,
                            style = MaterialTheme.typography.bodySmall,
                            color = BadgeGoldText,
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
                            .background(SurfaceVariant)
                            .padding(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (uiState.isChecklistView) Primary else Color.Transparent)
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
                                    tint = if (uiState.isChecklistView) Color(0xFF0B141B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Checklist",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (uiState.isChecklistView) Color(0xFF0B141B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (!uiState.isChecklistView) Primary else Color.Transparent)
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
                                    tint = if (!uiState.isChecklistView) Color(0xFF0B141B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Separados",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (!uiState.isChecklistView) Color(0xFF0B141B) else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            tint = Primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Agregar otro",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // 7. Barra inferior flotante de acciones: "Unir en uno" y "✓ Crear N"
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Botón "Unir en uno" (solo si hay al menos 2 recordatorios)
                if (uiState.selectedCount >= 2) {
                    Button(
                        onClick = onMergeAllIntoOne,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkButtonBg),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Unir en uno",
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
                        containerColor = Primary,
                        disabledContainerColor = Primary.copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = uiState.createButtonLabel,
                        color = Color(0xFF0B141B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }

        // Diálogo para editar propuesta
        val editing = uiState.editingProposal
        if (editing != null) {
            EditProposalDialog(
                proposal = editing,
                onDismiss = onDismissEdit,
                onSave = onSaveEdit,
            )
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
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (proposal.isSelected) Outline else Outline.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Fila superior: Checkbox + Título + Lápiz
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = proposal.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Primary,
                        checkmarkColor = Color(0xFF0B141B),
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

            // Si es checklist: lista de tareas
            if (proposal.type == ReminderType.Checklist && proposal.items.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp, top = 2.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
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
                                tint = if (item.isChecked) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasTime) Color(0xFF162D45) else SurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasTime) Color(0xFF2A5380) else Outline.copy(alpha = 0.5f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_schedule),
                            contentDescription = null,
                            tint = if (hasTime) Color(0xFF90CAF9) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasTime) Color(0xFF90CAF9) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Chip de Lugar
                if (!proposal.placeName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2E2211),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF664D24)),
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
                                tint = Color(0xFFFFCC80),
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = proposal.placeName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFCC80),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                // Chip de Prioridad
                if (!proposal.priorityName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF331A1A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF662929)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_alarm),
                                contentDescription = null,
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = proposal.priorityName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF8A80),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                // Chip de Lista
                if (!proposal.listName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Outline.copy(alpha = 0.5f)),
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Outline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
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
                        .background(Color(0xFF2C2415), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar_month),
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
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
                    checkedThumbColor = Primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

/**
 * Diálogo para editar título, fecha inferida y lugar de una propuesta.
 */
@Composable
private fun EditProposalDialog(
    proposal: ProposedReminder,
    onDismiss: () -> Unit,
    onSave: (ProposedReminder) -> Unit,
) {
    var title by remember { mutableStateOf(proposal.title) }
    var time by remember { mutableStateOf(proposal.triggerDisplayTime ?: "") }
    var place by remember { mutableStateOf(proposal.placeName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar recordatorio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Fecha / Horario (ej. Mañana 09:00)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text("Lugar (ej. Casa, Súper)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        proposal.copy(
                            title = title.ifBlank { proposal.title },
                            triggerDisplayTime = time.ifBlank { null },
                            placeName = place.ifBlank { null },
                        ),
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text(
                    text = "Guardar",
                    color = Color(0xFF0B141B),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

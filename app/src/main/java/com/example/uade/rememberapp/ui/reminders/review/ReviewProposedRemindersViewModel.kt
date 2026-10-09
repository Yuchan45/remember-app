package com.example.uade.rememberapp.ui.reminders.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.model.AiProcessingStatus
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.usecase.DetectSimilarRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.MergeProposedRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.ObserveRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * Controla el estado y las acciones del flujo de revisión de recordatorios propuestos por la IA.
 * Implementa "Split automático" (03.B), "Unir en uno", "¿Los unimos?" (03.C) y "Checklist" (03.D).
 */
class ReviewProposedRemindersViewModel(
    private val saveReminder: SaveReminderUseCase,
    private val mergeProposedReminders: MergeProposedRemindersUseCase,
    private val detectSimilarReminders: DetectSimilarRemindersUseCase,
    private val observeReminders: ObserveRemindersUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewProposedRemindersUiState())
    val uiState: StateFlow<ReviewProposedRemindersUiState> = _uiState.asStateFlow()

    fun initialize(result: AiAnalysisResult) {
        _uiState.update {
            it.copy(
                rawTranscript = result.rawTranscript,
                summaryBadge = result.summaryBadge,
                proposals = result.proposedReminders,
                isChecklistCandidate = result.isChecklistCandidate,
                isChecklistView = true,
                isSaved = false,
                similarMergeProposal = null,
                editingProposal = null,
            )
        }
    }

    fun onToggleProposalSelection(tempId: String) {
        _uiState.update { state ->
            val updated = state.proposals.map { p ->
                if (p.tempId == tempId) p.copy(isSelected = !p.isSelected) else p
            }
            state.copy(proposals = updated)
        }
    }

    fun onToggleChecklistItem(proposalId: String, itemIndex: Int) {
        _uiState.update { state ->
            val updated = state.proposals.map { p ->
                if (p.tempId == proposalId) {
                    val updatedItems = p.items.mapIndexed { index, item ->
                        if (index == itemIndex) item.copy(isChecked = !item.isChecked) else item
                    }
                    p.copy(items = updatedItems)
                } else {
                    p
                }
            }
            state.copy(proposals = updated)
        }
    }

    fun onSetChecklistView(isChecklist: Boolean) {
        _uiState.update { it.copy(isChecklistView = isChecklist) }
    }

    fun onMergeAllIntoOne() {
        val currentProposals = _uiState.value.proposals.filter { it.isSelected }
        if (currentProposals.size <= 1) return

        val merged = mergeProposedReminders(currentProposals)
        val unselected = _uiState.value.proposals.filter { !it.isSelected }

        _uiState.update { state ->
            state.copy(
                proposals = listOf(merged) + unselected,
                summaryBadge = "Recordatorios unidos en una única checklist.",
                isChecklistCandidate = true,
                isChecklistView = true,
            )
        }
    }

    fun onAddNewProposal() {
        val newProposal = ProposedReminder(
            tempId = UUID.randomUUID().toString(),
            title = "Nuevo recordatorio",
            type = ReminderType.Note,
            triggerDisplayTime = "Sin fecha",
            isSelected = true,
        )
        _uiState.update { it.copy(proposals = it.proposals + newProposal) }
        onStartEdit(newProposal)
    }

    fun onStartEdit(proposal: ProposedReminder) {
        _uiState.update { it.copy(editingProposal = proposal) }
    }

    fun onSaveEdit(updatedProposal: ProposedReminder) {
        _uiState.update { state ->
            val updated = state.proposals.map { p ->
                if (p.tempId == updatedProposal.tempId) updatedProposal else p
            }
            state.copy(proposals = updated, editingProposal = null)
        }
    }

    fun onDismissEdit() {
        _uiState.update { it.copy(editingProposal = null) }
    }

    fun onCreateClick() {
        val activeProposals = _uiState.value.proposals.filter { it.isSelected }
        if (activeProposals.isEmpty()) return

        viewModelScope.launch {
            val existingReminders = observeReminders().first()

            // Verificamos si alguna de las propuestas activa la sugerencia de merge (03.C)
            var foundMergeProposal: com.example.uade.rememberapp.domain.model.SimilarReminderMergeProposal? = null
            for (proposal in activeProposals) {
                val merge = detectSimilarReminders(proposal, existingReminders)
                if (merge != null) {
                    foundMergeProposal = merge
                    break
                }
            }

            if (foundMergeProposal != null) {
                _uiState.update { it.copy(similarMergeProposal = foundMergeProposal) }
            } else {
                saveAllProposals(activeProposals)
            }
        }
    }

    fun onConfirmMergeSimilar() {
        val proposal = _uiState.value.similarMergeProposal ?: return
        viewModelScope.launch {
            // Actualizamos el existente en Room con los items fusionados
            val updatedExisting = proposal.existingReminder.copy(
                type = ReminderType.Checklist,
                title = proposal.mergedTitle,
                items = proposal.checklistItems,
                trigger = proposal.mergedTrigger,
                aiStatus = AiProcessingStatus.Completed,
            )
            saveReminder(updatedExisting)

            // Guardamos el resto de las propuestas seleccionadas que no sean la fusionada
            val remaining = _uiState.value.proposals
                .filter { it.isSelected && it.tempId != proposal.newReminder.tempId }
            saveAllProposals(remaining)

            _uiState.update { it.copy(similarMergeProposal = null, isSaved = true) }
        }
    }

    fun onKeepSeparateSimilar() {
        val activeProposals = _uiState.value.proposals.filter { it.isSelected }
        viewModelScope.launch {
            _uiState.update { it.copy(similarMergeProposal = null) }
            saveAllProposals(activeProposals)
        }
    }

    private suspend fun saveAllProposals(proposals: List<ProposedReminder>) {
        for (p in proposals) {
            val reminder = Reminder(
                type = p.type,
                title = p.title,
                description = p.description,
                items = p.items,
                trigger = p.trigger,
                aiStatus = AiProcessingStatus.Completed,
                createdAt = Instant.now(),
            )
            saveReminder(reminder)
        }
        _uiState.update { it.copy(isSaved = true) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                ReviewProposedRemindersViewModel(
                    saveReminder = app.container.saveReminderUseCase,
                    mergeProposedReminders = app.container.mergeProposedRemindersUseCase,
                    detectSimilarReminders = app.container.detectSimilarRemindersUseCase,
                    observeReminders = app.container.observeRemindersUseCase,
                )
            }
        }
    }
}

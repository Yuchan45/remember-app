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
import com.example.uade.rememberapp.domain.usecase.SplitProposedReminderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.Importance
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Controla el estado y las acciones del flujo de revisión de recordatorios propuestos por la IA.
 * Implementa "Split automático" (03.B), "Unir en uno", "Desunir", "¿Los unimos?" (03.C) y "Checklist" (03.D).
 */
class ReviewProposedRemindersViewModel(
    private val saveReminder: SaveReminderUseCase,
    private val mergeProposedReminders: MergeProposedRemindersUseCase,
    private val splitProposedReminder: SplitProposedReminderUseCase,
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
        if (isChecklist == _uiState.value.isChecklistView) return
        if (isChecklist) {
            onMergeAllIntoOne()
        } else {
            onUnmergeSelected()
        }
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

    fun onUnmergeSelected() {
        val currentProposals = _uiState.value.proposals
        val selectedToUnmerge = currentProposals.filter {
            it.isSelected && (!it.originalProposals.isNullOrEmpty() || (it.type == ReminderType.Checklist && it.items.isNotEmpty()))
        }
        if (selectedToUnmerge.isEmpty()) return

        val newProposals = mutableListOf<ProposedReminder>()
        for (p in currentProposals) {
            if (p in selectedToUnmerge) {
                newProposals.addAll(splitProposedReminder(p))
            } else {
                newProposals.add(p)
            }
        }

        _uiState.update { state ->
            state.copy(
                proposals = newProposals,
                summaryBadge = "Tareas desunidas en recordatorios individuales.",
                isChecklistView = false,
            )
        }
    }

    fun onUnmergeProposal(tempId: String) {
        val currentProposals = _uiState.value.proposals
        val target = currentProposals.firstOrNull { it.tempId == tempId } ?: return
        val unmerged = splitProposedReminder(target)

        val newProposals = mutableListOf<ProposedReminder>()
        for (p in currentProposals) {
            if (p.tempId == tempId) {
                newProposals.addAll(unmerged)
            } else {
                newProposals.add(p)
            }
        }

        _uiState.update { state ->
            state.copy(
                proposals = newProposals,
                summaryBadge = "Recordatorio desunido en ${unmerged.size} tareas individuales.",
                isChecklistView = false,
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
                alarms = proposal.mergedAlarms,
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
            val importance = when (p.priorityName?.lowercase()) {
                "alta" -> Importance.High
                "urgente", "crítica" -> Importance.Critical
                "baja" -> Importance.Low
                else -> Importance.Default
            }

            val alarms = if (p.alarms.isNotEmpty()) {
                p.alarms
            } else {
                parseDisplayTimeToAlarm(p.triggerDisplayTime)
            }

            val reminder = Reminder(
                type = p.type,
                title = p.title,
                description = p.description,
                items = p.items,
                alarms = alarms,
                importance = importance,
                aiStatus = AiProcessingStatus.Completed,
                createdAt = Instant.now(),
            )
            saveReminder(reminder)
        }
        _uiState.update { it.copy(isSaved = true) }
    }

    private fun parseDisplayTimeToAlarm(displayTime: String?): List<Alarm> {
        if (displayTime.isNullOrBlank() || displayTime.equals("Sin fecha", ignoreCase = true)) {
            return emptyList()
        }
        val lower = displayTime.lowercase()
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)

        return try {
            val targetDateTime: ZonedDateTime? = when {
                lower.contains("hoy") -> {
                    val timePart = Regex("""(\d{1,2}):(\d{2})""").find(displayTime)?.value ?: "21:00"
                    val (h, m) = timePart.split(":").map { it.toInt() }
                    now.withHour(h).withMinute(m).withSecond(0).withNano(0)
                }
                lower.contains("mañana") -> {
                    val timePart = Regex("""(\d{1,2}):(\d{2})""").find(displayTime)?.value ?: "09:00"
                    val (h, m) = timePart.split(":").map { it.toInt() }
                    now.plusDays(1).withHour(h).withMinute(m).withSecond(0).withNano(0)
                }
                lower.contains("viernes") -> {
                    val daysUntilFriday = ((DayOfWeek.FRIDAY.value - now.dayOfWeek.value + 7) % 7).let { if (it == 0) 7 else it }
                    now.plusDays(daysUntilFriday.toLong()).withHour(9).withMinute(0).withSecond(0).withNano(0)
                }
                lower.contains("lunes") -> {
                    val daysUntilMonday = ((DayOfWeek.MONDAY.value - now.dayOfWeek.value + 7) % 7).let { if (it == 0) 7 else it }
                    now.plusDays(daysUntilMonday.toLong()).withHour(9).withMinute(0).withSecond(0).withNano(0)
                }
                else -> null
            }
            if (targetDateTime != null) {
                listOf(Alarm(targetDateTime.toInstant()))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                ReviewProposedRemindersViewModel(
                    saveReminder = app.container.saveReminderUseCase,
                    mergeProposedReminders = app.container.mergeProposedRemindersUseCase,
                    splitProposedReminder = app.container.splitProposedReminderUseCase,
                    detectSimilarReminders = app.container.detectSimilarRemindersUseCase,
                    observeReminders = app.container.observeRemindersUseCase,
                )
            }
        }
    }
}

package com.example.uade.rememberapp.ui.reminders.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.model.AiProcessingStatus
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.usecase.MergeProposedRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SplitProposedReminderUseCase
import com.example.uade.rememberapp.ui.reminders.components.formatReminderTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Controla el estado y las acciones del flujo de revisión de recordatorios propuestos por la IA.
 * Implementa "Split automático" (03.B), "Unir en uno", "Desunir", "¿Los unimos?" (03.C) y "Checklist" (03.D).
 */
class ReviewProposedRemindersViewModel(
    private val saveReminder: SaveReminderUseCase,
    private val mergeProposedReminders: MergeProposedRemindersUseCase,
    private val splitProposedReminder: SplitProposedReminderUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewProposedRemindersUiState())
    val uiState: StateFlow<ReviewProposedRemindersUiState> = _uiState.asStateFlow()

    /** El análisis con el que ya se cargó la pantalla (ver [initialize]). */
    private var loadedResult: AiAnalysisResult? = null

    /**
     * Carga las propuestas del análisis, una sola vez por análisis. La pantalla lo llama cada vez
     * que se vuelve a mostrar (ej. al volver a la app después de minimizarla): sin este control,
     * todo volvía a como lo armó la IA y se perdía lo que el usuario había cambiado.
     */
    fun initialize(result: AiAnalysisResult) {
        if (loadedResult == result) return
        loadedResult = result
        _uiState.update {
            it.copy(
                rawTranscript = result.rawTranscript,
                summaryBadge = result.summaryBadge,
                proposals = result.proposedReminders,
                isChecklistCandidate = result.isChecklistCandidate,
                isChecklistView = true,
                isSaved = false,
                editingProposal = null,
                groupedSnapshot = null,
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

    /**
     * El selector "Checklist / Separados". Tiene que poder ir y volver sin perder nada:
     * - "Separados" separa las listas en un recordatorio por ítem, y antes guarda cómo estaban
     *   agrupadas ([ReviewProposedRemindersUiState.groupedSnapshot]).
     * - "Checklist" vuelve a esa agrupación tal cual. Antes unía TODO en una sola lista, así que de
     *   dos listas quedaba una (parecía que una había desaparecido).
     *
     * Lo que se edite estando en "Separados" no pasa a la vista "Checklist": se vuelve a como
     * estaba antes de separar.
     */
    fun onSetChecklistView(isChecklist: Boolean) {
        val state = _uiState.value
        if (isChecklist == state.isChecklistView) return
        if (isChecklist) {
            _uiState.update {
                it.copy(
                    proposals = it.groupedSnapshot ?: it.proposals,
                    groupedSnapshot = null,
                    isChecklistView = true,
                    summaryBadge = "Tareas agrupadas como las armó la IA.",
                )
            }
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
                // "Unir en uno" es una agrupación nueva elegida por el usuario: pasa a ser la base.
                groupedSnapshot = null,
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
                // La agrupación de antes de separar, para que "Checklist" la devuelva tal cual. Si
                // ya había una guardada (se separó en varios pasos), se queda la primera.
                groupedSnapshot = state.groupedSnapshot ?: currentProposals,
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
                groupedSnapshot = state.groupedSnapshot ?: currentProposals,
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

    /**
     * El recordatorio que se abre en el editor de detalle para [proposal]: el que ya se editó
     * antes, o uno armado con lo que propuso la IA (con la nota de voz y su transcripción).
     */
    fun draftFor(proposal: ProposedReminder): Reminder =
        proposal.draft ?: proposal.toReminder()

    /**
     * Volvió el editor con [edited]: la propuesta guarda el borrador completo (para crearlo tal
     * cual) y actualiza lo que muestra su tarjeta.
     */
    fun onDraftEdited(tempId: String, edited: Reminder) {
        _uiState.update { state ->
            val updated = state.proposals.map { p ->
                if (p.tempId != tempId) return@map p
                p.copy(
                    draft = edited,
                    title = edited.title.orEmpty().ifBlank { p.title },
                    description = edited.description,
                    type = edited.type,
                    items = edited.items,
                    alarms = edited.alarms,
                    triggerDisplayTime = edited.alarms.minByOrNull { it.at }
                        ?.let { formatReminderTime(it.at, Instant.now(), Locale.forLanguageTag("es-AR")) },
                    priorityName = when (edited.importance) {
                        Importance.Low -> "Baja"
                        Importance.Default -> "Normal"
                        Importance.High -> "Alta"
                        Importance.Critical -> "Crítica"
                    },
                )
            }
            state.copy(proposals = updated, editingProposal = null)
        }
    }

    /**
     * La propuesta como recordatorio, con lo que la IA pudo deducir. La grabación y lo que se
     * transcribió van en todos los que salen del mismo audio, para poder volver a escucharlo.
     */
    private fun ProposedReminder.toReminder(): Reminder {
        val importance = when (priorityName?.lowercase()) {
            "alta" -> Importance.High
            "urgente", "crítica" -> Importance.Critical
            "baja" -> Importance.Low
            else -> Importance.Default
        }
        return Reminder(
            type = type,
            // Un Reminder necesita algo de contenido: si la IA dejó el título vacío, se pone uno.
            title = title.ifBlank { "Recordatorio" },
            description = description,
            items = items,
            alarms = alarms.ifEmpty { parseDisplayTimeToAlarm(triggerDisplayTime) },
            importance = importance,
            audioPath = loadedResult?.audioPath,
            transcript = _uiState.value.rawTranscript.ifBlank { null },
            aiStatus = AiProcessingStatus.Completed,
            createdAt = Instant.now(),
        )
    }

    fun onDismissEdit() {
        _uiState.update { it.copy(editingProposal = null) }
    }

    /**
     * "Crear": guarda las propuestas marcadas. Ya no busca recordatorios parecidos entre los que
     * el usuario tenía de antes para ofrecer unirlos: mezclaba lo recién dictado con cosas de otras
     * sesiones. "Unir todo" entre las propuestas de este mismo audio sigue disponible.
     */
    fun onCreateClick() {
        val activeProposals = _uiState.value.proposals.filter { it.isSelected }
        if (activeProposals.isEmpty()) return
        viewModelScope.launch { saveAllProposals(activeProposals) }
    }

    private suspend fun saveAllProposals(proposals: List<ProposedReminder>) {
        for (p in proposals) {
            // Si se editó en el editor de detalle, se crea tal cual quedó; si no, con lo de la IA.
            saveReminder(p.draft ?: p.toReminder())
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
                )
            }
        }
    }
}

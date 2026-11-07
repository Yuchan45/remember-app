package com.example.uade.rememberapp.ui.reminders.review

import com.example.uade.rememberapp.domain.model.ProposedReminder

/**
 * Estado de la pantalla de revisión de recordatorios propuestos por la IA (mockup 03.B y 03.D).
 */
data class ReviewProposedRemindersUiState(
    val rawTranscript: String = "",
    val summaryBadge: String = "",
    val proposals: List<ProposedReminder> = emptyList(),
    val isChecklistCandidate: Boolean = false,
    val isChecklistView: Boolean = true, // Toggle entre vista Checklist y Separados (03.D)
    val editingProposal: ProposedReminder? = null,
    val isSaved: Boolean = false,
    /**
     * Cómo estaban agrupadas las propuestas antes de pasar a "Separados", para que volver a
     * "Checklist" las deje igual. null si no se separó nada (o si se volvió a agrupar).
     */
    val groupedSnapshot: List<ProposedReminder>? = null,
) {
    val selectedCount: Int
        get() = proposals.count { it.isSelected }

    val canMerge: Boolean
        get() = proposals.count { it.isSelected } >= 2

    val canUnmerge: Boolean
        get() = proposals.any {
            it.isSelected && (!it.originalProposals.isNullOrEmpty() ||
                (it.type == com.example.uade.rememberapp.domain.model.ReminderType.Checklist && it.items.isNotEmpty()))
        }
}

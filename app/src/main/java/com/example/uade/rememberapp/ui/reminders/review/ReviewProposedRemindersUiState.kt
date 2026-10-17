package com.example.uade.rememberapp.ui.reminders.review

import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.SimilarReminderMergeProposal

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
    val similarMergeProposal: SimilarReminderMergeProposal? = null, // Modal 03.C ¿Los unimos?
    val isSaved: Boolean = false,
) {
    val selectedCount: Int
        get() = proposals.count { it.isSelected }

    val createButtonLabel: String
        get() {
            return if (isChecklistCandidate && isChecklistView) {
                val checklist = proposals.firstOrNull { it.type == com.example.uade.rememberapp.domain.model.ReminderType.Checklist }
                val count = checklist?.items?.size ?: 0
                val separateCount = proposals.count { it.isSeparateItem && it.isSelected }
                if (separateCount > 0) {
                    "✓ Crear checklist ($count) + $separateCount"
                } else {
                    "✓ Crear checklist ($count)"
                }
            } else {
                "✓ Crear $selectedCount"
            }
        }
}

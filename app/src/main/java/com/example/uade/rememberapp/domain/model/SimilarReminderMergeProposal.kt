package com.example.uade.rememberapp.domain.model

/**
 * Propuesta de unión (merge) entre un recordatorio existente en Room y un nuevo recordatorio
 * que comparten una franja horaria similar (ej. "Esta noche").
 */
data class SimilarReminderMergeProposal(
    val existingReminder: Reminder,
    val newReminder: ProposedReminder,
    val mergedTitle: String,
    val mergedTrigger: Trigger,
    val checklistItems: List<ChecklistItem>,
)

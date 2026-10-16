package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.SimilarReminderMergeProposal
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Detecta si una nueva propuesta coincide en fecha o franja horaria con un recordatorio activo
 * existente en la base de datos, para sugerir unirlos como checklist (mockup 03.C).
 */
class DetectSimilarRemindersUseCase {

    operator fun invoke(
        newProposal: ProposedReminder,
        existingReminders: List<Reminder>,
    ): SimilarReminderMergeProposal? {
        val newAlarm = newProposal.alarms.firstOrNull() ?: return null

        val match = existingReminders.firstOrNull { existing ->
            existing.status == ReminderStatus.Active && !existing.isDone &&
                existing.alarms.any { abs(ChronoUnit.HOURS.between(it.at, newAlarm.at)) <= 3 }
        } ?: return null

        val existingItems = if (match.items.isNotEmpty()) {
            match.items
        } else {
            listOf(ChecklistItem(text = match.title ?: "Recordatorio existente"))
        }

        val newItems = if (newProposal.items.isNotEmpty()) {
            newProposal.items
        } else {
            listOf(ChecklistItem(text = newProposal.title))
        }

        val combinedItems = existingItems + newItems
        val mergedTitle = "Checklist combinada (${combinedItems.size})"

        return SimilarReminderMergeProposal(
            existingReminder = match,
            newReminder = newProposal,
            mergedTitle = mergedTitle,
            mergedAlarms = match.alarms,
            checklistItems = combinedItems,
        )
    }
}

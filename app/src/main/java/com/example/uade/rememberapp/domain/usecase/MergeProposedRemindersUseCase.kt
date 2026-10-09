package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.Trigger
import java.util.UUID

/**
 * Une varias propuestas de recordatorio en una única propuesta de tipo Checklist.
 * Se utiliza para la acción "Unir en uno" del mockup 03.B y 03.D.
 */
class MergeProposedRemindersUseCase {

    operator fun invoke(proposals: List<ProposedReminder>): ProposedReminder {
        require(proposals.isNotEmpty()) { "Debe haber al menos una propuesta para unir" }

        val activeProposals = proposals.filter { it.isSelected }.ifEmpty { proposals }

        val firstWithTrigger = activeProposals.firstOrNull { it.trigger !is Trigger.None }
        val trigger = firstWithTrigger?.trigger ?: Trigger.None
        val displayTime = firstWithTrigger?.triggerDisplayTime

        val firstWithPlace = activeProposals.firstOrNull { !it.placeName.isNullOrBlank() }
        val placeName = firstWithPlace?.placeName

        val items = activeProposals.flatMap { p ->
            if (p.type == ReminderType.Checklist && p.items.isNotEmpty()) {
                p.items
            } else {
                listOf(ChecklistItem(text = p.title, note = p.description))
            }
        }

        val primaryListName = activeProposals.firstOrNull { !it.listName.isNullOrBlank() }?.listName
        val mergedTitle = if (!primaryListName.isNullOrBlank()) {
            "Lista: $primaryListName"
        } else {
            "Checklist de recordatorios"
        }

        return ProposedReminder(
            tempId = UUID.randomUUID().toString(),
            title = mergedTitle,
            type = ReminderType.Checklist,
            items = items,
            trigger = trigger,
            triggerDisplayTime = displayTime,
            placeName = placeName,
            priorityName = activeProposals.firstOrNull { !it.priorityName.isNullOrBlank() }?.priorityName,
            listName = primaryListName,
            isSelected = true,
        )
    }
}

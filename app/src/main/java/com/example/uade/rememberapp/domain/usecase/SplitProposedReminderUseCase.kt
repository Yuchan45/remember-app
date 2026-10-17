package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.ReminderType
import java.util.UUID

/**
 * Caso de uso para desunir o separar una propuesta de recordatorio (que haya sido unida
 * previamente o que sea una checklist) en recordatorios individuales de tipo Note.
 */
class SplitProposedReminderUseCase {

    operator fun invoke(proposal: ProposedReminder): List<ProposedReminder> {
        // 1. Si la propuesta proviene de un merge previo y tiene guardadas las propuestas originales:
        if (!proposal.originalProposals.isNullOrEmpty()) {
            return proposal.originalProposals.map { it.copy(isSelected = true) }
        }

        // 2. Si es una checklist con items: cada item se convierte en una propuesta independiente
        if (proposal.type == ReminderType.Checklist && proposal.items.isNotEmpty()) {
            return proposal.items.map { item ->
                ProposedReminder(
                    tempId = UUID.randomUUID().toString(),
                    title = item.text,
                    description = item.note ?: proposal.description,
                    type = ReminderType.Note,
                    items = emptyList(),
                    alarms = proposal.alarms,
                    triggerDisplayTime = proposal.triggerDisplayTime,
                    placeName = proposal.placeName,
                    priorityName = proposal.priorityName,
                    listName = proposal.listName,
                    isSelected = true,
                )
            }
        }

        // 3. Si no tiene ítems de lista ni propuestas originales previas, se retorna a sí misma
        return listOf(proposal)
    }
}

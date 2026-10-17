package com.example.uade.rememberapp.domain.model

import java.util.UUID

/**
 * Propuesta de recordatorio generada por el análisis de IA (Groq).
 * El usuario puede revisarla, tildarla, destildarla, editarla o unirla antes de persistirla en Room.
 */
data class ProposedReminder(
    val tempId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val type: ReminderType = ReminderType.Note,
    val items: List<ChecklistItem> = emptyList(),
    val alarms: List<Alarm> = emptyList(),
    val triggerDisplayTime: String? = null,
    val placeName: String? = null,
    val priorityName: String? = null,
    val listName: String? = null,
    val isSelected: Boolean = true,
    val isSeparateItem: Boolean = false,
    val originalProposals: List<ProposedReminder>? = null,
)

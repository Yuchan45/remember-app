package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.repository.ReminderRepository

/** Un recordatorio por su id, o null si no existe (ej. se borró mientras tanto). */
class GetReminderUseCase(
    private val reminders: ReminderRepository,
) {
    suspend operator fun invoke(id: Long): Reminder? = reminders.getById(id)
}

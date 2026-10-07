package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler

/**
 * Marca un recordatorio como completado (isDone = true) y cancela su alarma en el sistema.
 */
class MarkReminderDoneUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(reminderId: Long) {
        val reminder = reminders.getById(reminderId) ?: return
        reminders.save(reminder.copy(isDone = true))
        scheduler.cancel(reminderId)
    }
}

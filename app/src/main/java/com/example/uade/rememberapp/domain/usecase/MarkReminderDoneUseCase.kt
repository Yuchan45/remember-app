package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import com.example.uade.rememberapp.domain.scheduler.cancelAll
import java.time.Instant

/**
 * Marca un recordatorio como completado (isDone = true) y cancela todas sus alarmas en el sistema.
 * Desde ese momento ([com.example.uade.rememberapp.domain.model.Reminder.statusChangedAt]) corre
 * el plazo para que se borre solo del Historial.
 */
class MarkReminderDoneUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val now: () -> Instant = Instant::now,
) {
    suspend operator fun invoke(reminderId: Long) {
        val reminder = reminders.getById(reminderId) ?: return
        reminders.save(reminder.copy(isDone = true, statusChangedAt = now()))
        scheduler.cancelAll(reminderId, reminder.alarms)
    }
}

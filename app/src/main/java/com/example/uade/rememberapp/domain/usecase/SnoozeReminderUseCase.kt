package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Trigger
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Pospone el aviso de un recordatorio sumándole [minutes] (por defecto 15 minutos),
 * actualiza el repositorio y reprograma la alarma.
 */
class SnoozeReminderUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(reminderId: Long, minutes: Long = 15) {
        val reminder = reminders.getById(reminderId) ?: return
        val newTime = Instant.now().plus(minutes, ChronoUnit.MINUTES)
        val updated = reminder.copy(trigger = Trigger.AtTime(newTime))
        reminders.save(updated)
        scheduler.schedule(reminderId, newTime)
    }
}

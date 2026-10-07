package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.Trigger
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Reprograma en [ReminderScheduler] todas las alarmas de recordatorios activos y no completados
 * cuya fecha de aviso sea futura. Se usa principalmente cuando el dispositivo se reinicia.
 */
class RescheduleRemindersUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke() {
        val allReminders = reminders.observeAll().first()
        val now = Instant.now()
        allReminders
            .filter { it.status == ReminderStatus.Active && !it.isDone }
            .forEach { reminder ->
                val trigger = reminder.trigger
                if (trigger is Trigger.AtTime && trigger.at.isAfter(now)) {
                    scheduler.schedule(reminder.id, trigger.at)
                }
            }
    }
}

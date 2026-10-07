package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.Trigger
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler

/**
 * Guarda un recordatorio (nuevo o editado) y devuelve su id.
 *
 * Si tiene aviso por hora ([Trigger.AtTime]), programa la alarma en el sistema operativo.
 * Si el recordatorio se desactiva, se completa o se le quita la hora, cancela la alarma previa.
 */
class SaveReminderUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(reminder: Reminder): Long {
        val id = reminders.save(reminder)
        val finalReminder = if (reminder.id != 0L) reminder else reminder.copy(id = id)

        when (val trigger = finalReminder.trigger) {
            is Trigger.AtTime -> {
                if (finalReminder.status == ReminderStatus.Active && !finalReminder.isDone) {
                    scheduler.schedule(finalReminder.id, trigger.at)
                } else {
                    scheduler.cancel(finalReminder.id)
                }
            }
            else -> {
                // Si ya no es por hora (ej. se quitó la hora o pasó a Trigger.None / AtPlace), cancelamos la alarma previa
                scheduler.cancel(finalReminder.id)
            }
        }

        return id
    }
}

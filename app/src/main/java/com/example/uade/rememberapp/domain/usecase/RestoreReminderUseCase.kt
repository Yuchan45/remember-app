package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.repository.ReminderRepository

/**
 * Devuelve un recordatorio a la lista principal ([ReminderStatus.Active]): deshace mandarlo a la
 * papelera o archivarlo.
 *
 * TODO: volver a programar su alarma o geofence, que se cancelaron al sacarlo.
 */
class RestoreReminderUseCase(
    private val reminders: ReminderRepository,
) {
    suspend operator fun invoke(id: Long) {
        val reminder = reminders.getById(id) ?: return
        reminders.save(reminder.copy(status = ReminderStatus.Active))
    }
}

package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.repository.ReminderRepository

/**
 * Devuelve un recordatorio a la lista principal ([ReminderStatus.Active], pendiente): deshace
 * mandarlo a la papelera, archivarlo o marcarlo como hecho.
 *
 * Guarda con [SaveReminderUseCase], que vuelve a programar sus alarmas y geofences (se habían
 * cancelado al sacarlo de la Home). Los avisos que ya pasaron no suenan: el scheduler los ignora.
 */
class RestoreReminderUseCase(
    private val reminders: ReminderRepository,
    private val saveReminder: SaveReminderUseCase,
) {
    suspend operator fun invoke(id: Long) {
        val reminder = reminders.getById(id) ?: return
        saveReminder(reminder.copy(status = ReminderStatus.Active, isDone = false, statusChangedAt = null))
    }
}

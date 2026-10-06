package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.repository.ReminderRepository

/**
 * Manda un recordatorio a la papelera: no lo borra de la base, le cambia el estado a
 * [ReminderStatus.Deleted]. Así deja de aparecer en la Home (que solo muestra los activos) pero
 * se puede recuperar desde la sección Archivo.
 *
 * TODO: cancelar su alarma o geofence con ReminderScheduler / GeofenceRegistrar.
 * TODO: borrarlo definitivamente pasado un tiempo (ver ReminderStatus.Deleted).
 */
class TrashReminderUseCase(
    private val reminders: ReminderRepository,
) {
    suspend operator fun invoke(id: Long) {
        val reminder = reminders.getById(id) ?: return
        reminders.save(reminder.copy(status = ReminderStatus.Deleted))
    }
}

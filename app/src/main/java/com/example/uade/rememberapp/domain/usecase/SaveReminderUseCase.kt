package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.GeofenceRegistrar
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import com.example.uade.rememberapp.domain.scheduler.cancelAll

/**
 * Guarda un recordatorio (nuevo o editado) y devuelve su id.
 *
 * Además deja las alarmas del sistema iguales a sus avisos por hora: cancela las que tenía antes
 * de guardar (pudieron cambiar o borrarse) y programa las actuales. Si el recordatorio no está
 * activo o ya se completó, no programa ninguna.
 * También asegura el registro de geofences para los avisos por lugar activos.
 */
class SaveReminderUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val geofenceRegistrar: GeofenceRegistrar? = null,
) {
    suspend operator fun invoke(reminder: Reminder): Long {
        // Las de antes, para cancelarlas: AlarmManager no permite preguntar cuáles hay.
        val previous = if (reminder.id != 0L) reminders.getById(reminder.id) else null
        val id = reminders.save(reminder)

        previous?.let { scheduler.cancelAll(id, it.alarms) }
        if (reminder.status == ReminderStatus.Active && !reminder.isDone) {
            reminder.alarms.forEach { scheduler.schedule(id, it.at) }
            geofenceRegistrar?.let { registrar ->
                reminder.places.forEach { registrar.ensureRegistered(it.placeId) }
            }
        }
        return id
    }
}

package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.rolledForward
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.GeofenceRegistrar
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import com.example.uade.rememberapp.domain.scheduler.cancelAll
import java.time.Instant
import java.time.ZoneId

/**
 * Guarda un recordatorio (nuevo o editado) y devuelve su id.
 *
 * Además deja las alarmas del sistema iguales a sus avisos por hora: cancela las que tenía antes
 * de guardar (pudieron cambiar o borrarse) y programa las actuales. Si el recordatorio no está
 * activo o ya se completó, no programa ninguna.
 * También asegura el registro de geofences para los avisos por lugar activos.
 *
 * Los avisos que se repiten y ya pasaron se corren a su próxima vez antes de guardar (ej. al
 * restaurar uno de la papelera días después): si no, quedarían en el pasado y no sonarían más.
 *
 * [now] y [zone] se reciben por constructor para que los tests puedan fijar la hora.
 */
class SaveReminderUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val geofenceRegistrar: GeofenceRegistrar? = null,
    private val now: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    suspend operator fun invoke(reminder: Reminder): Long {
        // Las de antes, para cancelarlas: AlarmManager no permite preguntar cuáles hay.
        val previous = if (reminder.id != 0L) reminders.getById(reminder.id) else null
        val toSave = reminder.copy(alarms = reminder.alarms.rolledForward(now(), zone))
        val id = reminders.save(toSave)

        previous?.let { scheduler.cancelAll(id, it.alarms) }
        if (toSave.status == ReminderStatus.Active && !toSave.isDone) {
            toSave.alarms.forEach { scheduler.schedule(id, it.at) }
            geofenceRegistrar?.let { registrar ->
                toSave.places.forEach { registrar.ensureRegistered(it.placeId) }
            }
        }
        return id
    }
}

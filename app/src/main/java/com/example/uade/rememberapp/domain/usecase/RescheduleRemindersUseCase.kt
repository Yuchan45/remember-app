package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.GeofenceRegistrar
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Reprograma en [ReminderScheduler] y [GeofenceRegistrar] todos los avisos futuros y geocercas
 * de los recordatorios activos y no completados. Se usa principalmente cuando el dispositivo se
 * reinicia (el sistema borra las alarmas y geofences al apagarse).
 *
 * [now] es el reloj: se recibe por constructor para que los tests puedan fijar la hora.
 */
class RescheduleRemindersUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val geofenceRegistrar: GeofenceRegistrar? = null,
    private val now: () -> Instant = Instant::now,
) {
    suspend operator fun invoke() {
        val current = now()
        reminders.observeAll().first()
            .filter { it.status == ReminderStatus.Active && !it.isDone }
            .forEach { reminder ->
                reminder.alarms
                    .filter { it.at.isAfter(current) }
                    .forEach { scheduler.schedule(reminder.id, it.at) }

                geofenceRegistrar?.let { registrar ->
                    reminder.places.forEach { placeAlert ->
                        registrar.ensureRegistered(placeAlert.placeId)
                    }
                }
            }
    }
}

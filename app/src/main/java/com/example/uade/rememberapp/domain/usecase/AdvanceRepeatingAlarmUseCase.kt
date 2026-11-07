package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.nextOccurrence
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import java.time.Instant
import java.time.ZoneId

/**
 * Sonó el aviso de las [firedAt]: si se repite, lo reemplaza por su próxima vez y la programa.
 *
 * Ej.: un aviso diario a las 8:00 suena el lunes; queda guardado para el martes a las 8:00. Así
 * en la base siempre está la próxima vez y el resto de la app (la card, el detalle, reiniciar el
 * teléfono) lo ve como un aviso más, sin saber de repeticiones.
 *
 * Se corre ANTES de mostrar la notificación: "Posponer" reemplaza los avisos hasta [firedAt], y
 * el siguiente ya es posterior, así que no lo borra.
 *
 * [now] y [zone] se reciben por constructor para que los tests puedan fijar la hora.
 */
class AdvanceRepeatingAlarmUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val now: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    suspend operator fun invoke(reminderId: Long, firedAt: Instant) {
        val reminder = reminders.getById(reminderId) ?: return
        val fired = reminder.alarms.firstOrNull { it.at == firedAt } ?: return
        // Desde la que sonó o desde ahora, lo más tarde: si sonó con atraso, no se repite enseguida.
        val after = maxOf(firedAt, now())
        val next = fired.nextOccurrence(after, zone) ?: return

        val alarms = reminder.alarms.map { if (it == fired) it.copy(at = next) else it }
        reminders.save(reminder.copy(alarms = alarms.distinctBy { it.at }.sortedBy { it.at }))
        scheduler.schedule(reminderId, next)
    }
}

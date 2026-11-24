package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import com.example.uade.rememberapp.domain.scheduler.cancelAll
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * "Posponer" desde la notificación del aviso que sonó a las [firedAt]: lo vuelve a avisar dentro
 * de [minutes] minutos.
 *
 * El nuevo aviso pospuesto "lleva la posta": reemplaza al que sonó, a todos los anteriores y a
 * cualquier otro pospuesto que quedara pendiente (aunque sea para después). Así nunca hay dos
 * pospuestos sonando para el mismo recordatorio. Los avisos que el usuario puso para después del
 * que sonó se conservan.
 *
 * Ej.: avisos a las 15:00 y 16:00. A las 15:00 se pospone hasta las 15:15, y así hasta las
 * 16:05. Suena el de las 16:00 y se pospone: queda solo el de las 16:15 (el de las 16:05 se
 * descarta).
 *
 * [now] es el reloj: se recibe por constructor para que los tests puedan fijar la hora.
 */
class SnoozeReminderUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val now: () -> Instant = Instant::now,
) {
    suspend operator fun invoke(reminderId: Long, firedAt: Instant, minutes: Long = 15) {
        val reminder = reminders.getById(reminderId) ?: return
        val (kept, discarded) = reminder.alarms.partition { !it.isSnooze && it.at.isAfter(firedAt) }
        val snooze = Alarm(at = now().plus(minutes, ChronoUnit.MINUTES), isSnooze = true)

        reminders.save(reminder.copy(alarms = (kept + snooze).sortedBy { it.at }))
        scheduler.cancelAll(reminderId, discarded)
        scheduler.schedule(reminderId, snooze.at)
    }
}

package com.example.uade.rememberapp.domain.scheduler

import com.example.uade.rememberapp.domain.model.Alarm
import java.time.Instant

/**
 * Programa y cancela avisos por hora. Lo implementa la capa de datos con AlarmManager.
 *
 * Un recordatorio puede tener varios avisos: cada par ([reminderId], `at`) es una alarma
 * distinta del sistema. Para cancelar hay que decir cuál (el sistema no deja preguntar qué
 * alarmas hay programadas).
 */
interface ReminderScheduler {
    suspend fun schedule(reminderId: Long, at: Instant)
    suspend fun cancel(reminderId: Long, at: Instant)
}

/** Cancela todos estos avisos del recordatorio. */
suspend fun ReminderScheduler.cancelAll(reminderId: Long, alarms: List<Alarm>) {
    alarms.forEach { cancel(reminderId, it.at) }
}

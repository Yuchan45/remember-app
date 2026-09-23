package com.example.uade.rememberapp.domain.scheduler

import java.time.Instant

/** Programa y cancela avisos por hora. Lo implementa la capa de datos con AlarmManager. */
interface ReminderScheduler {
    suspend fun schedule(reminderId: Long, at: Instant)
    suspend fun cancel(reminderId: Long)
}

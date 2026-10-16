package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import java.time.Instant

/** Scheduler en memoria para tests: anota qué avisos quedaron programados, por recordatorio. */
internal class FakeReminderScheduler : ReminderScheduler {
    private val scheduled = mutableMapOf<Long, MutableSet<Instant>>()

    /** Los avisos programados de [reminderId], ordenados. */
    fun scheduledFor(reminderId: Long): List<Instant> = scheduled[reminderId].orEmpty().sorted()

    override suspend fun schedule(reminderId: Long, at: Instant) {
        scheduled.getOrPut(reminderId) { mutableSetOf() } += at
    }

    override suspend fun cancel(reminderId: Long, at: Instant) {
        scheduled[reminderId]?.remove(at)
    }
}

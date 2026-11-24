package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ReminderBucket
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import java.time.Instant

/**
 * Borra para siempre los recordatorios cuyo plazo en el Historial ya venció: los hechos a los 7
 * días y los eliminados a los 15 (ver [ReminderBucket.retention]). Los archivados no vencen.
 *
 * Lo corre todos los días un worker en segundo plano. Devuelve cuántos borró.
 */
class PurgeExpiredRemindersUseCase(
    private val reminders: ReminderRepository,
    private val deleteForever: DeleteRemindersForeverUseCase,
    private val now: () -> Instant = Instant::now,
) {
    suspend operator fun invoke(): Int {
        val current = now()
        val expired = ReminderBucket.entries
            .filter { it.retention != null }
            .flatMap { reminders.getByBucket(it) }
            .filter { reminder -> reminder.expiresAt?.let { !it.isAfter(current) } == true }
            .map { it.id }
        if (expired.isNotEmpty()) deleteForever(expired)
        return expired.size
    }
}

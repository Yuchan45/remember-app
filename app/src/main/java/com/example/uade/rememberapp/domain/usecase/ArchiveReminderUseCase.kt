package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import java.time.Instant

/**
 * Archiva un recordatorio ([ReminderStatus.Archived]): sale de la Home y queda en el Historial
 * para siempre (los archivados no se borran solos), hasta que se restaure.
 *
 * Guarda con [SaveReminderUseCase], que cancela sus alarmas porque ya no está activo.
 */
class ArchiveReminderUseCase(
    private val reminders: ReminderRepository,
    private val saveReminder: SaveReminderUseCase,
    private val now: () -> Instant = Instant::now,
) {
    suspend operator fun invoke(id: Long) {
        val reminder = reminders.getById(id) ?: return
        saveReminder(reminder.copy(status = ReminderStatus.Archived, statusChangedAt = now()))
    }
}

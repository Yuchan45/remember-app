package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import java.time.Instant

/**
 * Manda un recordatorio a la papelera: no lo borra de la base, le cambia el estado a
 * [ReminderStatus.Deleted]. Así deja de aparecer en la Home pero se puede recuperar desde el
 * Historial, hasta que [PurgeExpiredRemindersUseCase] lo borra pasados unos días.
 *
 * Guarda con [SaveReminderUseCase], que cancela sus alarmas porque ya no está activo.
 */
class TrashReminderUseCase(
    private val reminders: ReminderRepository,
    private val saveReminder: SaveReminderUseCase,
    private val now: () -> Instant = Instant::now,
) {
    suspend operator fun invoke(id: Long) {
        val reminder = reminders.getById(id) ?: return
        saveReminder(reminder.copy(status = ReminderStatus.Deleted, statusChangedAt = now()))
    }
}

package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Los recordatorios con cierto [ReminderStatus], actualizados en vivo. La Home pide los
 * activos; la sección Archivo va a pedir los archivados y los eliminados.
 */
class ObserveRemindersUseCase(
    private val reminders: ReminderRepository,
) {
    operator fun invoke(status: ReminderStatus = ReminderStatus.Active): Flow<List<Reminder>> =
        reminders.observeAll().map { list ->
            list.filter { reminder ->
                when (status) {
                    ReminderStatus.Active -> reminder.status == ReminderStatus.Active && !reminder.isDone
                    ReminderStatus.Archived -> reminder.status == ReminderStatus.Archived || (reminder.status == ReminderStatus.Active && reminder.isDone)
                    ReminderStatus.Deleted -> reminder.status == ReminderStatus.Deleted
                }
            }
        }
}

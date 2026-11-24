package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderBucket
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow

/**
 * Los recordatorios de una sección, actualizados en vivo. La Home pide los activos
 * ([ReminderBucket.Active]); el Historial, los hechos, los archivados y los eliminados.
 */
class ObserveRemindersUseCase(
    private val reminders: ReminderRepository,
) {
    operator fun invoke(bucket: ReminderBucket = ReminderBucket.Active): Flow<List<Reminder>> =
        reminders.observeByBucket(bucket)
}

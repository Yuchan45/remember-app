package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.repository.ReminderRepository

/**
 * Guarda un recordatorio (nuevo o editado) y devuelve su id.
 *
 * Hoy solo delega en el repositorio, pero existe para que las reglas que acompañan a guardar
 * no terminen en el ViewModel.
 *
 * TODO: programar una alarma con ReminderScheduler por cada Alarm (y cancelar las anteriores
 * si cambiaron) y registrar la geofence de cada PlaceAlert.
 */
class SaveReminderUseCase(
    private val reminders: ReminderRepository,
) {
    suspend operator fun invoke(reminder: Reminder): Long = reminders.save(reminder)
}

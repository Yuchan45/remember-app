package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.repository.ReminderRepository

/**
 * Guarda un recordatorio (nuevo o editado) y devuelve su id.
 *
 * Hoy solo delega en el repositorio, pero existe para que las reglas que acompañan a guardar
 * no terminen en el ViewModel.
 *
 * TODO: con Trigger.AtTime, programar la alarma con ReminderScheduler (y cancelar la anterior
 * si cambió); con Trigger.AtPlace, registrar la geofence del lugar.
 */
class SaveReminderUseCase(
    private val reminders: ReminderRepository,
) {
    suspend operator fun invoke(reminder: Reminder): Long = reminders.save(reminder)
}

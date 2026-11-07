package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.repository.PhotoRepository
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import com.example.uade.rememberapp.domain.scheduler.cancelAll

/**
 * Borra recordatorios para siempre: la fila (sus etiquetas, avisos e ítems se van solos por
 * CASCADE), sus alarmas y sus archivos (fotos, portada y audio). No se puede deshacer.
 *
 * Los archivos se borran a mano porque la base solo guarda la ruta: sin esto, cada nota borrada
 * dejaría sus fotos ocupando espacio en el teléfono.
 */
class DeleteRemindersForeverUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val files: PhotoRepository,
) {
    suspend operator fun invoke(ids: Collection<Long>) {
        ids.forEach { id ->
            val reminder = reminders.getById(id) ?: return@forEach
            scheduler.cancelAll(id, reminder.alarms)
            reminders.delete(id)
            // PhotoRepository.delete borra cualquier archivo de la app por su ruta, también el audio.
            (reminder.photos.map { it.path } + listOfNotNull(reminder.photoPath, reminder.audioPath))
                .distinct()
                .forEach { files.delete(it) }
        }
    }
}

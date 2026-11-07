package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.repository.PhotoRepository
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import com.example.uade.rememberapp.domain.scheduler.cancelAll
import kotlinx.coroutines.flow.first

/**
 * Borra recordatorios para siempre: la fila (sus etiquetas, avisos e ítems se van solos por
 * CASCADE), sus alarmas y sus archivos (fotos, portada y audio). No se puede deshacer.
 *
 * Los archivos se borran a mano porque la base solo guarda la ruta: sin esto, cada nota borrada
 * dejaría sus fotos ocupando espacio en el teléfono.
 *
 * El audio es la excepción: una misma nota de voz puede haber creado varios recordatorios, que
 * comparten la grabación. Solo se borra si ya no la usa ninguno de los que quedan.
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
            (reminder.photos.map { it.path } + listOfNotNull(reminder.photoPath))
                .distinct()
                .forEach { files.delete(it) }
            reminder.audioPath?.let { audio ->
                val stillUsed = reminders.observeAll().first().any { it.audioPath == audio }
                if (!stillUsed) files.delete(audio)
            }
        }
    }
}

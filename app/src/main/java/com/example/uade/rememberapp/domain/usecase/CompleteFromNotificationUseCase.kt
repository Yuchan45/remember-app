package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.AlarmRepeat
import com.example.uade.rememberapp.domain.repository.ReminderRepository

/**
 * El botón "Hecho" de la notificación. Hace lo mismo que la Home, con dos casos especiales:
 *
 * - **Con una lista sin terminar:** la notificación ya avisa en el botón ("Marcar todo y
 *   finalizar"), así que acá se tildan todos los ítems y se finaliza. En la Home el mismo caso
 *   pregunta antes con un diálogo; desde la notificación no se puede preguntar.
 * - **Con un aviso que se repite** (ej. "tomar la pastilla" todos los días): "Hecho" vale para
 *   esta vez. El recordatorio sigue activo y vuelve a sonar la próxima; si no, marcarlo una vez
 *   cortaría la repetición para siempre. Para terminarla, se archiva o se elimina desde la app.
 */
class CompleteFromNotificationUseCase(
    private val reminders: ReminderRepository,
    private val markReminderDone: MarkReminderDoneUseCase,
) {
    suspend operator fun invoke(reminderId: Long) {
        val reminder = reminders.getById(reminderId) ?: return
        if (reminder.alarms.any { it.repeat != AlarmRepeat.None }) return
        if (reminder.items.any { !it.isChecked }) {
            reminders.save(reminder.copy(items = reminder.items.map { it.copy(isChecked = true) }))
        }
        markReminderDone(reminderId)
    }
}

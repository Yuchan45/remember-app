package com.example.uade.rememberapp.data.local.mapper

import com.example.uade.rememberapp.data.local.entity.ActionEntity
import com.example.uade.rememberapp.data.local.entity.AlarmEntity
import com.example.uade.rememberapp.data.local.entity.ChecklistItemEntity
import com.example.uade.rememberapp.data.local.entity.PhotoEntity
import com.example.uade.rememberapp.data.local.entity.PlaceAlertEntity
import com.example.uade.rememberapp.data.local.entity.ReminderEntity
import com.example.uade.rememberapp.data.local.entity.ReminderWithDetails
import com.example.uade.rememberapp.domain.model.AiProcessingStatus
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderPhoto
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.Tag
import java.time.Instant

// Traducción entre el modelo del dominio y las filas de Room. Vive en `data` para que el dominio
// no sepa nada de cómo se guarda.

/** El recordatorio con sus etiquetas y avisos, tal como lo trae Room en una sola consulta. */
fun ReminderWithDetails.toDomain(): Reminder = reminder.toDomain(
    tags = tags.map { it.toDomain() },
    alarms = alarms.map { it.toDomain() }.sortedBy { it.at },
    places = places.map { it.toDomain() },
    action = actions.firstOrNull()?.toDomain(),
    photos = photos.sortedBy { it.position }.map { ReminderPhoto(path = it.path, isCover = it.isCover) },
    items = items.sortedBy { it.position }.map { it.toDomain() },
)

/**
 * Las etiquetas y los avisos no están en esta fila (viven en sus tablas): entran por parámetro.
 * Al revés, [toEntity] no los incluye; los guarda aparte ReminderDao.saveWithDetails.
 */
fun ReminderEntity.toDomain(
    tags: List<Tag> = emptyList(),
    alarms: List<Alarm> = emptyList(),
    places: List<PlaceAlert> = emptyList(),
    action: NotificationAction? = null,
    photos: List<ReminderPhoto> = emptyList(),
    items: List<ChecklistItem> = emptyList(),
): Reminder = Reminder(
    id = id,
    type = enumOrDefault(type, ReminderType.Note),
    title = title,
    description = description,
    photoPath = photoPath,
    photos = photos,
    audioPath = audioPath,
    items = items,
    tags = tags,
    alarms = alarms,
    places = places,
    action = action,
    isDone = isDone,
    status = enumOrDefault(status, ReminderStatus.Active),
    aiStatus = enumOrDefault(aiStatus, AiProcessingStatus.None),
    importance = enumOrDefault(importance, Importance.Default),
    createdAt = Instant.ofEpochMilli(createdAtMillis),
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    type = type.name,
    title = title,
    description = description,
    photoPath = photoPath,
    audioPath = audioPath,
    isDone = isDone,
    status = status.name,
    aiStatus = aiStatus.name,
    createdAtMillis = createdAt.toEpochMilli(),
    importance = importance.name,
)

fun AlarmEntity.toDomain(): Alarm = Alarm(at = Instant.ofEpochMilli(atMillis), isSnooze = isSnooze)

/** La fila de un aviso por hora; el reminderId lo completa el DAO al guardar. */
fun Alarm.toEntity(reminderId: Long = 0): AlarmEntity =
    AlarmEntity(reminderId = reminderId, atMillis = at.toEpochMilli(), isSnooze = isSnooze)

fun PlaceAlertEntity.toDomain(): PlaceAlert =
    PlaceAlert(placeId = placeId, event = enumOrDefault(event, PlaceAlertEvent.Arrive))

/** El [PlaceAlertEntity] de un aviso por lugar; el reminderId lo completa el DAO al guardar. */
fun PlaceAlert.toEntity(reminderId: Long = 0): PlaceAlertEntity =
    PlaceAlertEntity(reminderId = reminderId, placeId = placeId, event = event.name)

/** La fila de una foto; [position] es su lugar en la lista y el reminderId lo completa el DAO. */
fun ReminderPhoto.toEntity(position: Int, reminderId: Long = 0): PhotoEntity =
    PhotoEntity(reminderId = reminderId, path = path, position = position, isCover = isCover)

fun ChecklistItemEntity.toDomain(): ChecklistItem =
    ChecklistItem(id = id, text = text, note = note, isChecked = isChecked)

fun ChecklistItem.toEntity(position: Int, reminderId: Long = 0): ChecklistItemEntity =
    ChecklistItemEntity(
        id = id,
        reminderId = reminderId,
        text = text,
        note = note,
        isChecked = isChecked,
        position = position,
    )

/** Un valor guardado que ya no existe en el enum (ej. se renombró) cae en [default] en vez de crashear. */
private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

// La acción de notificación (sealed) se guarda en una fila de `reminder_actions`: [ActionEntity.type]
// dice cuál es y las demás columnas guardan sus datos.

private const val ActionCall = "Call"
private const val ActionMessage = "Message"
private const val ActionEmail = "Email"

/** La fila de una acción; el reminderId lo completa el DAO al guardar. */
fun NotificationAction.toEntity(reminderId: Long = 0): ActionEntity = when (this) {
    is NotificationAction.Call ->
        ActionEntity(reminderId = reminderId, type = ActionCall, channel = null, target = phone, text = null)
    is NotificationAction.Message ->
        ActionEntity(reminderId = reminderId, type = ActionMessage, channel = channel.name, target = to, text = text)
    is NotificationAction.Email ->
        ActionEntity(reminderId = reminderId, type = ActionEmail, channel = null, target = to, text = subject)
}

/** La acción de una fila, o null si el tipo no se conoce (ej. se renombró). */
fun ActionEntity.toDomain(): NotificationAction? = when (type) {
    ActionCall -> NotificationAction.Call(phone = target)
    ActionMessage -> NotificationAction.Message(
        channel = enumOrDefault(channel.orEmpty(), MessageChannel.Sms),
        to = target,
        text = text,
    )
    ActionEmail -> NotificationAction.Email(to = target, subject = text)
    else -> null
}

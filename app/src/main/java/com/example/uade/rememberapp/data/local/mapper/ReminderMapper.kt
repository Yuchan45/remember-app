package com.example.uade.rememberapp.data.local.mapper

import com.example.uade.rememberapp.data.local.entity.PlaceAlertEntity
import com.example.uade.rememberapp.data.local.entity.ReminderEntity
import com.example.uade.rememberapp.data.local.entity.ReminderWithDetails
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.Tag
import java.time.Instant

// Traducción entre el modelo del dominio y las filas de Room. Vive en `data` para que el dominio
// no sepa nada de cómo se guarda.

/** El recordatorio con sus etiquetas y avisos, tal como lo trae Room en una sola consulta. */
fun ReminderWithDetails.toDomain(): Reminder = reminder.toDomain(
    tags = tags.map { it.toDomain() },
    alarms = alarms.map { Alarm(Instant.ofEpochMilli(it.atMillis)) }.sortedBy { it.at },
    places = places.map { it.toDomain() },
)

/**
 * Las etiquetas y los avisos no están en esta fila (viven en sus tablas): entran por parámetro.
 * Al revés, [toEntity] no los incluye; los guarda aparte ReminderDao.saveWithDetails.
 */
fun ReminderEntity.toDomain(
    tags: List<Tag> = emptyList(),
    alarms: List<Alarm> = emptyList(),
    places: List<PlaceAlert> = emptyList(),
): Reminder = Reminder(
    id = id,
    type = enumOrDefault(type, ReminderType.Note),
    title = title,
    description = description,
    photoPath = photoPath,
    tags = tags,
    alarms = alarms,
    places = places,
    isDone = isDone,
    status = enumOrDefault(status, ReminderStatus.Active),
    createdAt = Instant.ofEpochMilli(createdAtMillis),
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    type = type.name,
    title = title,
    description = description,
    photoPath = photoPath,
    isDone = isDone,
    status = status.name,
    createdAtMillis = createdAt.toEpochMilli(),
)

fun PlaceAlertEntity.toDomain(): PlaceAlert =
    PlaceAlert(placeId = placeId, event = enumOrDefault(event, PlaceAlertEvent.Arrive))

/** El [PlaceAlertEntity] de un aviso por lugar; el reminderId lo completa el DAO al guardar. */
fun PlaceAlert.toEntity(reminderId: Long = 0): PlaceAlertEntity =
    PlaceAlertEntity(reminderId = reminderId, placeId = placeId, event = event.name)

/** Un valor guardado que ya no existe en el enum (ej. se renombró) cae en [default] en vez de crashear. */
private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

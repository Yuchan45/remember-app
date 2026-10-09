package com.example.uade.rememberapp.data.local.mapper

import com.example.uade.rememberapp.data.local.entity.ReminderEntity
import com.example.uade.rememberapp.domain.model.AiProcessingStatus
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.Trigger
import java.time.Instant

// Traducción entre el modelo del dominio y la fila de Room. Vive en `data` para que el dominio
// no sepa nada de cómo se guarda.

internal const val TriggerNone = "NONE"
internal const val TriggerAtTime = "AT_TIME"
internal const val TriggerAtPlace = "AT_PLACE"

fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    type = enumOrDefault(type, ReminderType.Note),
    title = title,
    description = description,
    photoPath = photoPath,
    audioPath = audioPath,
    trigger = toTrigger(),
    isDone = isDone,
    status = enumOrDefault(status, ReminderStatus.Active),
    aiStatus = enumOrDefault(aiStatus, AiProcessingStatus.None),
    createdAt = Instant.ofEpochMilli(createdAtMillis),
)

fun Reminder.toEntity(): ReminderEntity {
    val (triggerType, atMillis, placeId) = when (val t = trigger) {
        Trigger.None -> Triple(TriggerNone, null, null)
        is Trigger.AtTime -> Triple(TriggerAtTime, t.at.toEpochMilli(), null)
        is Trigger.AtPlace -> Triple(TriggerAtPlace, null, t.placeId)
    }
    return ReminderEntity(
        id = id,
        type = type.name,
        title = title,
        description = description,
        photoPath = photoPath,
        audioPath = audioPath,
        triggerType = triggerType,
        triggerAtMillis = atMillis,
        triggerPlaceId = placeId,
        isDone = isDone,
        status = status.name,
        aiStatus = aiStatus.name,
        createdAtMillis = createdAt.toEpochMilli(),
    )
}

/** Arma el Trigger desde sus tres columnas. Si faltan datos o el tipo no se conoce, no hay aviso. */
private fun ReminderEntity.toTrigger(): Trigger = when (triggerType) {
    TriggerAtTime -> triggerAtMillis?.let { Trigger.AtTime(Instant.ofEpochMilli(it)) } ?: Trigger.None
    TriggerAtPlace -> triggerPlaceId?.let { Trigger.AtPlace(it) } ?: Trigger.None
    else -> Trigger.None
}

/** Un valor guardado que ya no existe en el enum (ej. se renombró) cae en [default] en vez de crashear. */
private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

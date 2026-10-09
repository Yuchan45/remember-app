package com.example.uade.rememberapp.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Un recordatorio con todo lo que vive en otras tablas, como lo devuelve Room en una sola
 * consulta.
 *
 * - [Embedded]: las columnas de `reminders` van directo acá.
 * - [tags]: muchos a muchos. Room busca en `reminder_tags` las filas de este recordatorio
 *   (reminderId = id) y trae las `tags` correspondientes (tagId = id de la etiqueta). Por eso
 *   lleva [Junction] (la tabla intermedia).
 * - [alarms] y [places]: uno a muchos. Las filas de `reminder_alarms` y `reminder_places` con
 *   reminderId = id. No hace falta tabla intermedia.
 *
 * Las consultas que lo devuelven necesitan `@Transaction`: Room hace más de una consulta por
 * dentro y así las lee todas del mismo estado de la base.
 */
data class ReminderWithDetails(
    @Embedded val reminder: ReminderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ReminderTagCrossRef::class,
            parentColumn = "reminderId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
    @Relation(parentColumn = "id", entityColumn = "reminderId")
    val alarms: List<AlarmEntity>,
    @Relation(parentColumn = "id", entityColumn = "reminderId")
    val places: List<PlaceAlertEntity>,
)

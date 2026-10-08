package com.example.uade.rememberapp.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Un recordatorio con sus etiquetas, como lo devuelve Room en una sola consulta.
 *
 * - [Embedded]: las columnas de `reminders` van directo acá.
 * - [Relation] + [Junction]: Room busca en `reminder_tags` las filas de este recordatorio
 *   (reminderId = id) y trae las `tags` correspondientes (tagId = id de la etiqueta).
 *
 * Las consultas que lo devuelven necesitan `@Transaction`: Room hace más de una consulta por
 * dentro y así las lee todas del mismo estado de la base.
 */
data class ReminderWithTags(
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
)

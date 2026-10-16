package com.example.uade.rememberapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Fila de la tabla `reminder_tags`: "el recordatorio [reminderId] tiene la etiqueta [tagId]".
 *
 * Es una tabla intermedia (cross-reference) porque la relación es de muchos a muchos: un
 * recordatorio tiene varias etiquetas y una etiqueta está en varios recordatorios. No se puede
 * guardar con una columna en ninguna de las dos tablas.
 *
 * Las dos claves foráneas tienen CASCADE: si se borra el recordatorio o la etiqueta, la fila que
 * los unía se borra sola (borrar una etiqueta la saca de todos los recordatorios).
 */
@Entity(
    tableName = "reminder_tags",
    primaryKeys = ["reminderId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // reminderId ya está indexada por ser la primera columna de la clave primaria.
    indices = [Index("tagId")],
)
data class ReminderTagCrossRef(
    val reminderId: Long,
    val tagId: Long,
)

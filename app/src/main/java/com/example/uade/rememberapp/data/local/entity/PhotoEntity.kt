package com.example.uade.rememberapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `reminder_photos`: una foto elegida para un recordatorio (sin recortar).
 *
 * - [position]: el orden en que se eligieron (SQLite no garantiza el orden de las filas).
 * - [isCover]: la que se eligió de portada. El recorte vive en `reminders.photoPath`.
 *
 * CASCADE: si se borra el recordatorio, sus filas se borran solas (los archivos no: TODO).
 */
@Entity(
    tableName = "reminder_photos",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["reminderId"])],
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val path: String,
    val position: Int,
    val isCover: Boolean,
)

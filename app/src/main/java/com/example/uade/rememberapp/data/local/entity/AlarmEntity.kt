package com.example.uade.rememberapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `reminder_alarms`: un aviso por hora de un recordatorio. Un recordatorio
 * tiene una fila por cada aviso (relación de uno a muchos).
 *
 * CASCADE: si se borra el recordatorio, sus avisos se borran solos.
 */
@Entity(
    tableName = "reminder_alarms",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("reminderId")],
)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val atMillis: Long,
)

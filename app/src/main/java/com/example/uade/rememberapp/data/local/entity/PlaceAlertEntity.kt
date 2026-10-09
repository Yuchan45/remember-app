package com.example.uade.rememberapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `reminder_places`: un aviso por lugar de un recordatorio (al llegar o al
 * salir). Un recordatorio tiene una fila por cada lugar.
 *
 * CASCADE: si se borra el recordatorio, sus avisos se borran solos.
 * TODO: clave foránea de [placeId] a `places` cuando exista esa tabla.
 */
@Entity(
    tableName = "reminder_places",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("reminderId"), Index("placeId")],
)
data class PlaceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val placeId: Long,
    /** "Arrive" o "Leave", por nombre (ver PlaceAlertEvent). */
    val event: String,
)

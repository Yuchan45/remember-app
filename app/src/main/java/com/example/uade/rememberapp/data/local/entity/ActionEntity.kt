package com.example.uade.rememberapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `reminder_actions`: la acción rápida de la notificación de un recordatorio.
 *
 * Va en una tabla aparte (y no en columnas de `reminders`) para que `reminders` tenga solo datos
 * propios de la nota, igual que las alarmas y los lugares. Hoy es 1 a 1: el índice único sobre
 * [reminderId] asegura una acción como mucho. Para permitir varias, alcanza con sacarlo.
 *
 * CASCADE: si se borra el recordatorio, su acción se borra sola.
 */
@Entity(
    tableName = "reminder_actions",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["reminderId"], unique = true)],
)
data class ActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    /** "Call", "Message" o "Email". */
    val type: String,
    /** Solo para "Message": "Sms", "WhatsApp" o "Instagram" (ver MessageChannel). */
    val channel: String?,
    /** El número, usuario o correo. */
    val target: String,
    /** El texto del mensaje o el asunto del correo, si tiene. */
    val text: String?,
)

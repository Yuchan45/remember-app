package com.example.uade.rememberapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `reminder_checklist_items`: un ítem de un recordatorio de tipo checklist.
 *
 * - [position]: orden del ítem en la lista (reordenamiento manual / drag & drop).
 * - [note]: nota aclaratoria opcional para el ítem.
 * - [isChecked]: si el ítem está completado.
 *
 * CASCADE: al borrar el recordatorio, sus ítems se borran solos.
 */
@Entity(
    tableName = "reminder_checklist_items",
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
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long = 0,
    val text: String,
    val note: String? = null,
    val isChecked: Boolean = false,
    val position: Int = 0,
)

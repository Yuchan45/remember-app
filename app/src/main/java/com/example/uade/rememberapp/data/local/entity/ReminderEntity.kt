package com.example.uade.rememberapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `reminders`: cómo se guarda un Reminder del dominio en la base. Es un espejo
 * "plano" del modelo, pensado para SQLite:
 * - Los `Instant` van como epoch millis (`Long`).
 * - Los enums van por nombre (`String`), así reordenarlos no rompe los datos guardados.
 * - `Trigger` (sealed) se aplana en tres columnas: [triggerType] dice cuál es y las otras dos
 *   guardan su dato (la hora o el lugar); la que no aplica queda en null.
 *
 * TODO: guardar ítems de lista y etiquetas en tablas propias (con relación a esta).
 * TODO: FK de [triggerPlaceId] a `places` cuando exista esa tabla (onDelete = SET_NULL).
 */
@Entity(
    tableName = "reminders",
    indices = [Index("triggerPlaceId")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String?,
    val description: String?,
    val photoPath: String?,
    val triggerType: String,
    val triggerAtMillis: Long?,
    val triggerPlaceId: Long?,
    val isDone: Boolean,
    val status: String,
    val createdAtMillis: Long,
)

package com.example.uade.rememberapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `reminders`: cómo se guarda un Reminder del dominio en la base. Es un espejo
 * "plano" del modelo, pensado para SQLite:
 * - Los `Instant` van como epoch millis (`Long`).
 * - Los enums van por nombre (`String`), así reordenarlos no rompe los datos guardados.
 *
 * Lo que puede ser una lista vive en tablas aparte, relacionadas por `reminderId`: las etiquetas
 * (`reminder_tags`), los avisos por hora (`reminder_alarms`) y por lugar (`reminder_places`).
 *
 * TODO: guardar los ítems de lista en una tabla propia.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String?,
    val description: String?,
    val photoPath: String?,
    val isDone: Boolean,
    val status: String,
    val createdAtMillis: Long,
    /**
     * Por nombre (ver Importance). El valor por defecto tiene que coincidir con el de la
     * migración 3 → 4 (`DEFAULT 'Default'`): Room compara las dos al abrir la base.
     */
    @ColumnInfo(defaultValue = "Default")
    val importance: String = "Default",
)

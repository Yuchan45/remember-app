package com.example.uade.rememberapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.uade.rememberapp.data.local.entity.ReminderEntity
import com.example.uade.rememberapp.data.local.entity.ReminderWithTags
import kotlinx.coroutines.flow.Flow

/**
 * Consultas a la tabla `reminders` (y a `reminder_tags`, sus etiquetas). Room genera la
 * implementación en tiempo de compilación (con KSP) y valida el SQL: una columna mal escrita es
 * un error de build, no un crash.
 *
 * Es una clase abstracta (no una interfaz) para poder tener [saveWithTags], que combina varias
 * operaciones en una transacción.
 *
 * Las que devuelven [Flow] no son `suspend`: Room vuelve a emitir cada vez que cambia alguna de
 * las tablas que leen (también `tags` y `reminder_tags`), así la UI se actualiza sola.
 */
@Dao
abstract class ReminderDao {

    @Transaction
    @Query("SELECT * FROM reminders ORDER BY createdAtMillis DESC")
    abstract fun observeAll(): Flow<List<ReminderWithTags>>

    @Transaction
    @Query(
        "SELECT * FROM reminders WHERE triggerType = 'AT_PLACE' AND triggerPlaceId = :placeId " +
            "AND isDone = 0 AND status = 'Active'",
    )
    abstract fun observePendingForPlace(placeId: Long): Flow<List<ReminderWithTags>>

    @Transaction
    @Query("SELECT * FROM reminders WHERE id = :id")
    abstract suspend fun getById(id: Long): ReminderWithTags?

    /**
     * Guarda el recordatorio y deja asignadas exactamente [tagIds]. Devuelve su id.
     *
     * `@Transaction`: o se hace todo o nada. Si algo falla a mitad de camino, no queda un
     * recordatorio guardado con las etiquetas a medio actualizar.
     */
    @Transaction
    open suspend fun saveWithTags(reminder: ReminderEntity, tagIds: Collection<Long>): Long {
        val newId = upsert(reminder)
        // @Upsert devuelve -1 cuando actualiza una fila existente: el id es el que ya tenía.
        val id = if (reminder.id != 0L) reminder.id else newId
        // Lo más simple para "dejar exactamente estas": borrar las anteriores y poner las nuevas.
        deleteTagsOf(id)
        if (tagIds.isNotEmpty()) insertExistingTags(id, tagIds)
        return id
    }

    /** Inserta si el id es 0 (y devuelve el id nuevo); si ya existe, lo actualiza y devuelve -1. */
    @Upsert
    protected abstract suspend fun upsert(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminder_tags WHERE reminderId = :reminderId")
    protected abstract suspend fun deleteTagsOf(reminderId: Long)

    /**
     * Asigna [tagIds] al recordatorio, pero solo las que todavía existen en `tags`.
     *
     * No alcanza con `OR IGNORE`: en SQLite no ignora las violaciones de clave foránea, y una
     * etiqueta borrada mientras tanto (ej. al deshacer la papelera de una nota) cerraba la app.
     */
    @Query(
        "INSERT OR IGNORE INTO reminder_tags (reminderId, tagId) " +
            "SELECT :reminderId, id FROM tags WHERE id IN (:tagIds)",
    )
    protected abstract suspend fun insertExistingTags(reminderId: Long, tagIds: Collection<Long>)

    /** Sus filas de `reminder_tags` se borran solas (CASCADE). */
    @Query("DELETE FROM reminders WHERE id = :id")
    abstract suspend fun deleteById(id: Long)
}

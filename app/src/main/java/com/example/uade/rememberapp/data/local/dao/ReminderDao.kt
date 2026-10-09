package com.example.uade.rememberapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.uade.rememberapp.data.local.entity.AlarmEntity
import com.example.uade.rememberapp.data.local.entity.PlaceAlertEntity
import com.example.uade.rememberapp.data.local.entity.ReminderEntity
import com.example.uade.rememberapp.data.local.entity.ReminderWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * Consultas a la tabla `reminders` y a las que cuelgan de ella: sus etiquetas
 * (`reminder_tags`), avisos por hora (`reminder_alarms`) y por lugar (`reminder_places`). Room
 * genera la implementación en tiempo de compilación (con KSP) y valida el SQL: una columna mal
 * escrita es un error de build, no un crash.
 *
 * Es una clase abstracta (no una interfaz) para poder tener [saveWithDetails], que combina
 * varias operaciones en una transacción.
 *
 * Las que devuelven [Flow] no son `suspend`: Room vuelve a emitir cada vez que cambia alguna de
 * las tablas que leen, así la UI se actualiza sola.
 */
@Dao
abstract class ReminderDao {

    @Transaction
    @Query("SELECT * FROM reminders ORDER BY createdAtMillis DESC")
    abstract fun observeAll(): Flow<List<ReminderWithDetails>>

    /** Los pendientes con aviso en [placeId] (para la geofence de ese lugar). */
    @Transaction
    @Query(
        "SELECT DISTINCT reminders.* FROM reminders " +
            "JOIN reminder_places ON reminder_places.reminderId = reminders.id " +
            "WHERE reminder_places.placeId = :placeId AND isDone = 0 AND status = 'Active'",
    )
    abstract fun observePendingForPlace(placeId: Long): Flow<List<ReminderWithDetails>>

    @Transaction
    @Query("SELECT * FROM reminders WHERE id = :id")
    abstract suspend fun getById(id: Long): ReminderWithDetails?

    /**
     * Guarda el recordatorio y deja exactamente estas etiquetas ([tagIds]), avisos por hora
     * ([alarmsAt], en epoch millis) y por lugar ([places]). Devuelve su id.
     *
     * `@Transaction`: o se hace todo o nada. Si algo falla a mitad de camino, no queda un
     * recordatorio guardado con sus listas a medio actualizar.
     */
    @Transaction
    open suspend fun saveWithDetails(
        reminder: ReminderEntity,
        tagIds: Collection<Long>,
        alarmsAt: List<Long>,
        places: List<PlaceAlertEntity>,
    ): Long {
        val newId = upsert(reminder)
        // @Upsert devuelve -1 cuando actualiza una fila existente: el id es el que ya tenía.
        val id = if (reminder.id != 0L) reminder.id else newId
        // Lo más simple para "dejar exactamente estas": borrar las anteriores y poner las nuevas.
        deleteTagsOf(id)
        if (tagIds.isNotEmpty()) insertExistingTags(id, tagIds)
        deleteAlarmsOf(id)
        insertAlarms(alarmsAt.map { AlarmEntity(reminderId = id, atMillis = it) })
        deletePlacesOf(id)
        insertPlaces(places.map { it.copy(id = 0, reminderId = id) })
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

    @Query("DELETE FROM reminder_alarms WHERE reminderId = :reminderId")
    protected abstract suspend fun deleteAlarmsOf(reminderId: Long)

    @Insert
    protected abstract suspend fun insertAlarms(alarms: List<AlarmEntity>)

    @Query("DELETE FROM reminder_places WHERE reminderId = :reminderId")
    protected abstract suspend fun deletePlacesOf(reminderId: Long)

    @Insert
    protected abstract suspend fun insertPlaces(places: List<PlaceAlertEntity>)

    /** Sus etiquetas, avisos y lugares se borran solos (CASCADE). */
    @Query("DELETE FROM reminders WHERE id = :id")
    abstract suspend fun deleteById(id: Long)
}

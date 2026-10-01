package com.example.uade.rememberapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.uade.rememberapp.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Consultas a la tabla `reminders`. Room genera la implementación en tiempo de compilación
 * (con KSP) y valida el SQL: una columna mal escrita es un error de build, no un crash.
 *
 * Las que devuelven [Flow] no son `suspend`: Room vuelve a emitir cada vez que la tabla cambia,
 * así la UI se actualiza sola al guardar.
 */
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query(
        "SELECT * FROM reminders WHERE triggerType = 'AT_PLACE' AND triggerPlaceId = :placeId " +
            "AND isDone = 0 AND status = 'Active'",
    )
    fun observePendingForPlace(placeId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    /** Inserta si el id es 0 (y devuelve el id nuevo); si ya existe, lo actualiza y devuelve -1. */
    @Upsert
    suspend fun upsert(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}

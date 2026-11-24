package com.example.uade.rememberapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.uade.rememberapp.data.local.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

/** Consultas a la tabla `places`. */
@Dao
interface PlaceDao {

    /** Emite la lista completa de lugares ordenados por id. */
    @Query("SELECT * FROM places ORDER BY id")
    fun observeAll(): Flow<List<PlaceEntity>>

    /** Busca un lugar por su id. */
    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun getById(id: Long): PlaceEntity?

    /** Lista todos los lugares guardados. */
    @Query("SELECT * FROM places")
    suspend fun getAll(): List<PlaceEntity>

    /** Inserta o reemplaza un lugar. Devuelve el id generado o insertado. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: PlaceEntity): Long

    /** Actualiza un lugar existente. */
    @Update
    suspend fun update(place: PlaceEntity)

    /** Borra un lugar por id. */
    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Borra los avisos asociados a un lugar eliminado. */
    @Query("DELETE FROM reminder_places WHERE placeId = :placeId")
    suspend fun deleteAlertsByPlaceId(placeId: Long)
}

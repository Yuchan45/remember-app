package com.example.uade.rememberapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.uade.rememberapp.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/** Consultas a la tabla `tags`. */
@Dao
interface TagDao {

    /** En el orden en que se crearon, como se muestran en el panel de etiquetas. */
    @Query("SELECT * FROM tags ORDER BY id")
    fun observeAll(): Flow<List<TagEntity>>

    /** Para validar nombres repetidos antes de crear o renombrar. */
    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    /** Con id 0 la crea y devuelve el id nuevo; con un id (ej. al deshacer un borrado) usa ese. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    /** Sus asignaciones en `reminder_tags` se borran solas (CASCADE). */
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)
}

package com.example.uade.rememberapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.uade.rememberapp.data.local.dao.ReminderDao
import com.example.uade.rememberapp.data.local.entity.ReminderEntity

/**
 * La base de datos de la app (un archivo SQLite). Lista todas las tablas (`entities`) y expone
 * un DAO por cada una. Room genera la clase concreta (`AppDatabase_Impl`).
 *
 * Al agregar una tabla o cambiar columnas hay que subir `version`. Mientras la app no esté
 * publicada, el AppContainer borra y recrea la base en ese caso (se pierden los datos).
 */
@Database(
    entities = [ReminderEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}

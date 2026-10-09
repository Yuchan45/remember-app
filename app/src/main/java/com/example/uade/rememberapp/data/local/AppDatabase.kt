package com.example.uade.rememberapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.uade.rememberapp.data.local.dao.ReminderDao
import com.example.uade.rememberapp.data.local.dao.TagDao
import com.example.uade.rememberapp.data.local.entity.AlarmEntity
import com.example.uade.rememberapp.data.local.entity.PlaceAlertEntity
import com.example.uade.rememberapp.data.local.entity.ReminderEntity
import com.example.uade.rememberapp.data.local.entity.ReminderTagCrossRef
import com.example.uade.rememberapp.data.local.entity.TagEntity

/**
 * La base de datos de la app (un archivo SQLite). Lista todas las tablas (`entities`) y expone
 * un DAO por cada una. Room genera la clase concreta (`AppDatabase_Impl`).
 *
 * Al agregar una tabla o cambiar columnas hay que subir `version`. Mientras la app no esté
 * publicada, el AppContainer borra y recrea la base en ese caso (se pierden los datos).
 *
 * Versiones:
 * - 1: `reminders`.
 * - 2: `tags` y `reminder_tags` (etiquetas y a qué recordatorios están asignadas).
 * - 3: varios avisos por recordatorio: `reminder_alarms` (horas) y `reminder_places` (lugares),
 *   en lugar de las columnas de un solo aviso en `reminders`.
 */
@Database(
    entities = [
        ReminderEntity::class,
        TagEntity::class,
        ReminderTagCrossRef::class,
        AlarmEntity::class,
        PlaceAlertEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun tagDao(): TagDao
}

/**
 * Carga las etiquetas de ejemplo cuando la base se crea por primera vez. Las de ids 1 y 5 son
 * las mismas que usan los datos de ejemplo de las previews.
 *
 * Solo en onCreate, no en onDestructiveMigration: Room llama a ese callback después de borrar las
 * tablas pero ANTES de crearlas de nuevo, así que el INSERT fallaba ("no such table: tags") y la
 * app se cerraba al abrir. Por eso, si la base se recrea por un cambio de versión, arranca sin
 * etiquetas de ejemplo.
 */
internal class SeedTagsCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT OR IGNORE INTO tags (id, name, colorArgb) VALUES " +
                "(1, 'Salud', ${0xFF6FCF97}), (5, 'Ideas', ${0xFFF2C94C})",
        )
    }
}

package com.example.uade.rememberapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones de la base: cómo pasar los datos de una versión a la siguiente sin perderlos.
 *
 * Sin una migración, el AppContainer borra y recrea la base al cambiar la versión (se pierden las
 * notas). Con una, Room la ejecuta al abrir la base y después verifica que el resultado coincida
 * con las entidades; si no coincide, la app se cierra con "Migration didn't properly handle…".
 */

/** 3 → 4: la importancia de cada recordatorio. Las notas que ya existían quedan en "Default". */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // NOT NULL necesita un DEFAULT para las filas que ya existen. Tiene que ser el mismo que
        // el @ColumnInfo(defaultValue) de ReminderEntity.importance.
        db.execSQL("ALTER TABLE reminders ADD COLUMN importance TEXT NOT NULL DEFAULT 'Default'")
    }
}

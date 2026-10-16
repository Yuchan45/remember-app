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

/**
 * 4 → 5: la tabla de acciones de notificación (ver ActionEntity). Las notas que ya existían
 * quedan sin acción.
 *
 * El SQL es el mismo que genera Room para ActionEntity (está en AppDatabase_Impl, que KSP genera
 * en build/generated): si difiere en algo (tipos, NOT NULL, la FK o el nombre del índice), Room no
 * lo acepta al abrir la base.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminder_actions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`reminderId` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`channel` TEXT, " +
                "`target` TEXT NOT NULL, " +
                "`text` TEXT, " +
                "FOREIGN KEY(`reminderId`) REFERENCES `reminders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_reminder_actions_reminderId` " +
                "ON `reminder_actions` (`reminderId`)",
        )
    }
}

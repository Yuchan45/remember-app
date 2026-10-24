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

/**
 * 5 → 6: `reminder_photos`, las fotos elegidas de cada recordatorio. Las notas que ya existían
 * quedan sin fotos (su `photoPath`, si tenían, sigue siendo la portada).
 *
 * Igual que en MIGRATION_4_5, el SQL es el que genera Room para PhotoEntity.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminder_photos` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`reminderId` INTEGER NOT NULL, " +
                "`path` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "`isCover` INTEGER NOT NULL, " +
                "FOREIGN KEY(`reminderId`) REFERENCES `reminders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reminder_photos_reminderId` " +
                "ON `reminder_photos` (`reminderId`)",
        )
    }
}

/**
 * 6 → 7: `isSnooze` en `reminder_alarms`, para distinguir el aviso que creó "Posponer".
 * Verifica si la tabla `reminder_alarms` o `reminder_places` existen previamente, creándolas
 * si faltaban desde una versión previa de desarrollo para evitar fallos de migración SQLite.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val cursorAlarms = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='reminder_alarms'")
        val alarmsTableExists = cursorAlarms.use { it.moveToFirst() }

        if (!alarmsTableExists) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `reminder_alarms` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`reminderId` INTEGER NOT NULL, " +
                    "`atMillis` INTEGER NOT NULL, " +
                    "`isSnooze` INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(`reminderId`) REFERENCES `reminders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_reminder_alarms_reminderId` " +
                    "ON `reminder_alarms` (`reminderId`)",
            )
        } else {
            val colCursor = db.query("PRAGMA table_info(`reminder_alarms`)")
            var hasSnooze = false
            colCursor.use {
                val nameIndex = it.getColumnIndex("name")
                while (it.moveToNext()) {
                    if (nameIndex != -1 && it.getString(nameIndex) == "isSnooze") {
                        hasSnooze = true
                        break
                    }
                }
            }
            if (!hasSnooze) {
                db.execSQL("ALTER TABLE `reminder_alarms` ADD COLUMN `isSnooze` INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Aseguramos también reminder_places si faltaba de una versión previa
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminder_places` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`reminderId` INTEGER NOT NULL, " +
                "`placeId` INTEGER NOT NULL, " +
                "`event` TEXT NOT NULL, " +
                "FOREIGN KEY(`reminderId`) REFERENCES `reminders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reminder_places_reminderId` " +
                "ON `reminder_places` (`reminderId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reminder_places_placeId` " +
                "ON `reminder_places` (`placeId`)",
        )
    }
}

/**
 * 7 → 8: `audioPath` y `aiStatus` en `reminders`, para grabaciones de voz y estado de procesamiento IA.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val colCursor = db.query("PRAGMA table_info(`reminders`)")
        var hasAudioPath = false
        var hasAiStatus = false
        colCursor.use {
            val nameIndex = it.getColumnIndex("name")
            while (it.moveToNext()) {
                if (nameIndex != -1) {
                    val colName = it.getString(nameIndex)
                    if (colName == "audioPath") hasAudioPath = true
                    if (colName == "aiStatus") hasAiStatus = true
                }
            }
        }
        if (!hasAudioPath) {
            db.execSQL("ALTER TABLE `reminders` ADD COLUMN `audioPath` TEXT")
        }
        if (!hasAiStatus) {
            db.execSQL("ALTER TABLE `reminders` ADD COLUMN `aiStatus` TEXT NOT NULL DEFAULT 'None'")
        }
    }
}

/**
 * 8 → 9: `reminder_checklist_items`, ítems de un recordatorio tipo lista (checklist).
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminder_checklist_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`reminderId` INTEGER NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`note` TEXT, " +
                "`isChecked` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`reminderId`) REFERENCES `reminders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reminder_checklist_items_reminderId` " +
                "ON `reminder_checklist_items` (`reminderId`)",
        )
    }
}

/**
 * 9 → 10: `places`, lugares guardados por el usuario para alertas por ubicación.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `places` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, " +
                "`radiusMeters` INTEGER NOT NULL, " +
                "`kind` TEXT NOT NULL)",
        )
    }
}

package com.example.uade.rememberapp.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * Red de seguridad para no perder recordatorios: antes de que Room abra la base, si la versión
 * guardada en el archivo es distinta de la que espera la app (o sea, Room va a migrar), copia el
 * archivo a `databases/backups/<nombre>-v<versión vieja>-<fecha>.db`.
 *
 * Si una migración sale mal, o se instala una versión de otra rama, la copia sigue ahí y se puede
 * restaurar (ej. desde el Device Explorer de Android Studio). Se guardan las últimas
 * 5 para no llenar el teléfono.
 *
 * Se corre una sola vez por proceso, antes del primer acceso a la base: en ese momento Room
 * todavía no la abrió, así que el archivo no está a medio escribir.
 */
object DatabaseBackup {

    fun backupIfVersionChanged(context: Context, name: String, expectedVersion: Int) {
        val dbFile = context.getDatabasePath(name)
        if (!dbFile.exists()) return
        try {
            val currentVersion = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                .use { it.version }
            if (currentVersion == expectedVersion) return

            val dir = File(dbFile.parentFile, "backups").apply { mkdirs() }
            val base = "${dbFile.nameWithoutExtension}-v$currentVersion-${System.currentTimeMillis()}"
            // El archivo principal y, si existen, los del modo WAL (cambios que todavía no se
            // pasaron al principal): sin ellos la copia podría quedar incompleta.
            listOf("", "-wal", "-shm").forEach { suffix ->
                val source = File(dbFile.path + suffix)
                if (source.exists()) source.copyTo(File(dir, "$base.db$suffix"), overwrite = true)
            }
            Log.i("DatabaseBackup", "Copia de la base v$currentVersion antes de migrar a v$expectedVersion: $base")

            dir.listFiles { file -> file.name.endsWith(".db") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(5) // Deja las 5 más nuevas y borra el resto.
                ?.forEach { old ->
                    listOf("", "-wal", "-shm").forEach { File(old.path + it).delete() }
                }
        } catch (e: Exception) {
            // La copia es una ayuda: si falla, la app sigue igual (y Room no borra nada).
            Log.w("DatabaseBackup", "No se pudo copiar la base antes de migrar", e)
        }
    }
}

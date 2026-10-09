package com.example.uade.rememberapp

import android.content.Context
import androidx.room.Room
import com.example.uade.rememberapp.data.local.AppDatabase
import com.example.uade.rememberapp.data.local.MIGRATION_3_4
import com.example.uade.rememberapp.data.local.SeedTagsCallback
import com.example.uade.rememberapp.data.repository.ReminderRepositoryImpl
import com.example.uade.rememberapp.data.repository.TagRepositoryImpl
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.usecase.GetReminderUseCase
import com.example.uade.rememberapp.domain.usecase.ObserveRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.RestoreReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.domain.usecase.TrashReminderUseCase

/**
 * Inyección de dependencias manual: crea una sola vez la base de datos, los repositorios y los
 * casos de uso, y los expone con el tipo de la interfaz del dominio (así la UI no depende de
 * `data`).
 *
 * Todo es `by lazy`: se crea recién la primera vez que alguien lo pide.
 *
 * Vive en [RememberApp]; los ViewModels lo toman desde su Factory.
 */
class AppContainer(context: Context) {

    // applicationContext y no una Activity: la base vive lo que el proceso, y guardar una
    // Activity acá la mantendría en memoria después de cerrarse (memory leak).
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "hey.db")
            // Los cambios con migración escrita conservan los datos.
            .addMigrations(MIGRATION_3_4)
            // Si no hay migración (ej. desde la versión 1 o 2), borra y recrea la base. Sirve
            // mientras la app no esté publicada. TODO: escribir migraciones antes de publicar.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(SeedTagsCallback())
            .build()
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(database.reminderDao())
    }

    val saveReminderUseCase: SaveReminderUseCase by lazy { SaveReminderUseCase(reminderRepository) }
    val observeRemindersUseCase: ObserveRemindersUseCase by lazy { ObserveRemindersUseCase(reminderRepository) }
    val getReminderUseCase: GetReminderUseCase by lazy { GetReminderUseCase(reminderRepository) }
    val trashReminderUseCase: TrashReminderUseCase by lazy { TrashReminderUseCase(reminderRepository) }
    val restoreReminderUseCase: RestoreReminderUseCase by lazy { RestoreReminderUseCase(reminderRepository) }

    /** Arranca con las etiquetas de ejemplo que carga [SeedTagsCallback] al crear la base. */
    val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(database.tagDao())
    }
}

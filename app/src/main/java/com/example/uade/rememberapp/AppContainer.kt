package com.example.uade.rememberapp

import android.content.Context
import androidx.room.Room
import com.example.uade.rememberapp.data.local.AppDatabase
import com.example.uade.rememberapp.data.local.MIGRATION_3_4
import com.example.uade.rememberapp.data.local.MIGRATION_4_5
import com.example.uade.rememberapp.data.local.MIGRATION_5_6
import com.example.uade.rememberapp.data.local.MIGRATION_6_7
import com.example.uade.rememberapp.data.local.MIGRATION_7_8
import com.example.uade.rememberapp.data.local.MIGRATION_8_9
import com.example.uade.rememberapp.data.local.SeedTagsCallback
import com.example.uade.rememberapp.data.repository.ReminderRepositoryImpl
import com.example.uade.rememberapp.data.repository.TagRepositoryImpl
import com.example.uade.rememberapp.data.scheduler.AlarmReminderScheduler
import com.example.uade.rememberapp.data.storage.PhotoRepositoryImpl
import com.example.uade.rememberapp.domain.repository.PhotoRepository
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import com.example.uade.rememberapp.domain.usecase.GetReminderUseCase
import com.example.uade.rememberapp.domain.usecase.ImportPhotosUseCase
import com.example.uade.rememberapp.domain.usecase.MarkReminderDoneUseCase
import com.example.uade.rememberapp.domain.usecase.ObserveRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.RescheduleRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.RestoreReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SaveCoverUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SnoozeReminderUseCase
import com.example.uade.rememberapp.domain.usecase.TrashReminderUseCase
import com.example.uade.rememberapp.platform.audio.AudioRecorder
import com.example.uade.rememberapp.data.remote.GroqApiService
import com.example.uade.rememberapp.data.remote.GroqReminderServiceImpl
import com.example.uade.rememberapp.domain.repository.AiReminderService
import com.example.uade.rememberapp.domain.usecase.DetectSimilarRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.MergeProposedRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.ProcessVoiceWithAiUseCase
import com.example.uade.rememberapp.domain.usecase.SplitProposedReminderUseCase
import com.example.uade.rememberapp.platform.worker.WorkManagerScheduler
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            // Si no hay migración (ej. desde la versión 1 o 2), borra y recrea la base. Sirve
            // mientras la app no esté publicada. TODO: escribir migraciones antes de publicar.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(SeedTagsCallback())
            .build()
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(database.reminderDao())
    }

    val reminderScheduler: ReminderScheduler by lazy { AlarmReminderScheduler(appContext) }

    val saveReminderUseCase: SaveReminderUseCase by lazy { SaveReminderUseCase(reminderRepository, reminderScheduler) }
    val observeRemindersUseCase: ObserveRemindersUseCase by lazy { ObserveRemindersUseCase(reminderRepository) }
    val getReminderUseCase: GetReminderUseCase by lazy { GetReminderUseCase(reminderRepository) }
    val trashReminderUseCase: TrashReminderUseCase by lazy { TrashReminderUseCase(reminderRepository) }
    val restoreReminderUseCase: RestoreReminderUseCase by lazy { RestoreReminderUseCase(reminderRepository) }

    // Acciones de la notificación y reinicio del teléfono (alertas).
    val markReminderDoneUseCase: MarkReminderDoneUseCase by lazy {
        MarkReminderDoneUseCase(reminderRepository, reminderScheduler)
    }
    val snoozeReminderUseCase: SnoozeReminderUseCase by lazy {
        SnoozeReminderUseCase(reminderRepository, reminderScheduler)
    }
    val rescheduleRemindersUseCase: RescheduleRemindersUseCase by lazy {
        RescheduleRemindersUseCase(reminderRepository, reminderScheduler)
    }

    val photoRepository: PhotoRepository by lazy { PhotoRepositoryImpl(appContext) }

    val importPhotosUseCase: ImportPhotosUseCase by lazy { ImportPhotosUseCase(photoRepository) }
    val saveCoverUseCase: SaveCoverUseCase by lazy { SaveCoverUseCase(photoRepository) }

    // Audio Recorder, Groq AI & WorkManager
    val audioRecorder: AudioRecorder by lazy { AudioRecorder(appContext) }
    val workManagerScheduler: WorkManagerScheduler by lazy { WorkManagerScheduler(appContext) }

    val groqApiService: GroqApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApiService::class.java)
    }

    val aiReminderService: AiReminderService by lazy {
        GroqReminderServiceImpl(
            apiService = groqApiService,
            apiKey = BuildConfig.GROQ_API_KEY,
            gson = Gson(),
        )
    }

    val processVoiceWithAiUseCase: ProcessVoiceWithAiUseCase by lazy {
        ProcessVoiceWithAiUseCase(aiReminderService)
    }

    val mergeProposedRemindersUseCase: MergeProposedRemindersUseCase by lazy {
        MergeProposedRemindersUseCase()
    }

    val splitProposedReminderUseCase: SplitProposedReminderUseCase by lazy {
        SplitProposedReminderUseCase()
    }

    val detectSimilarRemindersUseCase: DetectSimilarRemindersUseCase by lazy {
        DetectSimilarRemindersUseCase()
    }

    /** Arranca con las etiquetas de ejemplo que carga [SeedTagsCallback] al crear la base. */
    val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(database.tagDao())
    }
}

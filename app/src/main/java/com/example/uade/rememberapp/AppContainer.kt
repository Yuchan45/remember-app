package com.example.uade.rememberapp

import android.content.Context
import androidx.room.Room
import com.example.uade.rememberapp.data.local.AppDatabase
import com.example.uade.rememberapp.data.repository.InMemoryTagRepository
import com.example.uade.rememberapp.data.repository.ReminderRepositoryImpl
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.usecase.ObserveRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.RestoreReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.domain.usecase.TrashReminderUseCase
import com.example.uade.rememberapp.platform.audio.AudioRecorder

import com.example.uade.rememberapp.data.remote.GroqApiService
import com.example.uade.rememberapp.data.remote.GroqReminderServiceImpl
import com.example.uade.rememberapp.domain.repository.AiReminderService
import com.example.uade.rememberapp.domain.usecase.DetectSimilarRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.MergeProposedRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.ProcessVoiceWithAiUseCase
import com.example.uade.rememberapp.platform.notification.NotificationHelper
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
            // Mientras la app no esté publicada: si cambia el esquema, borra y recrea la base.
            // TODO: escribir migraciones antes de publicar.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(database.reminderDao())
    }

    val saveReminderUseCase: SaveReminderUseCase by lazy { SaveReminderUseCase(reminderRepository) }
    val observeRemindersUseCase: ObserveRemindersUseCase by lazy { ObserveRemindersUseCase(reminderRepository) }
    val trashReminderUseCase: TrashReminderUseCase by lazy { TrashReminderUseCase(reminderRepository) }
    val restoreReminderUseCase: RestoreReminderUseCase by lazy { RestoreReminderUseCase(reminderRepository) }

    val audioRecorder: AudioRecorder by lazy { AudioRecorder(appContext) }
    val notificationHelper: NotificationHelper by lazy { NotificationHelper(appContext) }
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

    val detectSimilarRemindersUseCase: DetectSimilarRemindersUseCase by lazy {
        DetectSimilarRemindersUseCase()
    }

    // TODO: pasar a Room. Mientras tanto arranca con etiquetas de ejemplo.
    val tagRepository: TagRepository by lazy {
        InMemoryTagRepository(initial = SampleTags)
    }
}

private val SampleTags = listOf(
    Tag(id = 1, name = "Salud", colorArgb = 0xFF6FCF97),
    Tag(id = 5, name = "Ideas", colorArgb = 0xFFF2C94C),
)

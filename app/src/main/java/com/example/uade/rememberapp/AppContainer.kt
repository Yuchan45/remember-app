package com.example.uade.rememberapp

import android.content.Context
import androidx.room.Room
import com.example.uade.rememberapp.data.local.AppDatabase
import com.example.uade.rememberapp.data.local.DatabaseBackup
import com.example.uade.rememberapp.data.local.MIGRATION_3_4
import com.example.uade.rememberapp.data.local.MIGRATION_4_5
import com.example.uade.rememberapp.data.local.MIGRATION_5_6
import com.example.uade.rememberapp.data.local.MIGRATION_6_7
import com.example.uade.rememberapp.data.local.MIGRATION_7_8
import com.example.uade.rememberapp.data.local.MIGRATION_8_9
import com.example.uade.rememberapp.data.local.MIGRATION_9_10
import com.example.uade.rememberapp.data.local.MIGRATION_10_11
import com.example.uade.rememberapp.data.local.MIGRATION_11_12
import com.example.uade.rememberapp.data.local.MIGRATION_12_13
import com.example.uade.rememberapp.data.local.SeedTagsCallback
import com.example.uade.rememberapp.data.repository.PlaceRepositoryImpl
import com.example.uade.rememberapp.data.repository.ReminderRepositoryImpl
import com.example.uade.rememberapp.data.repository.TagRepositoryImpl
import com.example.uade.rememberapp.data.scheduler.AlarmReminderScheduler
import com.example.uade.rememberapp.data.scheduler.GeofenceRegistrarImpl
import com.example.uade.rememberapp.data.storage.PhotoRepositoryImpl
import com.example.uade.rememberapp.domain.repository.AddressSearchService
import com.example.uade.rememberapp.domain.repository.PhotoRepository
import com.example.uade.rememberapp.domain.repository.PlaceRepository
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.scheduler.GeofenceRegistrar
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import com.example.uade.rememberapp.domain.usecase.AdvanceRepeatingAlarmUseCase
import com.example.uade.rememberapp.domain.usecase.ArchiveReminderUseCase
import com.example.uade.rememberapp.domain.usecase.CompleteFromNotificationUseCase
import com.example.uade.rememberapp.domain.usecase.DeletePlaceUseCase
import com.example.uade.rememberapp.domain.usecase.DeleteRemindersForeverUseCase
import com.example.uade.rememberapp.domain.usecase.GetCurrentLocationUseCase
import com.example.uade.rememberapp.domain.usecase.GetReminderUseCase
import com.example.uade.rememberapp.domain.usecase.ImportPhotosUseCase
import com.example.uade.rememberapp.domain.usecase.MarkReminderDoneUseCase
import com.example.uade.rememberapp.domain.usecase.ObservePlacesUseCase
import com.example.uade.rememberapp.domain.usecase.ObserveRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.RescheduleRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.RestoreReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SaveCoverUseCase
import com.example.uade.rememberapp.domain.usecase.SavePlaceUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.domain.usecase.SearchAddressUseCase
import com.example.uade.rememberapp.domain.usecase.SnoozeReminderUseCase
import com.example.uade.rememberapp.domain.usecase.TrashReminderUseCase
import com.example.uade.rememberapp.platform.audio.AudioRecorder
import com.example.uade.rememberapp.platform.location.AndroidAddressSearchService
import com.example.uade.rememberapp.data.remote.GroqApiService
import com.example.uade.rememberapp.data.remote.GroqReminderServiceImpl
import com.example.uade.rememberapp.domain.repository.AiReminderService
import com.example.uade.rememberapp.domain.usecase.MergeProposedRemindersUseCase
import com.example.uade.rememberapp.domain.usecase.ProcessVoiceWithAiUseCase
import com.example.uade.rememberapp.domain.usecase.PurgeExpiredRemindersUseCase
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
        // Antes de que Room migre, una copia del archivo si cambió la versión: si algo sale mal,
        // los recordatorios se pueden recuperar.
        DatabaseBackup.backupIfVersionChanged(appContext, "hey.db", AppDatabase.VERSION)
        Room.databaseBuilder(appContext, AppDatabase::class.java, "hey.db")
            // Los cambios con migración escrita conservan los datos.
            .addMigrations(
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
            )
            // Sin fallbackToDestructiveMigration a propósito: si falta una migración (o se instala
            // una versión más vieja de la app, ej. al cambiar de rama), Room cierra la app con un
            // error en vez de borrar todos los recordatorios en silencio. Los datos quedan intactos
            // en el archivo y vuelven al instalar la versión correcta.
            .addCallback(SeedTagsCallback())
            .build()
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(database.reminderDao())
    }

    val reminderScheduler: ReminderScheduler by lazy { AlarmReminderScheduler(appContext) }

    val geofenceRegistrar: GeofenceRegistrar by lazy {
        GeofenceRegistrarImpl(appContext, placeRepository)
    }

    val saveReminderUseCase: SaveReminderUseCase by lazy {
        SaveReminderUseCase(reminderRepository, reminderScheduler, geofenceRegistrar)
    }
    val observeRemindersUseCase: ObserveRemindersUseCase by lazy { ObserveRemindersUseCase(reminderRepository) }
    val getReminderUseCase: GetReminderUseCase by lazy { GetReminderUseCase(reminderRepository) }
    val trashReminderUseCase: TrashReminderUseCase by lazy {
        TrashReminderUseCase(reminderRepository, saveReminderUseCase)
    }
    val archiveReminderUseCase: ArchiveReminderUseCase by lazy {
        ArchiveReminderUseCase(reminderRepository, saveReminderUseCase)
    }
    val restoreReminderUseCase: RestoreReminderUseCase by lazy {
        RestoreReminderUseCase(reminderRepository, saveReminderUseCase)
    }

    // Historial: borrar para siempre (a mano con "Vaciar" o solo, al vencer el plazo).
    val deleteRemindersForeverUseCase: DeleteRemindersForeverUseCase by lazy {
        DeleteRemindersForeverUseCase(reminderRepository, reminderScheduler, photoRepository)
    }
    val purgeExpiredRemindersUseCase: PurgeExpiredRemindersUseCase by lazy {
        PurgeExpiredRemindersUseCase(reminderRepository, deleteRemindersForeverUseCase)
    }

    // Acciones de la notificación y reinicio del teléfono (alertas).
    val markReminderDoneUseCase: MarkReminderDoneUseCase by lazy {
        MarkReminderDoneUseCase(reminderRepository, reminderScheduler)
    }
    val snoozeReminderUseCase: SnoozeReminderUseCase by lazy {
        SnoozeReminderUseCase(reminderRepository, reminderScheduler)
    }
    val rescheduleRemindersUseCase: RescheduleRemindersUseCase by lazy {
        RescheduleRemindersUseCase(reminderRepository, reminderScheduler, geofenceRegistrar)
    }
    val advanceRepeatingAlarmUseCase: AdvanceRepeatingAlarmUseCase by lazy {
        AdvanceRepeatingAlarmUseCase(reminderRepository, reminderScheduler)
    }
    val completeFromNotificationUseCase: CompleteFromNotificationUseCase by lazy {
        CompleteFromNotificationUseCase(reminderRepository, markReminderDoneUseCase)
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


    /** Arranca con las etiquetas de ejemplo que carga [SeedTagsCallback] al crear la base. */
    val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(database.tagDao())
    }

    /** Repositorio de lugares para recordatorios geolocalizados. */
    val placeRepository: PlaceRepository by lazy {
        PlaceRepositoryImpl(database.placeDao())
    }

    val addressSearchService: AddressSearchService by lazy {
        AndroidAddressSearchService(appContext)
    }

    val observePlacesUseCase: ObservePlacesUseCase by lazy { ObservePlacesUseCase(placeRepository) }
    val savePlaceUseCase: SavePlaceUseCase by lazy { SavePlaceUseCase(placeRepository) }
    val deletePlaceUseCase: DeletePlaceUseCase by lazy { DeletePlaceUseCase(placeRepository) }
    val searchAddressUseCase: SearchAddressUseCase by lazy { SearchAddressUseCase(addressSearchService) }
    val getCurrentLocationUseCase: GetCurrentLocationUseCase by lazy { GetCurrentLocationUseCase(addressSearchService) }
}

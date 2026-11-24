package com.example.uade.rememberapp.data.repository

import android.content.Context
import com.example.uade.rememberapp.data.local.AppDatabase
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.AlarmRepeat
import com.example.uade.rememberapp.domain.model.AppSettings
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.PlaceKind
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderStatus
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.backup.AlarmBackupDto
import com.example.uade.rememberapp.domain.model.backup.BackupMetadata
import com.example.uade.rememberapp.domain.model.backup.ChecklistItemBackupDto
import com.example.uade.rememberapp.domain.model.backup.HeyBackupDto
import com.example.uade.rememberapp.domain.model.backup.ImportResult
import com.example.uade.rememberapp.domain.model.backup.NotificationActionBackupDto
import com.example.uade.rememberapp.domain.model.backup.PlaceAlertBackupDto
import com.example.uade.rememberapp.domain.model.backup.PlaceBackupDto
import com.example.uade.rememberapp.domain.model.backup.ReminderBackupDto
import com.example.uade.rememberapp.domain.model.backup.TagBackupDto
import com.example.uade.rememberapp.domain.repository.DataManagementRepository
import com.example.uade.rememberapp.domain.repository.PlaceRepository
import com.example.uade.rememberapp.domain.repository.ReminderRepository
import com.example.uade.rememberapp.domain.repository.SettingsRepository
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.scheduler.GeofenceRegistrar
import com.example.uade.rememberapp.domain.scheduler.ReminderScheduler
import com.example.uade.rememberapp.domain.scheduler.cancelAll
import com.example.uade.rememberapp.domain.usecase.RescheduleRemindersUseCase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

/**
 * Implementación de [DataManagementRepository] para realizar copias de seguridad,
 * exportación/importación JSON, limpieza de almacenamiento y restablecimiento.
 */
class DataManagementRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase,
    private val reminderRepository: ReminderRepository,
    private val tagRepository: TagRepository,
    private val placeRepository: PlaceRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val geofenceRegistrar: GeofenceRegistrar,
    private val rescheduleRemindersUseCase: RescheduleRemindersUseCase,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create(),
) : DataManagementRepository {

    private val backupDir: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    override suspend fun createLocalBackup(): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val reminders = reminderRepository.observeAll().first()
            val tags = tagRepository.observeAll().first()
            val places = placeRepository.observeAll().first()
            val placesMapById = places.associateBy { it.id }

            val dto = HeyBackupDto(
                version = 1,
                exportedAtMillis = System.currentTimeMillis(),
                reminders = reminders.map { it.toBackupDto(placesMapById) },
                places = places.map { it.toBackupDto() },
                tags = tags.map { TagBackupDto(name = it.name, colorArgb = it.colorArgb) },
            )

            val json = gson.toJson(dto)
            val file = File(backupDir, "hey_backup_${System.currentTimeMillis()}.json")
            file.writeText(json)

            BackupMetadata(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSizeBytes = file.length(),
                createdAtMillis = file.lastModified(),
                remindersCount = dto.reminders.size,
                placesCount = dto.places.size,
                tagsCount = dto.tags.size,
            )
        }
    }

    override suspend fun getLatestLocalBackup(): BackupMetadata? = withContext(Dispatchers.IO) {
        val files = backupDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
        val latest = files?.maxByOrNull { it.lastModified() } ?: return@withContext null

        runCatching {
            val json = latest.readText()
            val dto = gson.fromJson(json, HeyBackupDto::class.java)
            BackupMetadata(
                fileName = latest.name,
                filePath = latest.absolutePath,
                fileSizeBytes = latest.length(),
                createdAtMillis = latest.lastModified(),
                remindersCount = dto.reminders.size,
                placesCount = dto.places.size,
                tagsCount = dto.tags.size,
            )
        }.getOrNull()
    }

    override suspend fun restoreFromLatestLocalBackup(): Result<ImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val files = backupDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            val latest = files?.maxByOrNull { it.lastModified() }
                ?: throw IllegalStateException("No se encontraron copias de seguridad locales.")

            val json = latest.readText()
            importBackupFromJson(json).getOrThrow()
        }
    }

    override suspend fun exportBackupFile(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val backup = createLocalBackup().getOrThrow()
            File(backup.filePath)
        }
    }

    override suspend fun importBackupFromJson(jsonString: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val dto = gson.fromJson(jsonString, HeyBackupDto::class.java)
                ?: throw IllegalArgumentException("Formato JSON de respaldo no válido.")

            // 1. Importar etiquetas faltantes
            val existingTags = tagRepository.observeAll().first().toMutableList()
            var tagsImported = 0
            for (tagDto in dto.tags) {
                if (existingTags.none { it.name.equals(tagDto.name, ignoreCase = true) }) {
                    val created = tagRepository.create(tagDto.name, tagDto.colorArgb)
                    existingTags.add(created)
                    tagsImported++
                }
            }

            // 2. Importar lugares faltantes
            val existingPlaces = placeRepository.observeAll().first().toMutableList()
            var placesImported = 0
            for (placeDto in dto.places) {
                if (existingPlaces.none { it.name.equals(placeDto.name, ignoreCase = true) }) {
                    val kind = runCatching { PlaceKind.valueOf(placeDto.kind) }.getOrDefault(PlaceKind.Other)
                    val newPlace = Place(
                        name = placeDto.name,
                        latitude = placeDto.latitude,
                        longitude = placeDto.longitude,
                        radiusMeters = placeDto.radiusMeters,
                        kind = kind,
                    )
                    val id = placeRepository.save(newPlace)
                    existingPlaces.add(newPlace.copy(id = id))
                    placesImported++
                }
            }

            val tagsMap = existingTags.associateBy { it.name.lowercase() }
            val placesMapByName = existingPlaces.associateBy { it.name.lowercase() }

            // 3. Importar recordatorios
            var remindersImported = 0
            for (reminderDto in dto.reminders) {
                val matchedTags = reminderDto.tagNames.mapNotNull { tagsMap[it.lowercase()] }
                val matchedPlaces = reminderDto.placeAlerts.mapNotNull { alertDto ->
                    placesMapByName[alertDto.placeName.lowercase()]?.let { place ->
                        val event = runCatching { PlaceAlertEvent.valueOf(alertDto.event) }
                            .getOrDefault(PlaceAlertEvent.Arrive)
                        PlaceAlert(
                            placeId = place.id,
                            event = event,
                        )
                    }
                }

                val alarms = reminderDto.alarms.map {
                    Alarm(
                        at = Instant.ofEpochMilli(it.atEpochMilli),
                        isSnooze = it.isSnooze,
                        repeat = runCatching { AlarmRepeat.valueOf(it.repeat ?: "") }.getOrDefault(AlarmRepeat.None),
                    )
                }

                val checklistItems = reminderDto.checklistItems.map {
                    ChecklistItem(
                        text = it.text,
                        note = it.note,
                        isChecked = it.isChecked,
                    )
                }

                val action: NotificationAction? = reminderDto.action?.let { actDto ->
                    when (actDto.type) {
                        "Call" -> NotificationAction.Call(phone = actDto.target)
                        "Email" -> NotificationAction.Email(to = actDto.target, subject = actDto.extra)
                        "Message" -> {
                            val ch = runCatching { MessageChannel.valueOf(actDto.channel ?: "") }
                                .getOrDefault(MessageChannel.Sms)
                            NotificationAction.Message(channel = ch, to = actDto.target, text = actDto.extra)
                        }
                        else -> null
                    }
                }

                val type = runCatching { ReminderType.valueOf(reminderDto.type) }.getOrDefault(ReminderType.Note)
                val status = runCatching { ReminderStatus.valueOf(reminderDto.status) }.getOrDefault(ReminderStatus.Active)
                val importance = runCatching { Importance.valueOf(reminderDto.importance) }.getOrDefault(Importance.Default)

                val reminder = Reminder(
                    type = type,
                    title = reminderDto.title,
                    description = reminderDto.description,
                    isDone = reminderDto.isDone,
                    status = status,
                    importance = importance,
                    tags = matchedTags,
                    places = matchedPlaces,
                    alarms = alarms,
                    items = checklistItems,
                    action = action,
                    statusChangedAt = reminderDto.statusChangedAtEpochMilli?.let(Instant::ofEpochMilli),
                    transcript = reminderDto.transcript,
                    createdAt = Instant.ofEpochMilli(reminderDto.createdAtEpochMilli),
                )

                reminderRepository.save(reminder)
                remindersImported++
            }

            // 4. Reprogramar alarmas y geocercas
            rescheduleRemindersUseCase()

            ImportResult(
                remindersImported = remindersImported,
                placesImported = placesImported,
                tagsImported = tagsImported,
            )
        }
    }

    override fun getCacheSizeBytes(): Long {
        return calculateDirectorySize(context.cacheDir)
    }

    override suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        val initialSize = getCacheSizeBytes()
        deleteDirectoryContents(context.cacheDir)
        initialSize
    }

    override suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Cancelar todas las alarmas y geocercas activas
            val allReminders = reminderRepository.observeAll().first()
            for (reminder in allReminders) {
                reminderScheduler.cancelAll(reminder.id, reminder.alarms)
                for (placeAlert in reminder.places) {
                    geofenceRegistrar.remove(placeAlert.placeId)
                }
            }

            // 2. Limpiar base de datos SQLite / Room
            database.clearAllTables()

            // 3. Re-crear etiquetas por defecto
            tagRepository.create("Salud", 0xFF6FCF97)
            tagRepository.create("Ideas", 0xFFF2C94C)

            // 4. Limpiar fotos y notas de voz locales
            deleteDirectoryContents(File(context.filesDir, "photos"))
            deleteDirectoryContents(File(context.filesDir, "audios"))
            deleteDirectoryContents(backupDir)

            // 5. Limpiar caché
            deleteDirectoryContents(context.cacheDir)

            // 6. Restablecer preferencias
            context.getSharedPreferences("hey_app_settings", Context.MODE_PRIVATE)
                .edit().clear().apply()

            settingsRepository.setThemeMode(AppSettings().themeMode)
            settingsRepository.setPalette(AppSettings().palette)
            settingsRepository.setFontScale(AppSettings().fontScale)
            settingsRepository.setFontFamily(AppSettings().fontFamily)
            settingsRepository.setUseGradientBackground(AppSettings().useGradientBackground)
            settingsRepository.setNotificationSettings(AppSettings().notifications)
            settingsRepository.setSecuritySettings(AppSettings().security)

            true
        }.getOrDefault(false)
    }

    private fun calculateDirectorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
        }
        return size
    }

    private fun deleteDirectoryContents(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirectoryContents(file)
            }
            file.delete()
        }
    }

    private fun Reminder.toBackupDto(placesMapById: Map<Long, Place>): ReminderBackupDto = ReminderBackupDto(
        type = type.name,
        title = title,
        description = description,
        isDone = isDone,
        status = status.name,
        importance = importance.name,
        createdAtEpochMilli = createdAt.toEpochMilli(),
        tagNames = tags.map { it.name },
        alarms = alarms.map {
            AlarmBackupDto(atEpochMilli = it.at.toEpochMilli(), isSnooze = it.isSnooze, repeat = it.repeat.name)
        },
        statusChangedAtEpochMilli = statusChangedAt?.toEpochMilli(),
        transcript = transcript,
        placeAlerts = places.mapNotNull { alert ->
            placesMapById[alert.placeId]?.let { place ->
                PlaceAlertBackupDto(
                    placeName = place.name,
                    event = alert.event.name,
                )
            }
        },
        checklistItems = items.map {
            ChecklistItemBackupDto(
                text = it.text,
                note = it.note,
                isChecked = it.isChecked,
            )
        },
        action = action?.let { act ->
            when (act) {
                is NotificationAction.Call -> NotificationActionBackupDto(
                    type = "Call",
                    target = act.phone,
                )
                is NotificationAction.Email -> NotificationActionBackupDto(
                    type = "Email",
                    target = act.to,
                    extra = act.subject,
                )
                is NotificationAction.Message -> NotificationActionBackupDto(
                    type = "Message",
                    target = act.to,
                    extra = act.text,
                    channel = act.channel.name,
                )
            }
        },
    )

    private fun Place.toBackupDto(): PlaceBackupDto = PlaceBackupDto(
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        kind = kind.name,
    )
}

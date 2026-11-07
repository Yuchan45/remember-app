package com.example.uade.rememberapp.domain.model.backup

/**
 * Metadatos descriptivos de una copia de seguridad local.
 */
data class BackupMetadata(
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val createdAtMillis: Long,
    val remindersCount: Int,
    val placesCount: Int,
    val tagsCount: Int,
)

/**
 * Resultado de una operación de restauración o importación de datos.
 */
data class ImportResult(
    val remindersImported: Int,
    val placesImported: Int,
    val tagsImported: Int,
)

/**
 * Estructura raíz serializable para la exportación y respaldo completo de datos de Hey!.
 */
data class HeyBackupDto(
    val version: Int = 1,
    val app: String = "Hey!",
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val reminders: List<ReminderBackupDto> = emptyList(),
    val places: List<PlaceBackupDto> = emptyList(),
    val tags: List<TagBackupDto> = emptyList(),
)

data class TagBackupDto(
    val name: String,
    val colorArgb: Long,
)

data class PlaceBackupDto(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 150,
    val kind: String = "Other",
)

data class ReminderBackupDto(
    val type: String,
    val title: String?,
    val description: String?,
    val isDone: Boolean = false,
    val status: String = "Active",
    val importance: String = "Default",
    val createdAtEpochMilli: Long,
    val tagNames: List<String> = emptyList(),
    val alarms: List<AlarmBackupDto> = emptyList(),
    val placeAlerts: List<PlaceAlertBackupDto> = emptyList(),
    val checklistItems: List<ChecklistItemBackupDto> = emptyList(),
    val action: NotificationActionBackupDto? = null,
)

data class AlarmBackupDto(
    val atEpochMilli: Long,
    val isSnooze: Boolean = false,
)

data class PlaceAlertBackupDto(
    val placeName: String,
    val event: String = "Arrive",
)

data class ChecklistItemBackupDto(
    val text: String,
    val note: String? = null,
    val isChecked: Boolean = false,
)

data class NotificationActionBackupDto(
    val type: String,
    val target: String,
    val extra: String? = null,
    val channel: String? = null,
)

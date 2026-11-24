package com.example.uade.rememberapp.ui.settings

import com.example.uade.rememberapp.domain.model.AlarmSoundType
import com.example.uade.rememberapp.domain.model.AppFontFamily
import com.example.uade.rememberapp.domain.model.AppLockType
import com.example.uade.rememberapp.domain.model.AppPalette
import com.example.uade.rememberapp.domain.model.AppSettings
import com.example.uade.rememberapp.domain.model.FontScale
import com.example.uade.rememberapp.domain.model.LockTimeout
import com.example.uade.rememberapp.domain.model.NotificationSettings
import com.example.uade.rememberapp.domain.model.SecuritySettings
import com.example.uade.rememberapp.domain.model.ThemeMode
import com.example.uade.rememberapp.domain.model.backup.BackupMetadata

/**
 * Estado de la comprobación de actualizaciones de la app.
 */
enum class UpdateCheckStatus {
    Idle,
    Checking,
    UpToDate,
}

/**
 * Estado inmutable de la pantalla de Ajustes.
 */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isAppearanceExpanded: Boolean = true,
    val isNotificationsExpanded: Boolean = true,
    val isSecurityExpanded: Boolean = true,
    val isDataExpanded: Boolean = true,
    val isAboutExpanded: Boolean = true,
    val updateCheckStatus: UpdateCheckStatus = UpdateCheckStatus.Idle,
    val latestBackup: BackupMetadata? = null,
    val cacheSizeBytes: Long = 0L,
    val dataActionMessage: String? = null,
    val isDataOperating: Boolean = false,
) {
    val areAnySectionsExpanded: Boolean
        get() = isAppearanceExpanded || isNotificationsExpanded || isSecurityExpanded || isDataExpanded || isAboutExpanded

    // Apariencia
    val themeMode: ThemeMode get() = settings.themeMode
    val usePureBlack: Boolean get() = settings.usePureBlack
    val palette: AppPalette get() = settings.palette
    val fontScale: FontScale get() = settings.fontScale
    val fontFamily: AppFontFamily get() = settings.fontFamily
    val useGradientBackground: Boolean get() = settings.useGradientBackground

    // Notificaciones
    val notifications: NotificationSettings get() = settings.notifications
    val timeAlertsEnabled: Boolean get() = notifications.timeAlertsEnabled
    val placeAlertsEnabled: Boolean get() = notifications.placeAlertsEnabled
    val highPriorityOnly: Boolean get() = notifications.highPriorityOnly
    val reliableRemindersEnabled: Boolean get() = notifications.reliableRemindersEnabled
    val notifyOverdueReminders: Boolean get() = notifications.notifyOverdueReminders
    val alarmSoundType: AlarmSoundType get() = notifications.alarmSoundType
    val vibrationEnabled: Boolean get() = notifications.vibrationEnabled

    // Seguridad
    val security: SecuritySettings get() = settings.security
    val isAppLockEnabled: Boolean get() = security.isAppLockEnabled
    val lockType: AppLockType get() = security.lockType
    val biometricUnlockEnabled: Boolean get() = security.biometricUnlockEnabled
    val lockTimeout: LockTimeout get() = security.lockTimeout
    val hasConfiguredPassword: Boolean get() = security.hasConfiguredPassword
}

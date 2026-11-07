package com.example.uade.rememberapp.data.repository

import android.content.Context
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
import com.example.uade.rememberapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.MessageDigest

/**
 * Implementación de [SettingsRepository] basada en [android.content.SharedPreferences].
 *
 * Mantiene un [MutableStateFlow] en memoria sincronizado con el almacenamiento para emitir
 * cambios instantáneamente a la UI sin bloqueos.
 */
class SettingsRepositoryImpl(
    context: Context,
) : SettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())

    override fun getSettings(): Flow<AppSettings> = _settings.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.update { it.copy(themeMode = mode) }
    }

    override suspend fun setUsePureBlack(usePureBlack: Boolean) {
        prefs.edit().putBoolean(KEY_USE_PURE_BLACK, usePureBlack).apply()
        _settings.update { it.copy(usePureBlack = usePureBlack) }
    }

    override suspend fun setPalette(palette: AppPalette) {
        prefs.edit().putString(KEY_PALETTE, palette.name).apply()
        _settings.update { it.copy(palette = palette) }
    }

    override suspend fun setFontScale(scale: FontScale) {
        prefs.edit().putString(KEY_FONT_SCALE, scale.name).apply()
        _settings.update { it.copy(fontScale = scale) }
    }

    override suspend fun setFontFamily(fontFamily: AppFontFamily) {
        prefs.edit().putString(KEY_FONT_FAMILY, fontFamily.name).apply()
        _settings.update { it.copy(fontFamily = fontFamily) }
    }

    override suspend fun setUseGradientBackground(useGradientBackground: Boolean) {
        prefs.edit().putBoolean(KEY_USE_GRADIENT_BG, useGradientBackground).apply()
        _settings.update { it.copy(useGradientBackground = useGradientBackground) }
    }

    override suspend fun setNotificationSettings(notifications: NotificationSettings) {
        prefs.edit()
            .putBoolean(KEY_NOTIF_TIME_ALERTS, notifications.timeAlertsEnabled)
            .putBoolean(KEY_NOTIF_PLACE_ALERTS, notifications.placeAlertsEnabled)
            .putBoolean(KEY_NOTIF_HIGH_PRIORITY_ONLY, notifications.highPriorityOnly)
            .putBoolean(KEY_NOTIF_RELIABLE, notifications.reliableRemindersEnabled)
            .putBoolean(KEY_NOTIF_OVERDUE, notifications.notifyOverdueReminders)
            .putString(KEY_NOTIF_ALARM_SOUND, notifications.alarmSoundType.name)
            .putBoolean(KEY_NOTIF_VIBRATION, notifications.vibrationEnabled)
            .apply()
        _settings.update { it.copy(notifications = notifications) }
    }

    override suspend fun setTimeAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_TIME_ALERTS, enabled).apply()
        _settings.update { it.copy(notifications = it.notifications.copy(timeAlertsEnabled = enabled)) }
    }

    override suspend fun setPlaceAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_PLACE_ALERTS, enabled).apply()
        _settings.update { it.copy(notifications = it.notifications.copy(placeAlertsEnabled = enabled)) }
    }

    override suspend fun setHighPriorityOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_HIGH_PRIORITY_ONLY, enabled).apply()
        _settings.update { it.copy(notifications = it.notifications.copy(highPriorityOnly = enabled)) }
    }

    override suspend fun setReliableRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_RELIABLE, enabled).apply()
        _settings.update { it.copy(notifications = it.notifications.copy(reliableRemindersEnabled = enabled)) }
    }

    override suspend fun setNotifyOverdueReminders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_OVERDUE, enabled).apply()
        _settings.update { it.copy(notifications = it.notifications.copy(notifyOverdueReminders = enabled)) }
    }

    override suspend fun setAlarmSoundType(soundType: AlarmSoundType) {
        prefs.edit().putString(KEY_NOTIF_ALARM_SOUND, soundType.name).apply()
        _settings.update { it.copy(notifications = it.notifications.copy(alarmSoundType = soundType)) }
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_VIBRATION, enabled).apply()
        _settings.update { it.copy(notifications = it.notifications.copy(vibrationEnabled = enabled)) }
    }

    // --- Seguridad ---

    override suspend fun setSecuritySettings(security: SecuritySettings) {
        prefs.edit()
            .putBoolean(KEY_SECURITY_APP_LOCK, security.isAppLockEnabled)
            .putString(KEY_SECURITY_LOCK_TYPE, security.lockType.name)
            .putBoolean(KEY_SECURITY_BIOMETRIC, security.biometricUnlockEnabled)
            .putString(KEY_SECURITY_TIMEOUT, security.lockTimeout.name)
            .putString(KEY_SECURITY_PASSWORD_HASH, security.passwordHash)
            .apply()
        _settings.update { it.copy(security = security) }
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURITY_APP_LOCK, enabled).apply()
        _settings.update { it.copy(security = it.security.copy(isAppLockEnabled = enabled)) }
    }

    override suspend fun setAppLockType(lockType: AppLockType) {
        prefs.edit().putString(KEY_SECURITY_LOCK_TYPE, lockType.name).apply()
        _settings.update { it.copy(security = it.security.copy(lockType = lockType)) }
    }

    override suspend fun setBiometricUnlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURITY_BIOMETRIC, enabled).apply()
        _settings.update { it.copy(security = it.security.copy(biometricUnlockEnabled = enabled)) }
    }

    override suspend fun setLockTimeout(timeout: LockTimeout) {
        prefs.edit().putString(KEY_SECURITY_TIMEOUT, timeout.name).apply()
        _settings.update { it.copy(security = it.security.copy(lockTimeout = timeout)) }
    }

    override suspend fun setLockPassword(password: String) {
        val hash = if (password.isBlank()) "" else hashPassword(password)
        prefs.edit().putString(KEY_SECURITY_PASSWORD_HASH, hash).apply()
        _settings.update { it.copy(security = it.security.copy(passwordHash = hash)) }
    }

    override fun verifyLockPassword(password: String): Boolean {
        val currentHash = _settings.value.security.passwordHash
        if (currentHash.isBlank()) return true
        return hashPassword(password) == currentHash
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(("hey_salt_" + password).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun readSettings(): AppSettings {
        val themeModeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.System.name)
        val themeMode = runCatching { ThemeMode.valueOf(themeModeStr ?: "") }.getOrDefault(ThemeMode.System)

        val usePureBlack = prefs.getBoolean(KEY_USE_PURE_BLACK, false)

        val paletteStr = prefs.getString(KEY_PALETTE, AppPalette.HeyBlue.name)
        val palette = runCatching { AppPalette.valueOf(paletteStr ?: "") }.getOrDefault(AppPalette.HeyBlue)

        val fontScaleStr = prefs.getString(KEY_FONT_SCALE, FontScale.Default.name)
        val fontScale = runCatching { FontScale.valueOf(fontScaleStr ?: "") }.getOrDefault(FontScale.Default)

        val fontFamilyStr = prefs.getString(KEY_FONT_FAMILY, AppFontFamily.System.name)
        val fontFamily = runCatching { AppFontFamily.valueOf(fontFamilyStr ?: "") }.getOrDefault(AppFontFamily.System)

        val useGradientBg = prefs.getBoolean(KEY_USE_GRADIENT_BG, true)

        val timeAlerts = prefs.getBoolean(KEY_NOTIF_TIME_ALERTS, true)
        val placeAlerts = prefs.getBoolean(KEY_NOTIF_PLACE_ALERTS, true)
        val highPriorityOnly = prefs.getBoolean(KEY_NOTIF_HIGH_PRIORITY_ONLY, false)
        val reliable = prefs.getBoolean(KEY_NOTIF_RELIABLE, true)
        val overdue = prefs.getBoolean(KEY_NOTIF_OVERDUE, true)
        val alarmSoundStr = prefs.getString(KEY_NOTIF_ALARM_SOUND, AlarmSoundType.ShortChime.name)
        val alarmSound = runCatching { AlarmSoundType.valueOf(alarmSoundStr ?: "") }.getOrDefault(AlarmSoundType.ShortChime)
        val vibration = prefs.getBoolean(KEY_NOTIF_VIBRATION, true)

        val isAppLock = prefs.getBoolean(KEY_SECURITY_APP_LOCK, false)
        val lockTypeStr = prefs.getString(KEY_SECURITY_LOCK_TYPE, AppLockType.Pin.name)
        val lockType = runCatching { AppLockType.valueOf(lockTypeStr ?: "") }.getOrDefault(AppLockType.Pin)
        val biometric = prefs.getBoolean(KEY_SECURITY_BIOMETRIC, false)
        val timeoutStr = prefs.getString(KEY_SECURITY_TIMEOUT, LockTimeout.Immediately.name)
        val lockTimeout = runCatching { LockTimeout.valueOf(timeoutStr ?: "") }.getOrDefault(LockTimeout.Immediately)
        val passwordHash = prefs.getString(KEY_SECURITY_PASSWORD_HASH, "") ?: ""

        return AppSettings(
            themeMode = themeMode,
            usePureBlack = usePureBlack,
            palette = palette,
            fontScale = fontScale,
            fontFamily = fontFamily,
            useGradientBackground = useGradientBg,
            notifications = NotificationSettings(
                timeAlertsEnabled = timeAlerts,
                placeAlertsEnabled = placeAlerts,
                highPriorityOnly = highPriorityOnly,
                reliableRemindersEnabled = reliable,
                notifyOverdueReminders = overdue,
                alarmSoundType = alarmSound,
                vibrationEnabled = vibration,
            ),
            security = SecuritySettings(
                isAppLockEnabled = isAppLock,
                lockType = lockType,
                biometricUnlockEnabled = biometric,
                lockTimeout = lockTimeout,
                passwordHash = passwordHash,
            ),
        )
    }

    companion object {
        private const val PREFS_NAME = "hey_app_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_USE_PURE_BLACK = "use_pure_black"
        private const val KEY_PALETTE = "palette"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_USE_GRADIENT_BG = "use_gradient_bg"

        private const val KEY_NOTIF_TIME_ALERTS = "notif_time_alerts"
        private const val KEY_NOTIF_PLACE_ALERTS = "notif_place_alerts"
        private const val KEY_NOTIF_HIGH_PRIORITY_ONLY = "notif_high_priority_only"
        private const val KEY_NOTIF_RELIABLE = "notif_reliable"
        private const val KEY_NOTIF_OVERDUE = "notif_overdue"
        private const val KEY_NOTIF_ALARM_SOUND = "notif_alarm_sound"
        private const val KEY_NOTIF_VIBRATION = "notif_vibration"

        private const val KEY_SECURITY_APP_LOCK = "security_app_lock"
        private const val KEY_SECURITY_LOCK_TYPE = "security_lock_type"
        private const val KEY_SECURITY_BIOMETRIC = "security_biometric"
        private const val KEY_SECURITY_TIMEOUT = "security_timeout"
        private const val KEY_SECURITY_PASSWORD_HASH = "security_password_hash"
    }
}

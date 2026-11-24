package com.example.uade.rememberapp.domain.repository

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
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de ajustes y preferencias del usuario.
 */
interface SettingsRepository {
    /** Emite las preferencias actuales y cualquier cambio posterior. */
    fun getSettings(): Flow<AppSettings>

    // Apariencia
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setUsePureBlack(usePureBlack: Boolean)
    suspend fun setPalette(palette: AppPalette)
    suspend fun setFontScale(scale: FontScale)
    suspend fun setFontFamily(fontFamily: AppFontFamily)
    suspend fun setUseGradientBackground(useGradientBackground: Boolean)

    // Notificaciones
    suspend fun setNotificationSettings(notifications: NotificationSettings)
    suspend fun setTimeAlertsEnabled(enabled: Boolean)
    suspend fun setPlaceAlertsEnabled(enabled: Boolean)
    suspend fun setHighPriorityOnly(enabled: Boolean)
    suspend fun setReliableRemindersEnabled(enabled: Boolean)
    suspend fun setNotifyOverdueReminders(enabled: Boolean)
    suspend fun setAlarmSoundType(soundType: AlarmSoundType)
    suspend fun setVibrationEnabled(enabled: Boolean)

    // Seguridad
    suspend fun setSecuritySettings(security: SecuritySettings)
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setAppLockType(lockType: AppLockType)
    suspend fun setBiometricUnlockEnabled(enabled: Boolean)
    suspend fun setLockTimeout(timeout: LockTimeout)
    suspend fun setLockPassword(password: String)
    fun verifyLockPassword(password: String): Boolean
}

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
import com.example.uade.rememberapp.domain.model.backup.ImportResult
import com.example.uade.rememberapp.domain.repository.DataManagementRepository
import com.example.uade.rememberapp.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsViewModelTest {

    private class FakeSettingsRepository(
        initial: AppSettings = AppSettings(),
    ) : SettingsRepository {
        private val state = MutableStateFlow(initial)

        override fun getSettings(): Flow<AppSettings> = state

        override suspend fun setThemeMode(mode: ThemeMode) {
            state.update { it.copy(themeMode = mode) }
        }

        override suspend fun setUsePureBlack(usePureBlack: Boolean) {
            state.update { it.copy(usePureBlack = usePureBlack) }
        }

        override suspend fun setPalette(palette: AppPalette) {
            state.update { it.copy(palette = palette) }
        }

        override suspend fun setFontScale(scale: FontScale) {
            state.update { it.copy(fontScale = scale) }
        }

        override suspend fun setFontFamily(fontFamily: AppFontFamily) {
            state.update { it.copy(fontFamily = fontFamily) }
        }

        override suspend fun setUseGradientBackground(useGradientBackground: Boolean) {
            state.update { it.copy(useGradientBackground = useGradientBackground) }
        }

        override suspend fun setNotificationSettings(notifications: NotificationSettings) {
            state.update { it.copy(notifications = notifications) }
        }

        override suspend fun setTimeAlertsEnabled(enabled: Boolean) {
            state.update { it.copy(notifications = it.notifications.copy(timeAlertsEnabled = enabled)) }
        }

        override suspend fun setPlaceAlertsEnabled(enabled: Boolean) {
            state.update { it.copy(notifications = it.notifications.copy(placeAlertsEnabled = enabled)) }
        }

        override suspend fun setHighPriorityOnly(enabled: Boolean) {
            state.update { it.copy(notifications = it.notifications.copy(highPriorityOnly = enabled)) }
        }

        override suspend fun setReliableRemindersEnabled(enabled: Boolean) {
            state.update { it.copy(notifications = it.notifications.copy(reliableRemindersEnabled = enabled)) }
        }

        override suspend fun setNotifyOverdueReminders(enabled: Boolean) {
            state.update { it.copy(notifications = it.notifications.copy(notifyOverdueReminders = enabled)) }
        }

        override suspend fun setAlarmSoundType(soundType: AlarmSoundType) {
            state.update { it.copy(notifications = it.notifications.copy(alarmSoundType = soundType)) }
        }

        override suspend fun setVibrationEnabled(enabled: Boolean) {
            state.update { it.copy(notifications = it.notifications.copy(vibrationEnabled = enabled)) }
        }

        override suspend fun setSecuritySettings(security: SecuritySettings) {
            state.update { it.copy(security = security) }
        }

        override suspend fun setAppLockEnabled(enabled: Boolean) {
            state.update { it.copy(security = it.security.copy(isAppLockEnabled = enabled)) }
        }

        override suspend fun setAppLockType(lockType: AppLockType) {
            state.update { it.copy(security = it.security.copy(lockType = lockType)) }
        }

        override suspend fun setBiometricUnlockEnabled(enabled: Boolean) {
            state.update { it.copy(security = it.security.copy(biometricUnlockEnabled = enabled)) }
        }

        override suspend fun setLockTimeout(timeout: LockTimeout) {
            state.update { it.copy(security = it.security.copy(lockTimeout = timeout)) }
        }

        override suspend fun setLockPassword(password: String) {
            state.update { it.copy(security = it.security.copy(passwordHash = "hash_$password")) }
        }

        override fun verifyLockPassword(password: String): Boolean {
            val hash = state.value.security.passwordHash
            return hash.isEmpty() || hash == "hash_$password"
        }
    }

    private class FakeDataManagementRepository : DataManagementRepository {
        var backupCreated = false
        var backupRestored = false
        var cacheCleared = false
        var allDataCleared = false
        var jsonImported = false

        override suspend fun createLocalBackup(): Result<BackupMetadata> {
            backupCreated = true
            return Result.success(
                BackupMetadata(
                    fileName = "test_backup.json",
                    filePath = "/fake/test_backup.json",
                    fileSizeBytes = 2048,
                    createdAtMillis = 1000L,
                    remindersCount = 5,
                    placesCount = 2,
                    tagsCount = 3,
                ),
            )
        }

        override suspend fun getLatestLocalBackup(): BackupMetadata? = null

        override suspend fun restoreFromLatestLocalBackup(): Result<ImportResult> {
            backupRestored = true
            return Result.success(ImportResult(5, 2, 3))
        }

        override suspend fun exportBackupFile(): Result<File> = Result.success(File("/fake/export.json"))

        override suspend fun importBackupFromJson(jsonString: String): Result<ImportResult> {
            jsonImported = true
            return Result.success(ImportResult(3, 1, 2))
        }

        override fun getCacheSizeBytes(): Long = 1024 * 1024 * 5

        override suspend fun clearCache(): Long {
            cacheCleared = true
            return 1024 * 1024 * 5
        }

        override suspend fun clearAllData(): Boolean {
            allDataCleared = true
            return true
        }
    }

    private fun createViewModel(
        repo: SettingsRepository,
        dataRepo: DataManagementRepository = FakeDataManagementRepository(),
    ): SettingsViewModel {
        return SettingsViewModel(
            settingsRepository = repo,
            dataManagementRepository = dataRepo,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )
    }

    @Test
    fun `estado inicial arranca con apariencia, notificaciones, seguridad, datos y acerca de expandidas y valores por defecto`() {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        val state = viewModel.uiState.value
        assertTrue(state.isAppearanceExpanded)
        assertTrue(state.isNotificationsExpanded)
        assertTrue(state.isSecurityExpanded)
        assertTrue(state.isDataExpanded)
        assertTrue(state.isAboutExpanded)
        assertEquals(UpdateCheckStatus.Idle, state.updateCheckStatus)
        assertEquals(ThemeMode.System, state.themeMode)
        assertEquals(AppPalette.HeyBlue, state.palette)
        assertEquals(FontScale.Default, state.fontScale)
        assertEquals(AppFontFamily.System, state.fontFamily)
        assertTrue(state.useGradientBackground)

        // Notificaciones por defecto
        assertTrue(state.timeAlertsEnabled)
        assertTrue(state.placeAlertsEnabled)
        assertFalse(state.highPriorityOnly)
        assertTrue(state.reliableRemindersEnabled)
        assertTrue(state.notifyOverdueReminders)
        assertEquals(AlarmSoundType.ShortChime, state.alarmSoundType)
        assertTrue(state.vibrationEnabled)

        // Seguridad por defecto
        assertFalse(state.isAppLockEnabled)
        assertEquals(AppLockType.Pin, state.lockType)
        assertFalse(state.biometricUnlockEnabled)
        assertEquals(LockTimeout.Immediately, state.lockTimeout)
        assertFalse(state.hasConfiguredPassword)
    }

    @Test
    fun `colapsar y expandir seccion de apariencia conmuta isAppearanceExpanded`() {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onToggleAppearanceExpanded()
        assertFalse(viewModel.uiState.value.isAppearanceExpanded)

        viewModel.onToggleAppearanceExpanded()
        assertTrue(viewModel.uiState.value.isAppearanceExpanded)
    }

    @Test
    fun `colapsar y expandir seccion de notificaciones conmuta isNotificationsExpanded`() {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onToggleNotificationsExpanded()
        assertFalse(viewModel.uiState.value.isNotificationsExpanded)

        viewModel.onToggleNotificationsExpanded()
        assertTrue(viewModel.uiState.value.isNotificationsExpanded)
    }

    @Test
    fun `colapsar y expandir seccion de seguridad conmuta isSecurityExpanded`() {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onToggleSecurityExpanded()
        assertFalse(viewModel.uiState.value.isSecurityExpanded)

        viewModel.onToggleSecurityExpanded()
        assertTrue(viewModel.uiState.value.isSecurityExpanded)
    }

    @Test
    fun `colapsar y expandir seccion de datos conmuta isDataExpanded`() {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onToggleDataExpanded()
        assertFalse(viewModel.uiState.value.isDataExpanded)

        viewModel.onToggleDataExpanded()
        assertTrue(viewModel.uiState.value.isDataExpanded)
    }

    @Test
    fun `colapsar y expandir seccion acerca de conmuta isAboutExpanded`() {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onToggleAboutExpanded()
        assertFalse(viewModel.uiState.value.isAboutExpanded)

        viewModel.onToggleAboutExpanded()
        assertTrue(viewModel.uiState.value.isAboutExpanded)
    }

    @Test
    fun `onToggleCollapseAll colapsa todas las secciones si alguna esta expandida y expande todas si ninguna lo esta`() {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        // Inicialmente todas están expandidas
        assertTrue(viewModel.uiState.value.areAnySectionsExpanded)
        assertTrue(viewModel.uiState.value.isAppearanceExpanded)
        assertTrue(viewModel.uiState.value.isNotificationsExpanded)
        assertTrue(viewModel.uiState.value.isSecurityExpanded)
        assertTrue(viewModel.uiState.value.isDataExpanded)
        assertTrue(viewModel.uiState.value.isAboutExpanded)

        // Al conmutar, se colapsan todas
        viewModel.onToggleCollapseAll()
        assertFalse(viewModel.uiState.value.areAnySectionsExpanded)
        assertFalse(viewModel.uiState.value.isAppearanceExpanded)
        assertFalse(viewModel.uiState.value.isNotificationsExpanded)
        assertFalse(viewModel.uiState.value.isSecurityExpanded)
        assertFalse(viewModel.uiState.value.isDataExpanded)
        assertFalse(viewModel.uiState.value.isAboutExpanded)

        // Al conmutar de nuevo, se expanden todas
        viewModel.onToggleCollapseAll()
        assertTrue(viewModel.uiState.value.areAnySectionsExpanded)
        assertTrue(viewModel.uiState.value.isAppearanceExpanded)
        assertTrue(viewModel.uiState.value.isNotificationsExpanded)
        assertTrue(viewModel.uiState.value.isSecurityExpanded)
        assertTrue(viewModel.uiState.value.isDataExpanded)
        assertTrue(viewModel.uiState.value.isAboutExpanded)

        // Si solo una está expandida, el botón colapsa todas
        viewModel.onToggleAppearanceExpanded() // ahora apariencia = false, resto = true
        assertTrue(viewModel.uiState.value.areAnySectionsExpanded)
        viewModel.onToggleCollapseAll()
        assertFalse(viewModel.uiState.value.areAnySectionsExpanded)
        assertFalse(viewModel.uiState.value.isAppearanceExpanded)
        assertFalse(viewModel.uiState.value.isNotificationsExpanded)
        assertFalse(viewModel.uiState.value.isSecurityExpanded)
        assertFalse(viewModel.uiState.value.isDataExpanded)
        assertFalse(viewModel.uiState.value.isAboutExpanded)
    }

    @Test
    fun `desactivar alertas por fecha y hora actualiza el estado`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onTimeAlertsToggled(false)
        assertFalse(viewModel.uiState.value.timeAlertsEnabled)
    }

    @Test
    fun `desactivar alertas por lugar actualiza el estado`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onPlaceAlertsToggled(false)
        assertFalse(viewModel.uiState.value.placeAlertsEnabled)
    }

    @Test
    fun `activar solo alta importancia actualiza el estado`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onHighPriorityOnlyToggled(true)
        assertTrue(viewModel.uiState.value.highPriorityOnly)
    }

    @Test
    fun `conmutar recordatorios fiables actualiza el estado`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onReliableRemindersToggled(false)
        assertFalse(viewModel.uiState.value.reliableRemindersEnabled)

        viewModel.onReliableRemindersToggled(true)
        assertTrue(viewModel.uiState.value.reliableRemindersEnabled)
    }

    @Test
    fun `conmutar notificar recordatorios vencidos actualiza el estado`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onNotifyOverdueToggled(false)
        assertFalse(viewModel.uiState.value.notifyOverdueReminders)
    }

    @Test
    fun `elegir alarma de 1 minuto continuo persiste en el estado`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onAlarmSoundTypeSelected(AlarmSoundType.OneMinuteAlarm)
        assertEquals(AlarmSoundType.OneMinuteAlarm, viewModel.uiState.value.alarmSoundType)
    }

    @Test
    fun `desactivar vibracion actualiza el estado`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onVibrationToggled(false)
        assertFalse(viewModel.uiState.value.vibrationEnabled)
    }

    @Test
    fun `conmutar bloqueo de la app actualiza isAppLockEnabled`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onAppLockToggled(true)
        assertTrue(viewModel.uiState.value.isAppLockEnabled)

        viewModel.onAppLockToggled(false)
        assertFalse(viewModel.uiState.value.isAppLockEnabled)
    }

    @Test
    fun `cambiar tipo de bloqueo a contrasenia actualiza el estado`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        assertEquals(AppLockType.Pin, viewModel.uiState.value.lockType)
        viewModel.onLockTypeSelected(AppLockType.Password)
        assertEquals(AppLockType.Password, viewModel.uiState.value.lockType)
    }

    @Test
    fun `conmutar datos biometricos actualiza biometricUnlockEnabled`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onBiometricUnlockToggled(true)
        assertTrue(viewModel.uiState.value.biometricUnlockEnabled)

        viewModel.onBiometricUnlockToggled(false)
        assertFalse(viewModel.uiState.value.biometricUnlockEnabled)
    }

    @Test
    fun `cambiar tiempo de bloqueo automatico actualiza lockTimeout`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        viewModel.onLockTimeoutSelected(LockTimeout.AfterFiveMinutes)
        assertEquals(LockTimeout.AfterFiveMinutes, viewModel.uiState.value.lockTimeout)
    }

    @Test
    fun `guardar codigo de bloqueo actualiza hasConfiguredPassword y verifyCurrentPassword valida correctamente`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        assertFalse(viewModel.uiState.value.hasConfiguredPassword)

        viewModel.onSaveLockPassword("1234")
        assertTrue(viewModel.uiState.value.hasConfiguredPassword)

        assertTrue(viewModel.verifyCurrentPassword("1234"))
        assertFalse(viewModel.verifyCurrentPassword("9999"))
    }

    @Test
    fun `crear copia de seguridad local invoca repositorio y actualiza metadata y mensaje`() = runBlocking {
        val repo = FakeSettingsRepository()
        val dataRepo = FakeDataManagementRepository()
        val viewModel = createViewModel(repo, dataRepo)

        viewModel.onCreateLocalBackup()
        assertTrue(dataRepo.backupCreated)
        assertNotNull(viewModel.uiState.value.latestBackup)
        assertEquals("test_backup.json", viewModel.uiState.value.latestBackup?.fileName)
        assertEquals("Copia de seguridad creada correctamente", viewModel.uiState.value.dataActionMessage)
    }

    @Test
    fun `restaurar copia de seguridad local invoca repositorio y actualiza mensaje`() = runBlocking {
        val repo = FakeSettingsRepository()
        val dataRepo = FakeDataManagementRepository()
        val viewModel = createViewModel(repo, dataRepo)

        viewModel.onRestoreLocalBackup()
        assertTrue(dataRepo.backupRestored)
        assertTrue(viewModel.uiState.value.dataActionMessage?.contains("5 recordatorios recuperados") == true)
    }

    @Test
    fun `importar respaldo JSON invoca repositorio y actualiza mensaje`() = runBlocking {
        val repo = FakeSettingsRepository()
        val dataRepo = FakeDataManagementRepository()
        val viewModel = createViewModel(repo, dataRepo)

        viewModel.onImportBackupJson("{ \"version\": 1 }")
        assertTrue(dataRepo.jsonImported)
        assertTrue(viewModel.uiState.value.dataActionMessage?.contains("Importación finalizada") == true)
    }

    @Test
    fun `limpiar cache libera memoria y actualiza tamano a 0`() = runBlocking {
        val repo = FakeSettingsRepository()
        val dataRepo = FakeDataManagementRepository()
        val viewModel = createViewModel(repo, dataRepo)

        viewModel.onClearCache()
        assertTrue(dataRepo.cacheCleared)
        assertEquals(0L, viewModel.uiState.value.cacheSizeBytes)
        assertTrue(viewModel.uiState.value.dataActionMessage?.contains("Caché liberada correctamente") == true)
    }

    @Test
    fun `borrar todos los datos invoca clearAllData y actualiza mensaje`() = runBlocking {
        val repo = FakeSettingsRepository()
        val dataRepo = FakeDataManagementRepository()
        val viewModel = createViewModel(repo, dataRepo)

        viewModel.onClearAllData()
        assertTrue(dataRepo.allDataCleared)
        assertEquals("Todos los datos de la app han sido eliminados", viewModel.uiState.value.dataActionMessage)
    }

    @Test
    fun `descartar mensaje de accion limpia dataActionMessage`() = runBlocking {
        val repo = FakeSettingsRepository()
        val dataRepo = FakeDataManagementRepository()
        val viewModel = createViewModel(repo, dataRepo)

        viewModel.onClearAllData()
        assertNotNull(viewModel.uiState.value.dataActionMessage)

        viewModel.onDismissDataActionMessage()
        assertEquals(null, viewModel.uiState.value.dataActionMessage)
    }

    @Test
    fun `buscar actualizaciones transiciona a Checking y luego a UpToDate`() = runBlocking {
        val repo = FakeSettingsRepository()
        val viewModel = createViewModel(repo)

        assertEquals(UpdateCheckStatus.Idle, viewModel.uiState.value.updateCheckStatus)

        viewModel.onCheckForUpdates()
        // Con Dispatchers.Unconfined inicia y entra inmediatamente en Checking
        assertEquals(UpdateCheckStatus.Checking, viewModel.uiState.value.updateCheckStatus)

        // Esperamos que complete el delay simulado de 1200ms
        Thread.sleep(1350)
        assertEquals(UpdateCheckStatus.UpToDate, viewModel.uiState.value.updateCheckStatus)
    }
}

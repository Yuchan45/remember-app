package com.example.uade.rememberapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.AlarmSoundType
import com.example.uade.rememberapp.domain.model.AppFontFamily
import com.example.uade.rememberapp.domain.model.AppLockType
import com.example.uade.rememberapp.domain.model.AppPalette
import com.example.uade.rememberapp.domain.model.FontScale
import com.example.uade.rememberapp.domain.model.LockTimeout
import com.example.uade.rememberapp.domain.model.ThemeMode
import com.example.uade.rememberapp.domain.repository.DataManagementRepository
import com.example.uade.rememberapp.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * ViewModel para gestionar las preferencias y ajustes visuales y de notificación de la app.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val dataManagementRepository: DataManagementRepository? = null,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {

    @Suppress("OPT_IN_USAGE")
    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            settingsRepository.getSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        refreshDataStorageInfo()
    }

    // --- Apariencia ---

    fun onThemeModeSelected(mode: ThemeMode) {
        scope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun onPureBlackToggled(usePureBlack: Boolean) {
        scope.launch {
            settingsRepository.setUsePureBlack(usePureBlack)
        }
    }

    fun onPaletteSelected(palette: AppPalette) {
        scope.launch {
            settingsRepository.setPalette(palette)
        }
    }

    fun onFontScaleSelected(scale: FontScale) {
        scope.launch {
            settingsRepository.setFontScale(scale)
        }
    }

    fun onFontFamilySelected(fontFamily: AppFontFamily) {
        scope.launch {
            settingsRepository.setFontFamily(fontFamily)
        }
    }

    fun onGradientBackgroundToggled(useGradient: Boolean) {
        scope.launch {
            settingsRepository.setUseGradientBackground(useGradient)
        }
    }

    fun onToggleCollapseAll() {
        val areAnyExpanded = _uiState.value.areAnySectionsExpanded
        _uiState.update {
            it.copy(
                isAppearanceExpanded = !areAnyExpanded,
                isNotificationsExpanded = !areAnyExpanded,
                isSecurityExpanded = !areAnyExpanded,
                isDataExpanded = !areAnyExpanded,
                isAboutExpanded = !areAnyExpanded,
            )
        }
    }

    fun onToggleAppearanceExpanded() {
        _uiState.update { it.copy(isAppearanceExpanded = !it.isAppearanceExpanded) }
    }

    // --- Notificaciones ---

    fun onToggleNotificationsExpanded() {
        _uiState.update { it.copy(isNotificationsExpanded = !it.isNotificationsExpanded) }
    }

    fun onTimeAlertsToggled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setTimeAlertsEnabled(enabled)
        }
    }

    fun onPlaceAlertsToggled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setPlaceAlertsEnabled(enabled)
        }
    }

    fun onHighPriorityOnlyToggled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setHighPriorityOnly(enabled)
        }
    }

    fun onReliableRemindersToggled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setReliableRemindersEnabled(enabled)
        }
    }

    fun onNotifyOverdueToggled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setNotifyOverdueReminders(enabled)
        }
    }

    fun onAlarmSoundTypeSelected(soundType: AlarmSoundType) {
        scope.launch {
            settingsRepository.setAlarmSoundType(soundType)
        }
    }

    fun onVibrationToggled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setVibrationEnabled(enabled)
        }
    }

    // --- Seguridad ---

    fun onToggleSecurityExpanded() {
        _uiState.update { it.copy(isSecurityExpanded = !it.isSecurityExpanded) }
    }

    fun onAppLockToggled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setAppLockEnabled(enabled)
        }
    }

    fun onLockTypeSelected(lockType: AppLockType) {
        scope.launch {
            settingsRepository.setAppLockType(lockType)
        }
    }

    fun onBiometricUnlockToggled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setBiometricUnlockEnabled(enabled)
        }
    }

    fun onLockTimeoutSelected(timeout: LockTimeout) {
        scope.launch {
            settingsRepository.setLockTimeout(timeout)
        }
    }

    fun onSaveLockPassword(password: String) {
        scope.launch {
            settingsRepository.setLockPassword(password)
        }
    }

    fun verifyCurrentPassword(password: String): Boolean {
        return settingsRepository.verifyLockPassword(password)
    }

    // --- Datos y almacenamiento ---

    fun onToggleDataExpanded() {
        _uiState.update { it.copy(isDataExpanded = !it.isDataExpanded) }
    }

    fun refreshDataStorageInfo() {
        scope.launch {
            val latestBackup = dataManagementRepository?.getLatestLocalBackup()
            val cacheSize = dataManagementRepository?.getCacheSizeBytes() ?: 0L
            _uiState.update {
                it.copy(
                    latestBackup = latestBackup,
                    cacheSizeBytes = cacheSize,
                )
            }
        }
    }

    fun onCreateLocalBackup() {
        scope.launch {
            _uiState.update { it.copy(isDataOperating = true) }
            val result = dataManagementRepository?.createLocalBackup()
            val metadata = result?.getOrNull()
            _uiState.update {
                it.copy(
                    isDataOperating = false,
                    latestBackup = metadata ?: it.latestBackup,
                    dataActionMessage = if (result?.isSuccess == true) {
                        "Copia de seguridad creada correctamente"
                    } else {
                        "Error al crear la copia de seguridad"
                    },
                )
            }
        }
    }

    fun onRestoreLocalBackup() {
        scope.launch {
            _uiState.update { it.copy(isDataOperating = true) }
            val result = dataManagementRepository?.restoreFromLatestLocalBackup()
            val importResult = result?.getOrNull()
            _uiState.update {
                it.copy(
                    isDataOperating = false,
                    dataActionMessage = if (importResult != null) {
                        "Copia restaurada: ${importResult.remindersImported} recordatorios recuperados"
                    } else {
                        "Error al restaurar la copia de seguridad"
                    },
                )
            }
        }
    }

    fun onExportBackup(onFileReady: (File) -> Unit) {
        scope.launch {
            _uiState.update { it.copy(isDataOperating = true) }
            val result = dataManagementRepository?.exportBackupFile()
            _uiState.update { it.copy(isDataOperating = false) }
            result?.getOrNull()?.let { file ->
                onFileReady(file)
            }
        }
    }

    fun onImportBackupJson(jsonString: String) {
        scope.launch {
            _uiState.update { it.copy(isDataOperating = true) }
            val result = dataManagementRepository?.importBackupFromJson(jsonString)
            val importResult = result?.getOrNull()
            refreshDataStorageInfo()
            _uiState.update {
                it.copy(
                    isDataOperating = false,
                    dataActionMessage = if (importResult != null) {
                        "Importación finalizada: ${importResult.remindersImported} recordatorios, ${importResult.placesImported} lugares y ${importResult.tagsImported} etiquetas"
                    } else {
                        "No se pudo importar el archivo. Verifica el formato."
                    },
                )
            }
        }
    }

    fun onClearCache() {
        scope.launch {
            _uiState.update { it.copy(isDataOperating = true) }
            val freedBytes = dataManagementRepository?.clearCache() ?: 0L
            val freedMb = String.format(Locale.US, "%.1f MB", freedBytes / (1024.0 * 1024.0))
            _uiState.update {
                it.copy(
                    isDataOperating = false,
                    cacheSizeBytes = 0L,
                    dataActionMessage = "Caché liberada correctamente ($freedMb)",
                )
            }
        }
    }

    fun onClearAllData() {
        scope.launch {
            _uiState.update { it.copy(isDataOperating = true) }
            val success = dataManagementRepository?.clearAllData() ?: false
            refreshDataStorageInfo()
            _uiState.update {
                it.copy(
                    isDataOperating = false,
                    dataActionMessage = if (success) {
                        "Todos los datos de la app han sido eliminados"
                    } else {
                        "Error al eliminar datos"
                    },
                )
            }
        }
    }

    fun onDismissDataActionMessage() {
        _uiState.update { it.copy(dataActionMessage = null) }
    }

    // --- Acerca de Hey! ---

    fun onToggleAboutExpanded() {
        _uiState.update { it.copy(isAboutExpanded = !it.isAboutExpanded) }
    }

    fun onCheckForUpdates() {
        scope.launch {
            _uiState.update { it.copy(updateCheckStatus = UpdateCheckStatus.Checking) }
            kotlinx.coroutines.delay(1200)
            _uiState.update { it.copy(updateCheckStatus = UpdateCheckStatus.UpToDate) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                SettingsViewModel(
                    settingsRepository = app.container.settingsRepository,
                    dataManagementRepository = app.container.dataManagementRepository,
                )
            }
        }
    }
}

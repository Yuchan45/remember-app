package com.example.uade.rememberapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AlarmSoundType
import com.example.uade.rememberapp.domain.model.AppFontFamily
import com.example.uade.rememberapp.domain.model.AppLockType
import com.example.uade.rememberapp.domain.model.AppPalette
import com.example.uade.rememberapp.domain.model.FontScale
import com.example.uade.rememberapp.domain.model.LockTimeout
import com.example.uade.rememberapp.domain.model.ThemeMode
import com.example.uade.rememberapp.ui.settings.components.AlarmSoundVibrationCard
import com.example.uade.rememberapp.ui.settings.components.AppLockCard
import com.example.uade.rememberapp.ui.settings.components.BiometricSecurityCard
import com.example.uade.rememberapp.ui.settings.components.FontFamilyCard
import com.example.uade.rememberapp.ui.settings.components.FontScaleCard
import com.example.uade.rememberapp.ui.settings.components.FutureSectionCard
import com.example.uade.rememberapp.ui.settings.components.LockTimeoutCard
import com.example.uade.rememberapp.ui.settings.components.OverdueRemindersCard
import com.example.uade.rememberapp.ui.settings.components.PaletteSelectorCard
import com.example.uade.rememberapp.ui.settings.components.ReliableRemindersCard
import com.example.uade.rememberapp.ui.settings.components.SettingsSectionHeader
import com.example.uade.rememberapp.ui.settings.components.SystemAlertsCard
import com.example.uade.rememberapp.ui.settings.components.ThemeModeCard
import com.example.uade.rememberapp.ui.settings.components.VisualEffectsCard
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.uade.rememberapp.ui.settings.components.CacheCleanupCard
import com.example.uade.rememberapp.ui.settings.components.DataTransferCard
import com.example.uade.rememberapp.ui.settings.components.LocalBackupCard
import com.example.uade.rememberapp.ui.settings.components.AppVersionCard
import com.example.uade.rememberapp.ui.settings.components.CheckUpdatesCard
import com.example.uade.rememberapp.ui.settings.components.PrivacyLicensesCard
import com.example.uade.rememberapp.ui.settings.components.ProjectInfoCard
import com.example.uade.rememberapp.ui.settings.components.ResetDataCard
import java.io.File
import com.example.uade.rememberapp.ui.theme.LocalIsDarkTheme
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.appBackground

/**
 * Pantalla de Ajustes de la aplicación dividida en secciones configurables:
 * [Apariencia], [Notificaciones], [Datos y almacenamiento] y [Acerca de Hey!].
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onToggleCollapseAll = viewModel::onToggleCollapseAll,
        // Apariencia
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onPureBlackToggled = viewModel::onPureBlackToggled,
        onPaletteSelected = viewModel::onPaletteSelected,
        onFontScaleSelected = viewModel::onFontScaleSelected,
        onFontFamilySelected = viewModel::onFontFamilySelected,
        onGradientBackgroundToggled = viewModel::onGradientBackgroundToggled,
        onToggleAppearanceExpanded = viewModel::onToggleAppearanceExpanded,
        // Notificaciones
        onToggleNotificationsExpanded = viewModel::onToggleNotificationsExpanded,
        onTimeAlertsToggled = viewModel::onTimeAlertsToggled,
        onPlaceAlertsToggled = viewModel::onPlaceAlertsToggled,
        onHighPriorityOnlyToggled = viewModel::onHighPriorityOnlyToggled,
        onReliableRemindersToggled = viewModel::onReliableRemindersToggled,
        onNotifyOverdueToggled = viewModel::onNotifyOverdueToggled,
        onAlarmSoundTypeSelected = viewModel::onAlarmSoundTypeSelected,
        onVibrationToggled = viewModel::onVibrationToggled,
        // Seguridad
        onToggleSecurityExpanded = viewModel::onToggleSecurityExpanded,
        onAppLockToggled = viewModel::onAppLockToggled,
        onLockTypeSelected = viewModel::onLockTypeSelected,
        onBiometricUnlockToggled = viewModel::onBiometricUnlockToggled,
        onLockTimeoutSelected = viewModel::onLockTimeoutSelected,
        onSaveLockPassword = viewModel::onSaveLockPassword,
        onVerifyCurrentPassword = viewModel::verifyCurrentPassword,
        // Datos y almacenamiento
        onToggleDataExpanded = viewModel::onToggleDataExpanded,
        onCreateLocalBackup = viewModel::onCreateLocalBackup,
        onRestoreLocalBackup = viewModel::onRestoreLocalBackup,
        onExportBackup = viewModel::onExportBackup,
        onImportBackupJson = viewModel::onImportBackupJson,
        onClearCache = viewModel::onClearCache,
        onClearAllData = viewModel::onClearAllData,
        onDismissDataActionMessage = viewModel::onDismissDataActionMessage,
        // Acerca de Hey!
        onToggleAboutExpanded = viewModel::onToggleAboutExpanded,
        onCheckForUpdates = viewModel::onCheckForUpdates,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

/**
 * Contenido sin estado de Ajustes para soporte de Previews y pruebas.
 */
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onToggleCollapseAll: () -> Unit,
    // Callbacks Apariencia
    onThemeModeSelected: (ThemeMode) -> Unit,
    onPureBlackToggled: (Boolean) -> Unit,
    onPaletteSelected: (AppPalette) -> Unit,
    onFontScaleSelected: (FontScale) -> Unit,
    onFontFamilySelected: (AppFontFamily) -> Unit,
    onGradientBackgroundToggled: (Boolean) -> Unit,
    onToggleAppearanceExpanded: () -> Unit,
    // Callbacks Notificaciones
    onToggleNotificationsExpanded: () -> Unit,
    onTimeAlertsToggled: (Boolean) -> Unit,
    onPlaceAlertsToggled: (Boolean) -> Unit,
    onHighPriorityOnlyToggled: (Boolean) -> Unit,
    onReliableRemindersToggled: (Boolean) -> Unit,
    onNotifyOverdueToggled: (Boolean) -> Unit,
    onAlarmSoundTypeSelected: (AlarmSoundType) -> Unit,
    onVibrationToggled: (Boolean) -> Unit,
    // Callbacks Seguridad
    onToggleSecurityExpanded: () -> Unit,
    onAppLockToggled: (Boolean) -> Unit,
    onLockTypeSelected: (AppLockType) -> Unit,
    onBiometricUnlockToggled: (Boolean) -> Unit,
    onLockTimeoutSelected: (LockTimeout) -> Unit,
    onSaveLockPassword: (String) -> Unit,
    onVerifyCurrentPassword: (String) -> Boolean,
    // Callbacks Datos y almacenamiento
    onToggleDataExpanded: () -> Unit,
    onCreateLocalBackup: () -> Unit,
    onRestoreLocalBackup: () -> Unit,
    onExportBackup: (onFileReady: (File) -> Unit) -> Unit,
    onImportBackupJson: (String) -> Unit,
    onClearCache: () -> Unit,
    onClearAllData: () -> Unit,
    onDismissDataActionMessage: () -> Unit,
    // Callbacks Acerca de Hey!
    onToggleAboutExpanded: () -> Unit,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val systemIsDark = LocalIsDarkTheme.current
    val isDarkTheme = when (uiState.themeMode) {
        ThemeMode.System -> systemIsDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.dataActionMessage) {
        uiState.dataActionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onDismissDataActionMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .appBackground()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        // Título de la pantalla con botón circular para colapsar/expandir todas las secciones de una
        item(key = "header_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                FilledTonalIconButton(
                    onClick = onToggleCollapseAll,
                    modifier = Modifier.size(42.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        painter = painterResource(
                            if (uiState.areAnySectionsExpanded) R.drawable.ic_unfold_less else R.drawable.ic_unfold_more,
                        ),
                        contentDescription = stringResource(
                            if (uiState.areAnySectionsExpanded) {
                                R.string.settings_action_collapse_all
                            } else {
                                R.string.settings_action_expand_all
                            },
                        ),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // ==========================================
        // SECCIÓN 1: APARIENCIA
        // ==========================================
        item(key = "section_appearance_header") {
            SettingsSectionHeader(
                icon = R.drawable.ic_palette,
                titleRes = R.string.settings_section_appearance,
                descriptionRes = R.string.settings_section_appearance_desc,
                isExpanded = uiState.isAppearanceExpanded,
                onToggleExpand = onToggleAppearanceExpanded,
            )
        }

        if (uiState.isAppearanceExpanded) {
            item(key = "theme_mode_card") {
                ThemeModeCard(
                    themeMode = uiState.themeMode,
                    usePureBlack = uiState.usePureBlack,
                    onThemeModeSelected = onThemeModeSelected,
                    onPureBlackToggled = onPureBlackToggled,
                )
            }

            item(key = "palette_card") {
                PaletteSelectorCard(
                    selectedPalette = uiState.palette,
                    onPaletteSelected = onPaletteSelected,
                    isDarkTheme = isDarkTheme,
                )
            }

            item(key = "font_scale_card") {
                FontScaleCard(
                    fontScale = uiState.fontScale,
                    onFontScaleSelected = onFontScaleSelected,
                )
            }

            item(key = "font_family_card") {
                FontFamilyCard(
                    selectedFontFamily = uiState.fontFamily,
                    onFontFamilySelected = onFontFamilySelected,
                )
            }

            item(key = "visual_effects_card") {
                VisualEffectsCard(
                    useGradientBackground = uiState.useGradientBackground,
                    onGradientBackgroundToggled = onGradientBackgroundToggled,
                )
            }
        }

        // ==========================================
        // SECCIÓN 2: NOTIFICACIONES
        // ==========================================
        item(key = "section_notifications_header") {
            SettingsSectionHeader(
                icon = R.drawable.ic_notifications,
                titleRes = R.string.settings_section_notifications,
                descriptionRes = R.string.settings_section_notifications_desc,
                isExpanded = uiState.isNotificationsExpanded,
                onToggleExpand = onToggleNotificationsExpanded,
            )
        }

        if (uiState.isNotificationsExpanded) {
            item(key = "notif_system_alerts_card") {
                SystemAlertsCard(
                    timeAlertsEnabled = uiState.timeAlertsEnabled,
                    placeAlertsEnabled = uiState.placeAlertsEnabled,
                    highPriorityOnly = uiState.highPriorityOnly,
                    onTimeAlertsToggled = onTimeAlertsToggled,
                    onPlaceAlertsToggled = onPlaceAlertsToggled,
                    onHighPriorityOnlyToggled = onHighPriorityOnlyToggled,
                )
            }

            item(key = "notif_reliable_card") {
                ReliableRemindersCard(
                    reliableRemindersEnabled = uiState.reliableRemindersEnabled,
                    onReliableRemindersToggled = onReliableRemindersToggled,
                )
            }

            item(key = "notif_overdue_card") {
                OverdueRemindersCard(
                    notifyOverdueReminders = uiState.notifyOverdueReminders,
                    onNotifyOverdueToggled = onNotifyOverdueToggled,
                )
            }

            item(key = "notif_sound_vibration_card") {
                AlarmSoundVibrationCard(
                    alarmSoundType = uiState.alarmSoundType,
                    vibrationEnabled = uiState.vibrationEnabled,
                    onAlarmSoundTypeSelected = onAlarmSoundTypeSelected,
                    onVibrationToggled = onVibrationToggled,
                )
            }
        }

        // ==========================================
        // SECCIÓN 3: SEGURIDAD Y PRIVACIDAD
        // ==========================================
        item(key = "header_security") {
            SettingsSectionHeader(
                icon = R.drawable.ic_lock,
                titleRes = R.string.settings_section_security,
                descriptionRes = R.string.settings_section_security_desc,
                isExpanded = uiState.isSecurityExpanded,
                onToggleExpand = onToggleSecurityExpanded,
            )
        }

        if (uiState.isSecurityExpanded) {
            item(key = "security_app_lock_card") {
                AppLockCard(
                    isAppLockEnabled = uiState.isAppLockEnabled,
                    lockType = uiState.lockType,
                    hasConfiguredPassword = uiState.hasConfiguredPassword,
                    onAppLockToggled = onAppLockToggled,
                    onLockTypeSelected = onLockTypeSelected,
                    onSavePassword = onSaveLockPassword,
                    onVerifyCurrentPassword = onVerifyCurrentPassword,
                )
            }

            item(key = "security_biometric_card") {
                BiometricSecurityCard(
                    biometricUnlockEnabled = uiState.biometricUnlockEnabled,
                    onBiometricUnlockToggled = onBiometricUnlockToggled,
                )
            }

            if (uiState.isAppLockEnabled) {
                item(key = "security_timeout_card") {
                    LockTimeoutCard(
                        selectedTimeout = uiState.lockTimeout,
                        onTimeoutSelected = onLockTimeoutSelected,
                    )
                }
            }
        }

        // ==========================================
        // SECCIÓN 4: DATOS Y ALMACENAMIENTO
        // ==========================================
        item(key = "header_data") {
            SettingsSectionHeader(
                icon = R.drawable.ic_archive,
                titleRes = R.string.settings_section_data,
                descriptionRes = R.string.settings_section_data_desc,
                isExpanded = uiState.isDataExpanded,
                onToggleExpand = onToggleDataExpanded,
            )
        }

        if (uiState.isDataExpanded) {
            item(key = "data_local_backup_card") {
                LocalBackupCard(
                    latestBackup = uiState.latestBackup,
                    isOperating = uiState.isDataOperating,
                    onCreateBackup = onCreateLocalBackup,
                    onRestoreBackup = onRestoreLocalBackup,
                )
            }

            item(key = "data_transfer_card") {
                DataTransferCard(
                    isOperating = uiState.isDataOperating,
                    onExportRequested = onExportBackup,
                    onImportJson = onImportBackupJson,
                )
            }

            item(key = "data_cache_card") {
                CacheCleanupCard(
                    cacheSizeBytes = uiState.cacheSizeBytes,
                    isOperating = uiState.isDataOperating,
                    onClearCache = onClearCache,
                )
            }

            item(key = "data_reset_card") {
                ResetDataCard(
                    isOperating = uiState.isDataOperating,
                    onConfirmClearAll = onClearAllData,
                )
            }
        }

        // ==========================================
        // SECCIÓN 5: ACERCA DE HEY!
        // ==========================================
        item(key = "header_about") {
            SettingsSectionHeader(
                icon = R.drawable.ic_info,
                titleRes = R.string.settings_section_about,
                descriptionRes = R.string.settings_section_about_desc,
                isExpanded = uiState.isAboutExpanded,
                onToggleExpand = onToggleAboutExpanded,
            )
        }

        if (uiState.isAboutExpanded) {
            item(key = "about_version_card") {
                AppVersionCard()
            }

            item(key = "about_check_updates_card") {
                CheckUpdatesCard(
                    updateCheckStatus = uiState.updateCheckStatus,
                    onCheckForUpdates = onCheckForUpdates,
                )
            }

            item(key = "about_project_info_card") {
                ProjectInfoCard()
            }

            item(key = "about_privacy_licenses_card") {
                PrivacyLicensesCard()
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 80.dp),
    )
}
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    RememberAppTheme {
        SettingsContent(
            uiState = SettingsUiState(),
            onToggleCollapseAll = {},
            onThemeModeSelected = {},
            onPureBlackToggled = {},
            onPaletteSelected = {},
            onFontScaleSelected = {},
            onFontFamilySelected = {},
            onGradientBackgroundToggled = {},
            onToggleAppearanceExpanded = {},
            onToggleNotificationsExpanded = {},
            onTimeAlertsToggled = {},
            onPlaceAlertsToggled = {},
            onHighPriorityOnlyToggled = {},
            onReliableRemindersToggled = {},
            onNotifyOverdueToggled = {},
            onAlarmSoundTypeSelected = {},
            onVibrationToggled = {},
            onToggleSecurityExpanded = {},
            onAppLockToggled = {},
            onLockTypeSelected = {},
            onBiometricUnlockToggled = {},
            onLockTimeoutSelected = {},
            onSaveLockPassword = {},
            onVerifyCurrentPassword = { true },
            onToggleDataExpanded = {},
            onCreateLocalBackup = {},
            onRestoreLocalBackup = {},
            onExportBackup = {},
            onImportBackupJson = {},
            onClearCache = {},
            onClearAllData = {},
            onDismissDataActionMessage = {},
            onToggleAboutExpanded = {},
            onCheckForUpdates = {},
        )
    }
}

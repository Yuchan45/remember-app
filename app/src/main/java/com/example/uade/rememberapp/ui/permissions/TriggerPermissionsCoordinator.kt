package com.example.uade.rememberapp.ui.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.platform.permissions.PermissionsHelper
import com.example.uade.rememberapp.platform.permissions.findActivity
import com.example.uade.rememberapp.ui.components.ContextualPermission
import com.example.uade.rememberapp.ui.components.ContextualPermissionDialog
import kotlinx.coroutines.launch

private enum class PermissionStep {
    Notifications,
    ForegroundLocation,
    BackgroundLocation,
    ExactAlarms,
    Finish,
}

/**
 * Coordinador reutilizable para solicitar en contexto (RNF07) los permisos necesarios al
 * configurar o guardar un recordatorio con aviso por hora o ubicación, o un lugar favorito.
 *
 * - Antes de abrir cualquier diálogo o pantalla de configuración de Android, muestra una ventana
 *   explicativa con botones "Permitir" y "Rechazar".
 * - Solicita cada permiso únicamente la primera vez que se usa la función que lo necesita.
 * - Siempre ejecuta `onProceed` al finalizar la secuencia (aunque el usuario rechace).
 * - Si algún permiso requerido sigue faltando, reporta la limitación a
 *   [PermissionNoticeController] para informar qué quedó limitado y cómo habilitarlo en Ajustes.
 */
@Stable
class TriggerPermissionsCoordinator internal constructor(
    private val context: Context,
    private val noticeController: PermissionNoticeController?,
    private val onLaunchNotifications: () -> Unit,
    private val onLaunchForegroundLocation: () -> Unit,
    private val onLaunchBackgroundLocation: () -> Unit,
    private val onShowExplanationDialog: (ContextualPermission) -> Unit,
) {
    private var pendingProceed: (() -> Unit)? = null
    private var pendingNeedsTime: Boolean = false
    private var pendingNeedsPlace: Boolean = false
    private var pendingIsPlaceOnly: Boolean = false
    private var promptedInFlow: Boolean = false
    private var promptedInSession: Boolean = false
    private var isLocationOnlyRequest: Boolean = false
    private var waitingSettingsResumeStep: PermissionStep? = null
    internal val isWaitingSettingsReturn: Boolean
        get() = waitingSettingsResumeStep != null

    /**
     * Ejecuta [onProceed] luego de verificar y solicitar (si corresponde) los permisos necesarios
     * para los avisos configurados. Siempre guarda/continúa aunque el usuario rechace permisos.
     */
    fun runWithPermissions(
        needsTimeAlarm: Boolean,
        needsPlaceAlert: Boolean,
        isPlaceOnly: Boolean = false,
        onProceed: () -> Unit,
    ) {
        if (!needsTimeAlarm && !needsPlaceAlert) {
            onProceed()
            return
        }
        pendingProceed = onProceed
        pendingNeedsTime = needsTimeAlarm
        pendingNeedsPlace = needsPlaceAlert
        pendingIsPlaceOnly = isPlaceOnly
        promptedInFlow = promptedInSession
        isLocationOnlyRequest = false
        advance(PermissionStep.Notifications)
    }

    /**
     * Solicita en contexto los permisos de ubicación cuando el usuario elige un lugar por primera
     * vez (antes de guardar). Si ya fueron solicitados antes, no vuelve a mostrar ventanas.
     */
    fun requestLocationInContext() {
        if (PermissionsHelper.hasAllLocationPermissions(context)) return
        pendingProceed = null
        pendingNeedsTime = false
        pendingNeedsPlace = true
        pendingIsPlaceOnly = false
        promptedInFlow = false
        isLocationOnlyRequest = true
        advance(PermissionStep.ForegroundLocation)
    }

    internal fun onDialogAllow(permission: ContextualPermission) {
        when (permission) {
            ContextualPermission.Notifications -> {
                PermissionsHelper.setNotificationsPrompted(context, true)
                val activity = context.findActivity()
                val needsRuntime = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !PermissionsHelper.hasPostNotificationsRuntimePermission(context)
                val permanentlyDenied = needsRuntime &&
                    activity != null &&
                    PermissionsHelper.isPermanentlyDenied(activity, Manifest.permission.POST_NOTIFICATIONS)
                if (needsRuntime && !permanentlyDenied) {
                    PermissionsHelper.markPermissionRequested(context, Manifest.permission.POST_NOTIFICATIONS)
                    onLaunchNotifications()
                } else {
                    waitingSettingsResumeStep = PermissionStep.ForegroundLocation
                    PermissionsHelper.openNotificationSettings(context)
                }
            }

            ContextualPermission.ForegroundLocation -> {
                PermissionsHelper.setLocationPrompted(context, true)
                val activity = context.findActivity()
                val permanentlyDenied = activity != null &&
                    PermissionsHelper.isPermanentlyDenied(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                if (!permanentlyDenied) {
                    PermissionsHelper.markPermissionRequested(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    onLaunchForegroundLocation()
                } else {
                    waitingSettingsResumeStep = PermissionStep.BackgroundLocation
                    PermissionsHelper.openAppSettings(context)
                }
            }

            ContextualPermission.BackgroundLocation -> {
                PermissionsHelper.setBackgroundLocationPrompted(context, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    onLaunchBackgroundLocation()
                } else {
                    waitingSettingsResumeStep = nextStepAfterBackground()
                    PermissionsHelper.openAppSettings(context)
                }
            }

            ContextualPermission.ExactAlarms -> {
                PermissionsHelper.setExactAlarmsPrompted(context, true)
                waitingSettingsResumeStep = PermissionStep.Finish
                PermissionsHelper.openExactAlarmSettings(context)
            }

            ContextualPermission.Microphone,
            ContextualPermission.Contacts,
            -> Unit
        }
    }

    internal fun onDialogReject(permission: ContextualPermission) {
        when (permission) {
            ContextualPermission.Notifications -> {
                PermissionsHelper.setNotificationsPrompted(context, true)
                advance(PermissionStep.ForegroundLocation)
            }

            ContextualPermission.ForegroundLocation -> {
                PermissionsHelper.setLocationPrompted(context, true)
                advance(nextStepAfterBackground())
            }

            ContextualPermission.BackgroundLocation -> {
                PermissionsHelper.setBackgroundLocationPrompted(context, true)
                advance(nextStepAfterBackground())
            }

            ContextualPermission.ExactAlarms -> {
                PermissionsHelper.setExactAlarmsPrompted(context, true)
                advance(PermissionStep.Finish)
            }

            ContextualPermission.Microphone,
            ContextualPermission.Contacts,
            -> Unit
        }
    }

    internal fun onNotificationsResult() {
        advance(PermissionStep.ForegroundLocation)
    }

    internal fun onForegroundLocationResult() {
        advance(PermissionStep.BackgroundLocation)
    }

    internal fun onBackgroundLocationResult() {
        advance(nextStepAfterBackground())
    }

    internal fun onReturnedFromSettings() {
        val nextStep = waitingSettingsResumeStep ?: return
        waitingSettingsResumeStep = null
        advance(nextStep)
    }

    private fun nextStepAfterBackground(): PermissionStep =
        if (isLocationOnlyRequest) PermissionStep.Finish else PermissionStep.ExactAlarms

    private fun advance(step: PermissionStep) {
        when (step) {
            PermissionStep.Notifications -> {
                val shouldAskNotifications = (pendingNeedsTime || pendingNeedsPlace) &&
                    !PermissionsHelper.hasNotificationPermission(context) &&
                    !PermissionsHelper.hasPromptedNotifications(context)
                if (shouldAskNotifications) {
                    promptedInFlow = true
                    promptedInSession = true
                    onShowExplanationDialog(ContextualPermission.Notifications)
                } else {
                    advance(PermissionStep.ForegroundLocation)
                }
            }

            PermissionStep.ForegroundLocation -> {
                val shouldAskFineLocation = pendingNeedsPlace &&
                    !PermissionsHelper.hasFineLocationPermission(context) &&
                    !PermissionsHelper.hasPromptedLocation(context)
                if (shouldAskFineLocation) {
                    promptedInFlow = true
                    promptedInSession = true
                    onShowExplanationDialog(ContextualPermission.ForegroundLocation)
                } else {
                    advance(PermissionStep.BackgroundLocation)
                }
            }

            PermissionStep.BackgroundLocation -> {
                val shouldAskBackground = pendingNeedsPlace &&
                    PermissionsHelper.hasFineLocationPermission(context) &&
                    !PermissionsHelper.hasBackgroundLocationPermission(context) &&
                    !PermissionsHelper.isBackgroundLocationPrompted(context)
                if (shouldAskBackground) {
                    promptedInFlow = true
                    promptedInSession = true
                    onShowExplanationDialog(ContextualPermission.BackgroundLocation)
                } else {
                    advance(nextStepAfterBackground())
                }
            }

            PermissionStep.ExactAlarms -> {
                val shouldAskExactAlarm = pendingNeedsTime &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !PermissionsHelper.canScheduleExactAlarms(context) &&
                    !PermissionsHelper.hasPromptedExactAlarms(context)
                if (shouldAskExactAlarm) {
                    promptedInFlow = true
                    promptedInSession = true
                    onShowExplanationDialog(ContextualPermission.ExactAlarms)
                } else {
                    advance(PermissionStep.Finish)
                }
            }

            PermissionStep.Finish -> {
                val proceed = pendingProceed
                val needsTime = pendingNeedsTime
                val needsPlace = pendingNeedsPlace
                val isPlaceOnly = pendingIsPlaceOnly
                val wasLocationOnly = isLocationOnlyRequest
                val shouldShowDialog = promptedInFlow

                pendingProceed = null
                pendingNeedsTime = false
                pendingNeedsPlace = false
                pendingIsPlaceOnly = false
                isLocationOnlyRequest = false

                proceed?.invoke()

                if (!wasLocationOnly) {
                    val missing = collectMissingCapabilities(
                        context = context,
                        needsTimeAlarm = needsTime,
                        needsPlaceAlert = needsPlace,
                    )
                    if (missing.isNotEmpty()) {
                        promptedInSession = false
                        noticeController?.reportLimitations(
                            missing = missing,
                            isPlaceOnly = isPlaceOnly,
                            showDialog = shouldShowDialog,
                        )
                    }
                }
            }
        }
    }
}

fun collectMissingCapabilities(
    context: Context,
    needsTimeAlarm: Boolean,
    needsPlaceAlert: Boolean,
): Set<LimitedCapability> = buildSet {
    if ((needsTimeAlarm || needsPlaceAlert) && !PermissionsHelper.hasNotificationPermission(context)) {
        add(LimitedCapability.Notifications)
    }
    if (needsTimeAlarm && !PermissionsHelper.canScheduleExactAlarms(context)) {
        add(LimitedCapability.ExactAlarms)
    }
    if (needsPlaceAlert && !PermissionsHelper.hasAllLocationPermissions(context)) {
        add(LimitedCapability.BackgroundLocation)
    }
}

@Composable
fun rememberTriggerPermissionsCoordinator(): TriggerPermissionsCoordinator {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val noticeController = LocalPermissionNoticeController.current
    val scope = rememberCoroutineScope()

    var activeDialogPermission by remember { mutableStateOf<ContextualPermission?>(null) }

    lateinit var coordinator: TriggerPermissionsCoordinator

    val notificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { coordinator.onNotificationsResult() }

    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { coordinator.onForegroundLocationResult() }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            val app = context.applicationContext as? RememberApp
            app?.let { scope.launch { it.container.rescheduleRemindersUseCase() } }
        }
        coordinator.onBackgroundLocationResult()
    }

    coordinator = remember(context, noticeController) {
        TriggerPermissionsCoordinator(
            context = context,
            noticeController = noticeController,
            onLaunchNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    coordinator.onNotificationsResult()
                }
            },
            onLaunchForegroundLocation = {
                foregroundLocationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
            onLaunchBackgroundLocation = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    coordinator.onBackgroundLocationResult()
                }
            },
            onShowExplanationDialog = { permission -> activeDialogPermission = permission },
        )
    }

    DisposableEffect(lifecycleOwner, coordinator) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && coordinator.isWaitingSettingsReturn) {
                val app = context.applicationContext as? RememberApp
                app?.let { scope.launch { it.container.rescheduleRemindersUseCase() } }
                coordinator.onReturnedFromSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    activeDialogPermission?.let { permission ->
        ContextualPermissionDialog(
            permission = permission,
            onAllow = {
                activeDialogPermission = null
                coordinator.onDialogAllow(permission)
            },
            onReject = {
                activeDialogPermission = null
                coordinator.onDialogReject(permission)
            },
        )
    }

    return coordinator
}

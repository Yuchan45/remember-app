package com.example.uade.rememberapp.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.uade.rememberapp.platform.permissions.PermissionsHelper
import com.example.uade.rememberapp.platform.permissions.findActivity
import com.example.uade.rememberapp.ui.components.ContextualPermission
import com.example.uade.rememberapp.ui.components.ContextualPermissionDialog

private enum class StartupEssentialStep {
    Notifications,
    ExactAlarms,
    Done,
}

/**
 * Solicita al abrir la aplicación únicamente los dos permisos esenciales para una app de
 * recordatorios: **Notificaciones** y **Alarmas y recordatorios (alarmas exactas)**.
 *
 * El resto de los permisos (ubicación en segundo plano, micrófono, contactos) se siguen
 * solicitando en contexto la primera vez que se utiliza la función que los necesita.
 */
@Composable
fun StartupEssentialPermissionsHandler() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var activeDialog by remember { mutableStateOf<ContextualPermission?>(null) }
    var waitingSettingsNextStep by remember { mutableStateOf<StartupEssentialStep?>(null) }

    fun advanceTo(step: StartupEssentialStep) {
        when (step) {
            StartupEssentialStep.Notifications -> {
                val shouldAskNotifications = !PermissionsHelper.hasNotificationPermission(context) &&
                    !PermissionsHelper.hasPromptedNotifications(context)
                if (shouldAskNotifications) {
                    activeDialog = ContextualPermission.Notifications
                } else {
                    advanceTo(StartupEssentialStep.ExactAlarms)
                }
            }

            StartupEssentialStep.ExactAlarms -> {
                val shouldAskExactAlarms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !PermissionsHelper.canScheduleExactAlarms(context) &&
                    !PermissionsHelper.hasPromptedExactAlarms(context)
                if (shouldAskExactAlarms) {
                    activeDialog = ContextualPermission.ExactAlarms
                } else {
                    activeDialog = null
                }
            }

            StartupEssentialStep.Done -> {
                activeDialog = null
            }
        }
    }

    val notificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        advanceTo(StartupEssentialStep.ExactAlarms)
    }

    LaunchedEffect(Unit) {
        advanceTo(StartupEssentialStep.Notifications)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nextStep = waitingSettingsNextStep ?: return@LifecycleEventObserver
                waitingSettingsNextStep = null
                advanceTo(nextStep)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    activeDialog?.let { permission ->
        ContextualPermissionDialog(
            permission = permission,
            onAllow = {
                activeDialog = null
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
                            notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            waitingSettingsNextStep = StartupEssentialStep.ExactAlarms
                            PermissionsHelper.openNotificationSettings(context)
                        }
                    }

                    ContextualPermission.ExactAlarms -> {
                        PermissionsHelper.setExactAlarmsPrompted(context, true)
                        waitingSettingsNextStep = StartupEssentialStep.Done
                        PermissionsHelper.openExactAlarmSettings(context)
                    }

                    else -> Unit
                }
            },
            onReject = {
                activeDialog = null
                when (permission) {
                    ContextualPermission.Notifications -> {
                        PermissionsHelper.setNotificationsPrompted(context, true)
                        advanceTo(StartupEssentialStep.ExactAlarms)
                    }

                    ContextualPermission.ExactAlarms -> {
                        PermissionsHelper.setExactAlarmsPrompted(context, true)
                        advanceTo(StartupEssentialStep.Done)
                    }

                    else -> Unit
                }
            },
        )
    }
}

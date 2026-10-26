package com.example.uade.rememberapp.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.example.uade.rememberapp.ui.components.LocationPermissionDialog
import com.example.uade.rememberapp.ui.components.PermanentDenialDialog
import com.example.uade.rememberapp.ui.components.StartupPermissionsDialog
import kotlinx.coroutines.launch

/**
 * Gestor declarativo del flujo de permisos al inicio de la aplicación en Jetpack Compose.
 *
 * Implementa las directrices y mejores prácticas de Android:
 * 1. Diálogo educativo previo (Primer / Rationale) antes de lanzar los prompts nativos.
 * 2. Flujo secuencial estricto: Ubicación en primer plano primero, y solo si se concede,
 *    se presenta la explicación y solicitud para ubicación en segundo plano (Android 10+).
 * 3. Manejo de rechazos permanentes con opción de navegación directa a Ajustes de la App.
 * 4. Reprogramación de alarmas y geocercas en el caso de uso del dominio al otorgar permisos
 *    o al resumir la aplicación.
 */
@Composable
fun StartupPermissionsHandler() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = context.applicationContext as? RememberApp

    var showPrimerDialog by remember { mutableStateOf(false) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }
    var showPermanentDenialDialog by remember { mutableStateOf(false) }

    fun rescheduleReminders() {
        app?.let {
            coroutineScope.launch {
                it.container.rescheduleRemindersUseCase()
            }
        }
    }

    // Launcher para ubicación en segundo plano (solicitud en paso separado)
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            rescheduleReminders()
        }
    }

    // Launcher para permisos de primer plano (Notificaciones, Ubicación precisa y Micrófono)
    val foregroundPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            PermissionsHelper.hasFineLocationPermission(context)

        // Android 10+ requiere ACCESS_BACKGROUND_LOCATION en un paso separado tras conceder primer plano
        if (fineLocationGranted && !PermissionsHelper.hasBackgroundLocationPermission(context)) {
            showBackgroundLocationDialog = true
        }

        // Si se concedió alguno de los permisos esenciales, reprogramamos recordatorios
        val anyGranted = permissions.values.any { it }
        if (anyGranted) {
            rescheduleReminders()
        }

        // Verificamos si alguno de los permisos requeridos fue rechazado permanentemente
        val activity = context.findActivity()
        if (activity != null) {
            val hasPermanentDenial = permissions.entries.any { (perm, granted) ->
                !granted && PermissionsHelper.isPermanentlyDenied(activity, perm)
            }
            if (hasPermanentDenial && !PermissionsHelper.hasAllForegroundPermissions(context)) {
                showPermanentDenialDialog = true
            }
        }
    }

    // Verificación al arrancar la pantalla
    LaunchedEffect(Unit) {
        val allGranted = PermissionsHelper.hasAllForegroundPermissions(context)
        val alreadyPrompted = PermissionsHelper.isStartupPromptDismissed(context)

        if (!allGranted && !alreadyPrompted) {
            showPrimerDialog = true
        } else if (PermissionsHelper.hasFineLocationPermission(context) &&
            !PermissionsHelper.hasBackgroundLocationPermission(context) &&
            !PermissionsHelper.isBackgroundLocationPrompted(context)
        ) {
            showBackgroundLocationDialog = true
        }
    }

    // Observa el ciclo de vida para sincronizar cuando el usuario regresa de Configuración
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (PermissionsHelper.hasAllLocationPermissions(context)) {
                    rescheduleReminders()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 1. Diálogo educativo de bienvenida y explicación de permisos
    if (showPrimerDialog) {
        StartupPermissionsDialog(
            onConfirm = {
                showPrimerDialog = false
                PermissionsHelper.setStartupPromptDismissed(context, true)
                val missing = PermissionsHelper.getMissingForegroundPermissions(context)
                if (missing.isNotEmpty()) {
                    foregroundPermissionsLauncher.launch(missing.toTypedArray())
                }
            },
            onDismiss = {
                showPrimerDialog = false
                PermissionsHelper.setStartupPromptDismissed(context, true)
            },
        )
    }

    // 2. Diálogo específico de ubicación en segundo plano ("Permitir todo el tiempo")
    if (showBackgroundLocationDialog) {
        LocationPermissionDialog(
            onConfirm = {
                showBackgroundLocationDialog = false
                PermissionsHelper.setBackgroundLocationPrompted(context, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    PermissionsHelper.openAppSettings(context)
                }
            },
            onDismiss = {
                showBackgroundLocationDialog = false
                PermissionsHelper.setBackgroundLocationPrompted(context, true)
            },
        )
    }

    // 3. Diálogo de orientación hacia Ajustes si se rechazó permanentemente
    if (showPermanentDenialDialog) {
        PermanentDenialDialog(
            onOpenSettings = {
                showPermanentDenialDialog = false
                PermissionsHelper.openAppSettings(context)
            },
            onDismiss = {
                showPermanentDenialDialog = false
            },
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

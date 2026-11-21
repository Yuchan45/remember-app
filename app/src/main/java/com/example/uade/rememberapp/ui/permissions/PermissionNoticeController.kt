package com.example.uade.rememberapp.ui.permissions

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.platform.permissions.PermissionsHelper

/**
 * Capacidades del sistema que pueden quedar limitadas al guardar un recordatorio o lugar si el
 * usuario rechaza o desactiva el permiso correspondiente.
 */
enum class LimitedCapability {
    Notifications,
    ExactAlarms,
    BackgroundLocation,
}

data class PermissionLimitationNotice(
    val capabilities: Set<LimitedCapability>,
    val isPlaceOnly: Boolean = false,
    val id: Long = System.nanoTime(),
)

/**
 * Controlador de nivel de aplicación para mostrar los avisos de funcionalidad limitada tras
 * guardar un recordatorio o lugar (RNF07 / RNF04), sobreviviendo al cierre de modales o pantallas
 * de detalle.
 */
@Stable
class PermissionNoticeController {
    var dialogNotice by mutableStateOf<PermissionLimitationNotice?>(null)
        private set

    var snackbarNotice by mutableStateOf<PermissionLimitationNotice?>(null)
        private set

    fun reportLimitations(
        missing: Set<LimitedCapability>,
        isPlaceOnly: Boolean = false,
        showDialog: Boolean,
    ) {
        if (missing.isEmpty()) return
        val notice = PermissionLimitationNotice(capabilities = missing, isPlaceOnly = isPlaceOnly)
        if (showDialog) {
            dialogNotice = notice
        } else {
            snackbarNotice = notice
        }
    }

    fun showDialogFromSnackbar(notice: PermissionLimitationNotice) {
        snackbarNotice = null
        dialogNotice = notice
    }

    fun dismissDialog() {
        dialogNotice = null
    }

    fun dismissSnackbar(id: Long) {
        if (snackbarNotice?.id == id) {
            snackbarNotice = null
        }
    }
}

val LocalPermissionNoticeController = staticCompositionLocalOf<PermissionNoticeController?> { null }

fun openSettingsForCapability(context: Context, capability: LimitedCapability) {
    when (capability) {
        LimitedCapability.Notifications -> PermissionsHelper.openNotificationSettings(context)
        LimitedCapability.ExactAlarms -> PermissionsHelper.openExactAlarmSettings(context)
        LimitedCapability.BackgroundLocation -> PermissionsHelper.openAppSettings(context)
    }
}

@StringRes
fun snackbarMessageResFor(capabilities: Set<LimitedCapability>): Int = when {
    capabilities.size > 1 -> R.string.permission_limitation_snackbar_multiple
    LimitedCapability.Notifications in capabilities -> R.string.permission_limitation_snackbar_notifications
    LimitedCapability.ExactAlarms in capabilities -> R.string.permission_limitation_snackbar_exact_alarms
    else -> R.string.permission_limitation_snackbar_location
}

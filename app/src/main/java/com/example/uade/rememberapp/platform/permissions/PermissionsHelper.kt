package com.example.uade.rememberapp.platform.permissions

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.uade.rememberapp.platform.location.LocationPermissionHelper

/**
 * Utilidades para verificación, seguimiento de solicitudes en contexto y apertura de Ajustes
 * del sistema para los permisos de la aplicación (notificaciones, ubicación, micrófono,
 * alarmas exactas y contactos) según RNF07.
 */
object PermissionsHelper {

    private const val PREFS_NAME = "remember_permissions_prefs"
    private const val KEY_NOTIFICATIONS_PROMPTED = "key_notifications_prompted"
    private const val KEY_EXACT_ALARMS_PROMPTED = "key_exact_alarms_prompted"
    private const val KEY_LOCATION_PROMPTED = "key_location_prompted"
    private const val KEY_BACKGROUND_PROMPTED = "key_background_location_prompted"
    private const val KEY_MICROPHONE_PROMPTED = "key_microphone_prompted"
    private const val KEY_CONTACTS_PROMPTED = "key_contacts_prompted"
    private const val KEY_REQUESTED_PREFIX = "key_requested_"

    fun hasPostNotificationsRuntimePermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun hasNotificationPermission(context: Context): Boolean =
        hasPostNotificationsRuntimePermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun canScheduleExactAlarms(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

    fun hasRecordAudioPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    fun hasFineLocationPermission(context: Context): Boolean =
        LocationPermissionHelper.hasFineLocation(context)

    fun hasCoarseLocationPermission(context: Context): Boolean =
        LocationPermissionHelper.hasCoarseLocation(context)

    fun hasBackgroundLocationPermission(context: Context): Boolean =
        LocationPermissionHelper.hasBackgroundLocation(context)

    fun hasAllLocationPermissions(context: Context): Boolean =
        LocationPermissionHelper.hasAllLocationPermissions(context)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasPromptedNotifications(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATIONS_PROMPTED, false)

    fun setNotificationsPrompted(context: Context, prompted: Boolean = true) {
        prefs(context).edit { putBoolean(KEY_NOTIFICATIONS_PROMPTED, prompted) }
    }

    fun hasPromptedExactAlarms(context: Context): Boolean =
        prefs(context).getBoolean(KEY_EXACT_ALARMS_PROMPTED, false)

    fun setExactAlarmsPrompted(context: Context, prompted: Boolean = true) {
        prefs(context).edit { putBoolean(KEY_EXACT_ALARMS_PROMPTED, prompted) }
    }

    fun hasPromptedLocation(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCATION_PROMPTED, false)

    fun setLocationPrompted(context: Context, prompted: Boolean = true) {
        prefs(context).edit { putBoolean(KEY_LOCATION_PROMPTED, prompted) }
    }

    fun isBackgroundLocationPrompted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BACKGROUND_PROMPTED, false)

    fun setBackgroundLocationPrompted(context: Context, prompted: Boolean = true) {
        prefs(context).edit { putBoolean(KEY_BACKGROUND_PROMPTED, prompted) }
    }

    fun hasPromptedMicrophone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MICROPHONE_PROMPTED, false)

    fun setMicrophonePrompted(context: Context, prompted: Boolean = true) {
        prefs(context).edit { putBoolean(KEY_MICROPHONE_PROMPTED, prompted) }
    }

    fun hasPromptedContacts(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CONTACTS_PROMPTED, false)

    fun setContactsPrompted(context: Context, prompted: Boolean = true) {
        prefs(context).edit { putBoolean(KEY_CONTACTS_PROMPTED, prompted) }
    }

    fun hasContactsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED

    fun markPermissionRequested(context: Context, permission: String) {
        prefs(context).edit { putBoolean("$KEY_REQUESTED_PREFIX$permission", true) }
    }

    fun wasPermissionRequested(context: Context, permission: String): Boolean =
        prefs(context).getBoolean("$KEY_REQUESTED_PREFIX$permission", false)

    /**
     * Verifica si un permiso runtime fue rechazado permanentemente ("No volver a preguntar").
     * Solo puede ser verdadero si ya se solicitó alguna vez, porque antes de la primera
     * solicitud `shouldShowRequestPermissionRationale` también devuelve `false`.
     */
    fun isPermanentlyDenied(activity: Activity, permission: String): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        if (isGranted) return false
        if (!wasPermissionRequested(activity, permission)) return false
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun openAppSettings(context: Context) {
        LocationPermissionHelper.openAppSettings(context)
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings(context)
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Fallback a Ajustes de la app si el fabricante no implementa la pantalla directa.
            }
        }
        openAppSettings(context)
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

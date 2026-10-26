package com.example.uade.rememberapp.platform.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.uade.rememberapp.platform.location.LocationPermissionHelper

/**
 * Utilidades para verificación y gestión de permisos en tiempo de ejecución
 * (notificaciones, ubicación y micrófono) según las mejores prácticas de Android.
 */
object PermissionsHelper {

    private const val PREFS_NAME = "remember_permissions_prefs"
    private const val KEY_STARTUP_PROMPTED = "key_startup_permissions_prompted"
    private const val KEY_BACKGROUND_PROMPTED = "key_background_location_prompted"

    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
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

    /**
     * Retorna true si todos los permisos de primer plano necesarios para la app
     * (notificaciones, ubicación precisa y micrófono) ya fueron otorgados.
     */
    fun hasAllForegroundPermissions(context: Context): Boolean =
        hasNotificationPermission(context) &&
            hasFineLocationPermission(context) &&
            hasRecordAudioPermission(context)

    /**
     * Lista de permisos de primer plano que aún faltan solicitar al usuario.
     */
    fun getMissingForegroundPermissions(context: Context): List<String> = buildList {
        if (!hasNotificationPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!hasFineLocationPermission(context)) {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!hasRecordAudioPermission(context)) {
            add(Manifest.permission.RECORD_AUDIO)
        }
    }

    /**
     * Determina si el diálogo de bienvenida/permisos de inicio ya fue mostrado al usuario.
     */
    fun isStartupPromptDismissed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_STARTUP_PROMPTED, false)
    }

    fun setStartupPromptDismissed(context: Context, dismissed: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_STARTUP_PROMPTED, dismissed).apply()
    }

    fun isBackgroundLocationPrompted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BACKGROUND_PROMPTED, false)
    }

    fun setBackgroundLocationPrompted(context: Context, prompted: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BACKGROUND_PROMPTED, prompted).apply()
    }

    /**
     * Verifica si algún permiso fue denegado permanentemente ("No volver a preguntar").
     */
    fun isPermanentlyDenied(activity: Activity, permission: String): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        if (isGranted) return false
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun openAppSettings(context: Context) {
        LocationPermissionHelper.openAppSettings(context)
    }
}

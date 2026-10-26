package com.example.uade.rememberapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.uade.rememberapp.platform.location.LocationPermissionHelper
import com.example.uade.rememberapp.ui.navigation.AppNavHost
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Única Activity de la app. Monta el AppNavHost, que tiene la barra inferior y todas las
 * pantallas; la navegación entre ellas es con Navigation Compose, no con Activities.
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* No-op si acepta/rechaza */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        // La app es siempre oscura: íconos claros en las barras del sistema aunque el
        // teléfono esté en modo claro (sin argumentos, seguiría el tema del sistema).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            RememberAppTheme {
                AppNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (LocationPermissionHelper.hasAllLocationPermissions(this)) {
            val app = application as? RememberApp
            app?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    it.container.rescheduleRemindersUseCase()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

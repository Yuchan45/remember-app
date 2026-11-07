package com.example.uade.rememberapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.uade.rememberapp.platform.notification.NotificationHelper
import com.example.uade.rememberapp.ui.components.NavigationBarScrim
import com.example.uade.rememberapp.ui.navigation.AppNavHost
import com.example.uade.rememberapp.ui.permissions.StartupPermissionsHandler
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Única Activity de la app. Monta el [AppNavHost] y el [StartupPermissionsHandler] para la
 * gestión reactiva de permisos esenciales al inicio siguiendo las mejores prácticas de Compose,
 * y encima de todo el velo de la barra de navegación del sistema ([NavigationBarScrim]).
 *
 * También recibe el toque en una notificación: trae el id del recordatorio y lo abre.
 */
class MainActivity : ComponentActivity() {

    /**
     * El recordatorio que pidió abrir una notificación, hasta que [AppNavHost] navega a él. Es
     * estado de Compose: cambiarlo en [onNewIntent] (con la app ya abierta) navega solo.
     */
    private var reminderToOpen by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // La app es siempre oscura: íconos claros en las barras del sistema aunque el
        // teléfono esté en modo claro (sin argumentos, seguiría el tema del sistema).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        // Solo la primera vez: al girar el teléfono la Activity se recrea con el mismo intent, y
        // el detalle ya está abierto (lo restaura la navegación).
        if (savedInstanceState == null) reminderToOpen = intent.reminderIdToOpen()

        setContent {
            RememberAppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        reminderToOpen = reminderToOpen,
                        onReminderOpened = { reminderToOpen = null },
                    )
                    StartupPermissionsHandler()
                    // Encima de todas las pantallas: la barra del sistema negra y translúcida.
                    NavigationBarScrim(modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }

    /** Con la app ya abierta, tocar una notificación llega acá (por FLAG_ACTIVITY_SINGLE_TOP). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.reminderIdToOpen()?.let { reminderToOpen = it }
    }

    private fun Intent.reminderIdToOpen(): Long? =
        getLongExtra(NotificationHelper.EXTRA_NAVIGATE_TO_REMINDER_ID, -1L).takeIf { it > 0 }
}

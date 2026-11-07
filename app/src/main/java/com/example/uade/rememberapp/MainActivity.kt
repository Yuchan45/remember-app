package com.example.uade.rememberapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uade.rememberapp.domain.model.AppSettings
import com.example.uade.rememberapp.domain.model.ThemeMode
import com.example.uade.rememberapp.platform.notification.NotificationHelper
import com.example.uade.rememberapp.ui.components.NavigationBarScrim
import com.example.uade.rememberapp.ui.navigation.AppNavHost
import com.example.uade.rememberapp.ui.navigation.Routes
import com.example.uade.rememberapp.ui.permissions.StartupPermissionsHandler
import com.example.uade.rememberapp.ui.security.AppLockScreen
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Única Activity de la app. Monta el [AppNavHost], aplica el tema reactivo según los
 * ajustes del usuario y sincroniza la barra de estado y navegación del sistema. En tema oscuro,
 * además dibuja encima de todo el velo de la barra de navegación del sistema
 * ([NavigationBarScrim]).
 *
 * También recibe el toque en una notificación: trae el id del recordatorio y lo abre.
 */
class MainActivity : ComponentActivity() {

    /**
     * La pantalla (ruta) del recordatorio que pidió abrir una notificación, hasta que [AppNavHost]
     * navega a ella: el detalle, o la pantalla del mensaje programado si es uno de esos. Es
     * estado de Compose: cambiarlo en [onNewIntent] (con la app ya abierta) navega solo.
     */
    private var reminderToOpen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as RememberApp).container

        // Solo la primera vez: al girar el teléfono la Activity se recrea con el mismo intent, y
        // el detalle ya está abierto (lo restaura la navegación).
        if (savedInstanceState == null) reminderToOpen = intent.routeToOpen()

        setContent {
            val settings by container.settingsRepository.getSettings()
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            val isDarkTheme = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
                onDispose { }
            }

            // Gestión de sesión bloqueada / desbloqueada
            var isUnlockedThisSession by rememberSaveable { mutableStateOf(false) }
            var backgroundTimestamp by remember { mutableLongStateOf(0L) }
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner, settings.security) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            backgroundTimestamp = System.currentTimeMillis()
                        }
                        Lifecycle.Event.ON_START -> {
                            if (backgroundTimestamp > 0L &&
                                settings.security.isAppLockEnabled &&
                                settings.security.hasConfiguredPassword
                            ) {
                                val elapsedSec = (System.currentTimeMillis() - backgroundTimestamp) / 1000
                                if (elapsedSec >= settings.security.lockTimeout.seconds) {
                                    isUnlockedThisSession = false
                                }
                            }
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val isLocked = settings.security.isAppLockEnabled &&
                settings.security.hasConfiguredPassword &&
                !isUnlockedThisSession

            RememberAppTheme(settings = settings) {
                if (isLocked) {
                    AppLockScreen(
                        securitySettings = settings.security,
                        onVerifyPassword = { input -> container.settingsRepository.verifyLockPassword(input) },
                        onUnlockSuccess = { isUnlockedThisSession = true },
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(
                            reminderToOpen = reminderToOpen,
                            onReminderOpened = { reminderToOpen = null },
                        )
                        StartupPermissionsHandler()
                        // Encima de todas las pantallas: la barra del sistema negra y translúcida.
                        // Solo en tema oscuro: con el tema claro, una franja negra abajo desentona.
                        if (isDarkTheme) {
                            NavigationBarScrim(modifier = Modifier.align(Alignment.BottomCenter))
                        }
                    }
                }
            }
        }
    }

    /** Con la app ya abierta, tocar una notificación llega acá (por FLAG_ACTIVITY_SINGLE_TOP). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.routeToOpen()?.let { reminderToOpen = it }
    }

    private fun Intent.routeToOpen(): String? {
        val id = getLongExtra(NotificationHelper.EXTRA_NAVIGATE_TO_REMINDER_ID, -1L).takeIf { it > 0 }
            ?: return null
        return if (getBooleanExtra(NotificationHelper.EXTRA_IS_SCHEDULED_MESSAGE, false)) {
            Routes.scheduledMessageDetail(id)
        } else {
            Routes.reminderDetail(id)
        }
    }
}

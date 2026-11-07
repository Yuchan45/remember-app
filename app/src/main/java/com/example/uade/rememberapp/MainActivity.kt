package com.example.uade.rememberapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uade.rememberapp.domain.model.AppSettings
import com.example.uade.rememberapp.domain.model.ThemeMode
import com.example.uade.rememberapp.ui.navigation.AppNavHost
import com.example.uade.rememberapp.ui.permissions.StartupPermissionsHandler
import com.example.uade.rememberapp.ui.security.AppLockScreen
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Única Activity de la app. Monta el [AppNavHost], aplica el tema reactivo según los
 * ajustes del usuario y sincroniza la barra de estado y navegación del sistema.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as RememberApp).container

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
                    AppNavHost()
                    StartupPermissionsHandler()
                }
            }
        }
    }
}

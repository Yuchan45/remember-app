package com.example.uade.rememberapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.uade.rememberapp.ui.navigation.AppNavHost
import com.example.uade.rememberapp.ui.permissions.StartupPermissionsHandler
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Única Activity de la app. Monta el [AppNavHost] y el [StartupPermissionsHandler] para la
 * gestión reactiva de permisos esenciales al inicio siguiendo las mejores prácticas de Compose.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // La app es siempre oscura: íconos claros en las barras del sistema aunque el
        // teléfono esté en modo claro (sin argumentos, seguiría el tema del sistema).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            RememberAppTheme {
                AppNavHost()
                StartupPermissionsHandler()
            }
        }
    }
}

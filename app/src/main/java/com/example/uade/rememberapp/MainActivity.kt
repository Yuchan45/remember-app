package com.example.uade.rememberapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.uade.rememberapp.ui.reminders.list.RemindersListScreen
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Única Activity de la app. Por ahora monta directo la pantalla de inicio;
 * cuando haya más de una pantalla, acá va el NavGraph en su lugar.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RememberAppTheme {
                RemindersListScreen()
            }
        }
    }
}

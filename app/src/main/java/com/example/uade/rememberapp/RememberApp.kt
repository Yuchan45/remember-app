package com.example.uade.rememberapp

import android.app.Application
import com.example.uade.rememberapp.platform.notification.NotificationHelper

/**
 * Punto de entrada del proceso. Acá vive el [AppContainer] (inyección de dependencias
 * manual): repositorios y, más adelante, base de datos, schedulers y casos de uso, creados una
 * sola vez.
 */
class RememberApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createNotificationChannel(this)
    }
}

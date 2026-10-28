package com.example.uade.rememberapp

import android.app.Application
import android.content.Context
import com.example.uade.rememberapp.platform.notification.NotificationHelper
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Punto de entrada del proceso. Acá vive el [AppContainer] (inyección de dependencias
 * manual): repositorios y, más adelante, base de datos, schedulers y casos de uso, creados una
 * sola vez.
 */
class RememberApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        initOsmDroid()
        container = AppContainer(this)
        NotificationHelper.createNotificationChannel(this)
    }

    private fun initOsmDroid() {
        val prefs = getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        val osmConfig = Configuration.getInstance()
        osmConfig.load(this, prefs)
        osmConfig.userAgentValue = "HeyRememberApp/1.0 (Android; org.uade.rememberapp)"
        val osmDir = File(cacheDir, "osmdroid")
        osmConfig.osmdroidBasePath = osmDir
        osmConfig.osmdroidTileCache = File(osmDir, "tiles")
        osmConfig.save(this, prefs)

        // Limpiar caché previa de CartoDB si existía para evitar tiles con marcas de agua
        try {
            val cartoCache = File(osmConfig.osmdroidTileCache, "CartoDark")
            if (cartoCache.exists()) {
                cartoCache.deleteRecursively()
            }
        } catch (_: Exception) {
        }
    }
}

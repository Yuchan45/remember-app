package com.example.uade.rememberapp

import android.app.Application

/**
 * Punto de entrada del proceso. Acá va a vivir el AppContainer (inyección de dependencias
 * manual): base de datos, repositorios, schedulers y casos de uso, creados una sola vez.
 */
class RememberApp : Application() {
    // lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // container = AppContainer(this)
    }
}

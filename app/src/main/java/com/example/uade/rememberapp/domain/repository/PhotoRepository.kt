package com.example.uade.rememberapp.domain.repository

import com.example.uade.rememberapp.domain.model.CropRect

/**
 * Los archivos de las fotos de los recordatorios. Las rutas son Strings para que el dominio no
 * dependa de `android.net.Uri` ni de `java.io.File`.
 */
interface PhotoRepository {

    /** Copia la foto de [uri] (la que devuelve el selector de Android) dentro de la app y devuelve su ruta. */
    suspend fun import(uri: String): String

    /** Guarda la porción [crop] de la foto en [sourcePath] como un archivo nuevo y devuelve su ruta. */
    suspend fun saveCover(sourcePath: String, crop: CropRect): String

    suspend fun delete(path: String)
}

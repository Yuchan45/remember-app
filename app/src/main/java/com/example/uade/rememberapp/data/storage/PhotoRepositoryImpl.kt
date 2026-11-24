package com.example.uade.rememberapp.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.example.uade.rememberapp.domain.model.CropRect
import com.example.uade.rememberapp.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Guarda las fotos de los recordatorios en `filesDir/photos` (privado de la app: no aparecen en la
 * galería y se borran al desinstalar).
 *
 * Todo corre en [Dispatchers.IO]: leer, decodificar y comprimir imágenes tarda, y en el hilo
 * principal congelaría la pantalla.
 */
class PhotoRepositoryImpl(context: Context) : PhotoRepository {

    private val appContext = context.applicationContext
    private val dir: File get() = File(appContext.filesDir, "photos").apply { mkdirs() }

    /**
     * Copia la foto del selector. No se guarda la URI: el permiso para leerla es temporal y
     * después de reiniciar la app ya no se podría abrir.
     *
     * De paso la achica (una foto de cámara pesa varios MB y nunca se muestra a ese tamaño) y la
     * endereza según el EXIF: las cámaras guardan la foto "acostada" y anotan cómo girarla.
     */
    override suspend fun import(uri: String): String = withContext(Dispatchers.IO) {
        val source = uri.toUri()
        val resolver = appContext.contentResolver

        // 1) Solo el tamaño, sin cargar la imagen, para elegir cuánto achicarla al decodificar.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MaxImportSide)
        }

        // 2) La imagen, ya achicada.
        val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("No se pudo leer la imagen $uri")

        // 3) La rotación anotada en el EXIF.
        val orientation = resolver.openInputStream(source)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val upright = decoded.rotatedBy(orientation)
        save(upright, prefix = "photo")
    }

    /** Recorta la porción [crop] y la achica a [MaxCoverWidth] de ancho (alcanza para la pantalla). */
    override suspend fun saveCover(sourcePath: String, crop: CropRect): String = withContext(Dispatchers.IO) {
        val source = BitmapFactory.decodeFile(sourcePath) ?: error("No se pudo leer la imagen $sourcePath")
        val left = (crop.left * source.width).roundToInt().coerceIn(0, source.width - 1)
        val top = (crop.top * source.height).roundToInt().coerceIn(0, source.height - 1)
        val width = (crop.right * source.width).roundToInt().coerceAtMost(source.width) - left
        val height = (crop.bottom * source.height).roundToInt().coerceAtMost(source.height) - top
        val cropped = Bitmap.createBitmap(source, left, top, width.coerceAtLeast(1), height.coerceAtLeast(1))

        val scaled = if (cropped.width > MaxCoverWidth) {
            val ratio = MaxCoverWidth.toFloat() / cropped.width
            cropped.scale(MaxCoverWidth, (cropped.height * ratio).roundToInt())
        } else {
            cropped
        }
        save(scaled, prefix = "cover")
    }

    override suspend fun delete(path: String) {
        withContext(Dispatchers.IO) { File(path).delete() }
    }

    private fun save(bitmap: Bitmap, prefix: String): String {
        val file = File(dir, "${prefix}_${UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JpegQuality, it) }
        return file.absolutePath
    }

    private fun Bitmap.rotatedBy(exifOrientation: Int): Bitmap {
        val degrees = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return this
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    /**
     * `inSampleSize` decodifica 1 de cada N píxeles (N potencia de 2): con 2, la imagen sale con
     * la mitad de ancho y de alto. Se busca el mayor N que deje el lado más largo en [maxSide] o más.
     */
    private fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    private companion object {
        const val MaxImportSide = 2048
        const val MaxCoverWidth = 1280
        const val JpegQuality = 90
    }
}

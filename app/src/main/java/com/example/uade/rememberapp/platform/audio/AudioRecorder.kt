package com.example.uade.rememberapp.platform.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

/**
 * Graba audio del micrófono usando [MediaRecorder] de Android en formato AAC / MPEG-4 (.m4a).
 * Pertenece a la capa `platform` porque interactúa directamente con el hardware y APIs del sistema.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isRecording = false
    private var isPaused = false

    /**
     * Inicia la grabación guardando en [outputFile].
     * Devuelve `true` si inició correctamente, `false` si ocurrió un error.
     */
    fun start(outputFile: File): Boolean {
        stopAndRelease()

        return try {
            outputFile.parentFile?.mkdirs()

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            currentFile = outputFile
            isRecording = true
            isPaused = false
            true
        } catch (e: IOException) {
            e.printStackTrace()
            stopAndRelease()
            false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            stopAndRelease()
            false
        }
    }

    /** Pausa la grabación (disponible nativamente desde API 24). */
    fun pause() {
        if (isRecording && !isPaused) {
            try {
                recorder?.pause()
                isPaused = true
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    /** Reanuda la grabación en pausa. */
    fun resume() {
        if (isRecording && isPaused) {
            try {
                recorder?.resume()
                isPaused = false
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Detiene la grabación y devuelve el archivo generado si todo fue exitoso.
     */
    fun stop(): File? {
        val file = currentFile
        stopAndRelease()
        return if (file != null && file.exists() && file.length() > 0) file else null
    }

    /**
     * Cancela y elimina el archivo de audio grabado.
     */
    fun cancel() {
        val file = currentFile
        stopAndRelease()
        file?.delete()
    }

    /**
     * Devuelve la amplitud máxima registrada desde la última llamada (entre 0 y 32767).
     * Si no está grabando o está pausado, devuelve 0.
     */
    fun getMaxAmplitude(): Int {
        if (!isRecording || isPaused) return 0
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: IllegalStateException) {
            0
        }
    }

    private fun stopAndRelease() {
        try {
            if (isRecording) {
                recorder?.stop()
            }
        } catch (e: RuntimeException) {
            // Si se detiene inmediatamente tras start(), MediaRecorder puede arrojar RuntimeException
            currentFile?.delete()
        } finally {
            try {
                recorder?.reset()
                recorder?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            recorder = null
            isRecording = false
            isPaused = false
        }
    }
}

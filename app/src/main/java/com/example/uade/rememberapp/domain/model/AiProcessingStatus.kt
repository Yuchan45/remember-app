package com.example.uade.rememberapp.domain.model

/**
 * Estado del procesamiento por IA (Groq Whisper + LLM) de una nota de voz o captura.
 */
enum class AiProcessingStatus {
    /** Creado normalmente sin requerir procesamiento posterior. */
    None,

    /** Guardado sin conexión, esperando que WorkManager lo procese al recuperar red. */
    Pending,

    /** Procesándose actualmente. */
    Processing,

    /** Procesado y estructurado con éxito por la IA. */
    Completed,

    /** Falló la llamada a la IA tras reintentos; el audio original se preserva para editar o reintentar. */
    Failed,
}

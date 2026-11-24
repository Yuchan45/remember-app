package com.example.uade.rememberapp.domain.model

/**
 * Por qué falló procesar una nota de voz con la IA. Lo devuelve AiReminderService dentro del
 * `Result.failure`, para que la pantalla pueda decir lo que pasó de verdad.
 *
 * Antes todos los errores se mostraban como "audio vacío o ininteligible" (incluso sin clave de
 * API o con una clave inválida), y además se borraba la grabación: parecía que la app no
 * entendía, cuando en realidad nunca llegó a mandarla.
 */
sealed class AiProcessingError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** La app se compiló sin la clave de Groq (`groq.api.key` en local.properties). */
    class MissingApiKey : AiProcessingError("Falta la clave de API de Groq")

    /** Groq rechazó la clave (401/403): está mal copiada, vencida o revocada. */
    class InvalidApiKey(cause: Throwable? = null) : AiProcessingError("Clave de API de Groq inválida", cause)

    /** Se mandó el audio pero no tenía voz que se entendiera (silencio, ruido, muy corto). */
    class Unintelligible : AiProcessingError("El audio está vacío o es ininteligible")

    /** Sin conexión o Groq no respondió: se puede reintentar más tarde con el mismo audio. */
    class Network(cause: Throwable? = null) : AiProcessingError("Error de red con Groq", cause)
}

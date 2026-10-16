package com.example.uade.rememberapp.domain.model

/**
 * Cuánto muestra la notificación de un recordatorio con el teléfono bloqueado. Desbloqueado, la
 * notificación siempre se ve completa.
 */
enum class Visibility {
    /** Se ven el título y el contenido. */
    Default,

    /** Se ve el título; el contenido queda oculto. */
    Private,

    /** No se muestra nada en la pantalla bloqueada. */
    Secret,
}

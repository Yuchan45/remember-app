package com.example.uade.rememberapp.domain.model

/**
 * Qué tan insistente es el aviso de un recordatorio.
 *
 * Cada nivel tiene su canal de notificación (ver NotificationHelper): ahí se decide el sonido,
 * la vibración y si aparece arriba de la pantalla. El usuario los puede ajustar en Android.
 */
enum class Importance {
    /** Silencioso: sin sonido, vibración ni ícono en la barra de estado. */
    Low,

    /** Con sonido y vibración. */
    Default,

    /** Aparece en pantalla como alerta emergente. */
    High,

    /** Suena como alarma aunque el teléfono esté en silencio, y se repite hasta marcarlo hecho. */
    Critical,
}

package com.example.uade.rememberapp.domain.model

/**
 * Qué tan insistente es el aviso de un recordatorio.
 *
 * TODO: por ahora solo se guarda. Usarlo para elegir el canal de notificación (silencioso,
 * con sonido, emergente o alarma que se repite).
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

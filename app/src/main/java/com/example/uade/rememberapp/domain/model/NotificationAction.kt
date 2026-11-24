package com.example.uade.rememberapp.domain.model

/**
 * Una acción rápida que se ofrece en la notificación del recordatorio, ej. "Llamar al número:
 * Pediatría". Un recordatorio tiene como mucho una.
 *
 * La notificación la ofrece como un botón que abre la app que corresponde (marcador, SMS,
 * WhatsApp, Instagram o correo) ya con el destinatario; ver NotificationHelper.
 */
sealed interface NotificationAction {
    data class Call(val phone: String) : NotificationAction

    /** [to] es un número (SMS y WhatsApp) o un usuario (Instagram, sin "@"). */
    data class Message(
        val channel: MessageChannel,
        val to: String,
        val text: String? = null,
    ) : NotificationAction

    data class Email(
        val to: String,
        val subject: String? = null,
    ) : NotificationAction
}

/** Por dónde se envía el mensaje de [NotificationAction.Message]. */
enum class MessageChannel {
    Sms,
    WhatsApp,
    Instagram,
}

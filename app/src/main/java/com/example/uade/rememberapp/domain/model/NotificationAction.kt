package com.example.uade.rememberapp.domain.model

/**
 * Una acción rápida que se ofrece en la notificación del recordatorio, ej. "Llamar al número:
 * Pediatría". Un recordatorio tiene como mucho una.
 *
 * TODO: por ahora solo se guarda. Ejecutarla desde la notificación con un Intent (marcador,
 * SMS / WhatsApp / Instagram, cliente de correo).
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

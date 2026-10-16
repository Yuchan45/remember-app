package com.example.uade.rememberapp.ui.reminders.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.ui.theme.InstagramPink
import com.example.uade.rememberapp.ui.theme.WhatsAppGreen

/**
 * Los tipos de acción de notificación que se ofrecen en el modal, con su ícono y su nombre.
 * El dominio no tiene recursos de Android: este enum es su cara en la UI.
 */
enum class NotificationActionKind(
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
) {
    Call(R.drawable.ic_call, R.string.reminder_action_call),
    Message(R.drawable.ic_send, R.string.reminder_action_message),
    Email(R.drawable.ic_mail, R.string.reminder_action_email),
    ;

    companion object {
        /** El tipo de una acción del dominio. */
        fun of(action: NotificationAction): NotificationActionKind = when (action) {
            is NotificationAction.Call -> Call
            is NotificationAction.Message -> Message
            is NotificationAction.Email -> Email
        }
    }
}

/**
 * Cada canal de "Enviar mensaje" en la UI: ícono, nombre, color y qué se le pide al usuario.
 *
 * - [brandColor]: el color de la marca para el ícono y la tarjeta elegida; null = el color de
 *   acento de la app (SMS no tiene marca).
 * - [targetLabel]: la etiqueta del campo destinatario ("Número de teléfono" o "Usuario de
 *   Instagram").
 * - [isPhone]: si el destinatario es un número (teclado numérico) o un usuario (con "@").
 */
enum class MessageChannelOption(
    val channel: MessageChannel,
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
    val brandColor: Color?,
    @get:StringRes val targetLabel: Int,
    val isPhone: Boolean,
) {
    Sms(
        MessageChannel.Sms,
        R.drawable.ic_sms,
        R.string.reminder_action_channel_sms,
        brandColor = null,
        targetLabel = R.string.reminder_action_field_phone,
        isPhone = true,
    ),
    WhatsApp(
        MessageChannel.WhatsApp,
        R.drawable.ic_whatsapp,
        R.string.reminder_action_channel_whatsapp,
        brandColor = WhatsAppGreen,
        targetLabel = R.string.reminder_action_field_phone,
        isPhone = true,
    ),
    Instagram(
        MessageChannel.Instagram,
        R.drawable.ic_instagram,
        R.string.reminder_action_channel_instagram,
        brandColor = InstagramPink,
        targetLabel = R.string.reminder_action_field_instagram,
        isPhone = false,
    ),
    ;

    companion object {
        fun of(channel: MessageChannel): MessageChannelOption = entries.first { it.channel == channel }
    }
}

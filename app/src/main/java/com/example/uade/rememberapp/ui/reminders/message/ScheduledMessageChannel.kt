package com.example.uade.rememberapp.ui.reminders.message

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.WhatsAppGreen

/**
 * Canales soportados para los mensajes programados.
 */
enum class ScheduledMessageChannel(
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
    val brandColor: Color,
) {
    WhatsApp(
        icon = R.drawable.ic_whatsapp,
        label = R.string.scheduled_message_channel_whatsapp,
        brandColor = WhatsAppGreen,
    ),
    Sms(
        icon = R.drawable.ic_sms,
        label = R.string.scheduled_message_channel_sms,
        brandColor = Color(0xFF3897F0),
    ),
    Email(
        icon = R.drawable.ic_mail,
        label = R.string.scheduled_message_channel_email,
        brandColor = Color(0xFFEA4335),
    ),
}

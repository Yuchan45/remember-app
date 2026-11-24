package com.example.uade.rememberapp.platform.message

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction

/**
 * Utilidad responsable de construir Intents del sistema y despachar la apertura de
 * aplicaciones externas de mensajería (WhatsApp, SMS, Correo y Llamadas telefónicas).
 */
object MessageLauncherHelper {

    /**
     * Construye un [Intent] con flags para iniciar una actividad externa configurada
     * según la acción rápida del recordatorio.
     */
    fun createSendIntent(
        context: Context,
        action: NotificationAction,
        defaultText: String? = null,
    ): Intent? = when (action) {
        is NotificationAction.Call -> {
            val cleanPhone = action.phone.filter { it.isDigit() || it == '+' }
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(cleanPhone)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        is NotificationAction.Message -> when (action.channel) {
            MessageChannel.WhatsApp -> {
                val cleanPhone = action.to.filter { it.isDigit() || it == '+' }.removePrefix("+")
                val text = action.text?.takeIf { it.isNotBlank() } ?: defaultText.orEmpty()
                val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}"
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            MessageChannel.Sms -> {
                val cleanPhone = action.to.filter { it.isDigit() || it == '+' }
                val text = action.text?.takeIf { it.isNotBlank() } ?: defaultText.orEmpty()
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(cleanPhone)}")).apply {
                    putExtra("sms_body", text)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            MessageChannel.Instagram -> {
                val username = action.to.removePrefix("@")
                Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$username")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }
        is NotificationAction.Email -> {
            val body = defaultText.orEmpty()
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(action.to.trim())}")).apply {
                if (!action.subject.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_SUBJECT, action.subject)
                }
                if (body.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    /**
     * Inicia la actividad correspondiente para enviar el mensaje o realizar la llamada,
     * mostrando un Toast informativo si no hay ninguna aplicación que pueda atender el Intent.
     */
    fun launchMessage(
        context: Context,
        action: NotificationAction,
        defaultText: String? = null,
    ): Boolean {
        val intent = createSendIntent(context, action, defaultText) ?: return false
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.scheduled_message_app_not_installed),
                Toast.LENGTH_SHORT,
            ).show()
            false
        }
    }

    /**
     * Devuelve el texto amigable para la acción en notificaciones o botones.
     */
    fun getActionLabel(context: Context, action: NotificationAction): String = when (action) {
        is NotificationAction.Call -> context.getString(R.string.notification_action_call)
        is NotificationAction.Email -> context.getString(R.string.notification_action_send_email)
        is NotificationAction.Message -> when (action.channel) {
            MessageChannel.WhatsApp -> context.getString(R.string.notification_action_send_whatsapp)
            MessageChannel.Sms -> context.getString(R.string.notification_action_send_sms)
            MessageChannel.Instagram -> "Instagram"
        }
    }

    /**
     * Devuelve el ícono vectorial correspondiente para la acción.
     */
    fun getActionIcon(action: NotificationAction): Int = when (action) {
        is NotificationAction.Call -> R.drawable.ic_call
        is NotificationAction.Email -> R.drawable.ic_mail
        is NotificationAction.Message -> when (action.channel) {
            MessageChannel.WhatsApp -> R.drawable.ic_whatsapp
            MessageChannel.Sms -> R.drawable.ic_sms
            MessageChannel.Instagram -> R.drawable.ic_instagram
        }
    }
}

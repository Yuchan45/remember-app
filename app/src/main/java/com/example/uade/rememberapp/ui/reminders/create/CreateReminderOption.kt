package com.example.uade.rememberapp.ui.reminders.create

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.CreateAudioColor
import com.example.uade.rememberapp.ui.theme.CreateNoteColor
import com.example.uade.rememberapp.ui.theme.CreateScheduledMessageColor

/**
 * Tipos de recordatorio que se pueden crear desde el botón "+", en el orden del menú.
 * Cada uno trae su ícono, el color de fondo del ícono, el nombre y una descripción corta.
 *
 * No hay opción "Checklist": la lista es parte de la nota (se agrega desde el editor con
 * "Agregar lista"), así hay un solo punto de entrada para escribir.
 */
enum class CreateReminderOption(
    @get:DrawableRes val icon: Int,
    val iconBackground: Color,
    @get:StringRes val title: Int,
    @get:StringRes val description: Int,
) {
    Note(
        R.drawable.ic_description,
        CreateNoteColor,
        R.string.reminders_create_note,
        R.string.reminders_create_note_description,
    ),
    Audio(
        R.drawable.ic_mic,
        CreateAudioColor,
        R.string.reminders_create_audio,
        R.string.reminders_create_audio_description,
    ),
    ScheduledMessage(
        R.drawable.ic_send,
        CreateScheduledMessageColor,
        R.string.reminders_create_scheduled_message,
        R.string.reminders_create_scheduled_message_description,
    ),
}

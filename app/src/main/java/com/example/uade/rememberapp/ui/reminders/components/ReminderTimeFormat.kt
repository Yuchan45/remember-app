package com.example.uade.rememberapp.ui.reminders.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** true si [at] cae el mismo día calendario que [now] en la zona horaria del teléfono. */
internal fun isSameDay(at: Instant, now: Instant, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    at.atZone(zone).toLocalDate() == now.atZone(zone).toLocalDate()

/**
 * Hora de un aviso como la muestra la card: "18:00" si es hoy, "Sáb 11:00" si es otro día.
 *
 * El día sale del [locale] (en inglés sería "Sat 11:00"). Se le saca el punto de la
 * abreviatura ("sáb." → "Sáb") y se pone en mayúscula.
 */
internal fun formatReminderTime(
    at: Instant,
    now: Instant,
    locale: Locale,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val dateTime = at.atZone(zone)
    val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    if (isSameDay(at, now, zone)) return time

    val day = dateTime.format(DateTimeFormatter.ofPattern("EEE", locale))
        .removeSuffix(".")
        .replaceFirstChar { it.titlecase(locale) }
    return "$day $time"
}

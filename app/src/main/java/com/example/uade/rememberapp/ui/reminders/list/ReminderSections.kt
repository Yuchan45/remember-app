package com.example.uade.rememberapp.ui.reminders.list

import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.nextAlarm
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

private fun String.normalizedForSearch(): String =
    Normalizer.normalize(lowercase(Locale.getDefault()), Normalizer.Form.NFD)
        .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }

/**
 * Filtra [reminders] según [query] ignorando mayúsculas y tildes. Busca en título, descripción,
 * transcripción de voz, ítems de checklist (texto y nota), etiquetas, lugares y destinatarios de
 * acciones programadas.
 */
internal fun filterRemindersByQuery(
    reminders: List<Reminder>,
    query: String,
    placeNames: Map<Long, String> = emptyMap(),
): List<Reminder> {
    val cleanQuery = query.trim().normalizedForSearch()
    if (cleanQuery.isEmpty()) return reminders

    return reminders.filter { reminder ->
        reminder.title?.normalizedForSearch()?.contains(cleanQuery) == true ||
            reminder.description?.normalizedForSearch()?.contains(cleanQuery) == true ||
            reminder.transcript?.normalizedForSearch()?.contains(cleanQuery) == true ||
            reminder.items.any { item ->
                item.text.normalizedForSearch().contains(cleanQuery) ||
                    item.note?.normalizedForSearch()?.contains(cleanQuery) == true
            } ||
            reminder.tags.any { tag ->
                tag.name.normalizedForSearch().contains(cleanQuery)
            } ||
            reminder.places.any { alert ->
                placeNames[alert.placeId]?.normalizedForSearch()?.contains(cleanQuery) == true
            } ||
            when (val action = reminder.action) {
                is NotificationAction.Call -> action.phone.normalizedForSearch().contains(cleanQuery)
                is NotificationAction.Message -> action.to.normalizedForSearch().contains(cleanQuery) ||
                    action.text?.normalizedForSearch()?.contains(cleanQuery) == true
                is NotificationAction.Email -> action.to.normalizedForSearch().contains(cleanQuery) ||
                    action.subject?.normalizedForSearch()?.contains(cleanQuery) == true
                null -> false
            }
    }
}

/**
 * Arma las secciones de la Home agrupando por la fecha del próximo aviso por hora (si ya pasaron
 * todos, el último; ver nextAlarm):
 * - Hoy: avisos de hoy, y también los vencidos de días anteriores (para que no se pierdan).
 * - Mañana / Próximos: avisos de mañana / de más adelante.
 * - Sin fecha: sin avisos por hora (sin avisos o solo por lugar).
 *
 * Dentro de cada sección, los que tienen hora van del más próximo al más lejano; los sin
 * fecha, del más nuevo al más viejo. Las secciones vacías no se muestran.
 *
 * Es una función pura (sin reloj propio) para poder testearla con cualquier "ahora".
 */
internal fun groupByDate(
    reminders: List<Reminder>,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): List<ReminderSection> {
    val today = now.atZone(zone).toLocalDate()
    val tomorrow = today.plusDays(1)

    val byKey = reminders.groupBy { reminder ->
        val at = reminder.nextAlarm(now)?.at
        if (at == null) {
            ReminderSectionKey.NoDate
        } else {
            val day = at.atZone(zone).toLocalDate()
            when {
                !day.isAfter(today) -> ReminderSectionKey.Today
                day == tomorrow -> ReminderSectionKey.Tomorrow
                else -> ReminderSectionKey.Upcoming
            }
        }
    }

    // ReminderSectionKey.entries ya está en el orden en que se muestran las secciones.
    return ReminderSectionKey.entries.mapNotNull { key ->
        val items = byKey[key] ?: return@mapNotNull null
        val sorted = if (key == ReminderSectionKey.NoDate) {
            items.sortedByDescending { it.createdAt }
        } else {
            items.sortedBy { checkNotNull(it.nextAlarm(now)).at }
        }
        ReminderSection(key = key, reminders = sorted)
    }
}

package com.example.uade.rememberapp.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

/**
 * Un aviso por hora de un recordatorio. Un recordatorio puede tener varios.
 *
 * [isSnooze]: lo creó "Posponer" desde la notificación (no el usuario al elegir la hora). Hay
 * como mucho uno por recordatorio: posponer de nuevo reemplaza al anterior (ver
 * SnoozeReminderUseCase).
 *
 * [repeat]: si se repite, [at] es siempre la PRÓXIMA vez que suena. Cuando suena, se reemplaza
 * por la siguiente (ver AdvanceRepeatingAlarmUseCase), así el resto de la app no necesita saber
 * de repeticiones: ve un aviso a una hora, como cualquier otro. Un pospuesto nunca se repite.
 */
data class Alarm(
    val at: Instant,
    val isSnooze: Boolean = false,
    val repeat: AlarmRepeat = AlarmRepeat.None,
)

/** Cada cuánto se repite un aviso por hora. */
enum class AlarmRepeat {
    None,
    Daily,

    /** De lunes a viernes. */
    Weekdays,
    Weekly,
}

/**
 * La próxima vez que suena este aviso después de [after], a la misma hora del día en [zone]
 * (así un aviso de las 8:00 sigue a las 8:00 aunque cambie el horario de verano). Null si no se
 * repite.
 */
fun Alarm.nextOccurrence(after: Instant, zone: ZoneId): Instant? {
    if (repeat == AlarmRepeat.None) return null
    var next = at.atZone(zone)
    // Un loop simple alcanza: aunque el teléfono haya estado apagado un año, son pocas vueltas.
    while (!next.toInstant().isAfter(after)) {
        next = when (repeat) {
            AlarmRepeat.Daily -> next.plusDays(1)
            AlarmRepeat.Weekly -> next.plusWeeks(1)
            AlarmRepeat.Weekdays -> {
                var day = next.plusDays(1)
                while (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) {
                    day = day.plusDays(1)
                }
                day
            }
            AlarmRepeat.None -> return null
        }
    }
    return next.toInstant()
}

/**
 * Los avisos que se repiten y ya pasaron, corridos a su próxima vez después de [now] (ej. al
 * prender el teléfono después de unos días). Los que no se repiten quedan igual.
 */
fun List<Alarm>.rolledForward(now: Instant, zone: ZoneId): List<Alarm> = map { alarm ->
    if (alarm.repeat != AlarmRepeat.None && !alarm.at.isAfter(now)) {
        alarm.nextOccurrence(now, zone)?.let { alarm.copy(at = it) } ?: alarm
    } else {
        alarm
    }
}

/** Un aviso por lugar: al llegar a [placeId] o al salir de él. Un recordatorio puede tener varios. */
data class PlaceAlert(
    val placeId: Long,
    val event: PlaceAlertEvent = PlaceAlertEvent.Arrive,
)

/** Si el aviso por lugar salta al llegar o al salir. */
enum class PlaceAlertEvent {
    Arrive,
    Leave,
}

/**
 * El aviso por hora que más importa mostrar: el próximo que todavía no pasó, o, si ya pasaron
 * todos, el último (así la card puede marcarlo como vencido). Null si no tiene avisos por hora.
 *
 * Se calcula en vez de guardarse porque depende de la hora actual.
 */
fun Reminder.nextAlarm(now: Instant): Alarm? =
    alarms.filter { !it.at.isBefore(now) }.minByOrNull { it.at }
        ?: alarms.maxByOrNull { it.at }

/** true si el aviso es dentro de menos de [window] (o ya pasó). */
fun Alarm.isDueSoon(now: Instant, window: Duration = 1.hours): Boolean =
    !at.isAfter(now.plus(window.toJavaDuration()))

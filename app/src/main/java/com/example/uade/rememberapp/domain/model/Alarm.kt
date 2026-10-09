package com.example.uade.rememberapp.domain.model

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

/**
 * Un aviso por hora de un recordatorio. Un recordatorio puede tener varios.
 *
 * Es una clase (y no un Instant suelto) para poder sumarle la repetición más adelante.
 * TODO: guardar la repetición (diario, lunes a viernes, semanal).
 */
data class Alarm(val at: Instant)

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

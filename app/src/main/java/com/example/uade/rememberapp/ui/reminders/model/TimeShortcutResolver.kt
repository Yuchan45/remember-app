package com.example.uade.rememberapp.ui.reminders.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Hora que se usa si en "Elegir fecha…" se eligió el día pero no la hora. */
internal val DefaultPickedTime: LocalTime = LocalTime.of(9, 0)

private val TonightTime: LocalTime = LocalTime.of(21, 0)
private val MorningTime: LocalTime = LocalTime.of(9, 0)
private val WeekendTime: LocalTime = LocalTime.of(10, 0)

/**
 * Convierte lo elegido en el panel "Fecha y hora" en un momento concreto, o null si no se
 * eligió nada (la nota queda sin aviso).
 *
 * Es una función pura (recibe [now] en vez de leer el reloj) para poder testearla con
 * cualquier día y hora.
 *
 * - "En 1 hora": ahora + 1 h, sin segundos.
 * - "Hoy 21:00": hoy a las 21; si ya pasaron, mañana a las 21.
 * - "Mañana 9:00": mañana a las 9.
 * - "Sáb 10:00": el próximo sábado a las 10 (hoy, si es sábado y todavía no son las 10).
 * - "Elegir fecha…": el día elegido, a la hora elegida o a las 9 si no se eligió.
 */
internal fun resolveReminderTime(
    shortcut: TimeShortcut?,
    pickedDate: LocalDate?,
    pickedTime: LocalTime?,
    now: ZonedDateTime,
): ZonedDateTime? {
    val today = now.toLocalDate()
    return when (shortcut) {
        null -> null
        TimeShortcut.InOneHour -> now.plusHours(1).truncatedTo(ChronoUnit.MINUTES)
        TimeShortcut.Tonight -> {
            val tonight = now.with(today.atTime(TonightTime))
            if (tonight.isAfter(now)) tonight else tonight.plusDays(1)
        }
        TimeShortcut.TomorrowMorning -> now.with(today.plusDays(1).atTime(MorningTime))
        TimeShortcut.Weekend -> {
            val saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            val candidate = now.with(saturday.atTime(WeekendTime))
            if (candidate.isAfter(now)) candidate else candidate.plusWeeks(1)
        }
        TimeShortcut.PickDate -> pickedDate?.let { now.with(it.atTime(pickedTime ?: DefaultPickedTime)) }
    }
}

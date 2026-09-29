package com.example.uade.rememberapp.domain.model

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

/** Cómo vuelve el recordatorio: nunca, a una hora, o al llegar a un lugar. */
sealed interface Trigger {
    data object None : Trigger
    data class AtTime(val at: Instant) : Trigger
    data class AtPlace(val placeId: Long) : Trigger
}

/**
 * true si el aviso es por hora y falta menos de [window] (o ya pasó). Se calcula en vez de
 * guardarse porque depende de la hora actual.
 */
fun Trigger.isDueSoon(now: Instant, window: Duration = 1.hours): Boolean =
    this is Trigger.AtTime && !at.isAfter(now.plus(window.toJavaDuration()))

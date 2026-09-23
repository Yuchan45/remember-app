package com.example.uade.rememberapp.domain.model

import java.time.Instant

/** Cómo vuelve el recordatorio: nunca, a una hora, o al llegar a un lugar. */
sealed interface Trigger {
    data object None : Trigger
    data class AtTime(val at: Instant) : Trigger
    data class AtPlace(val placeId: Long) : Trigger
}

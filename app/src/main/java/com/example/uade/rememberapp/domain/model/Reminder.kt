package com.example.uade.rememberapp.domain.model

import java.time.Instant

/** Un pendiente capturado por el usuario: texto y/o foto, con un disparador opcional. */
data class Reminder(
    val id: Long = 0,
    val text: String,
    val photoPath: String? = null,
    val trigger: Trigger = Trigger.None,
    val isDone: Boolean = false,
    val createdAt: Instant,
)

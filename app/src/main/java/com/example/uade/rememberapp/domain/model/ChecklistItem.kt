package com.example.uade.rememberapp.domain.model

/** Un ítem de un recordatorio de tipo lista, ej. "Vacío 5kg" con la nota "En el Coto". */
data class ChecklistItem(
    val id: Long = 0,
    val text: String,
    val note: String? = null,
    val isChecked: Boolean = false,
)

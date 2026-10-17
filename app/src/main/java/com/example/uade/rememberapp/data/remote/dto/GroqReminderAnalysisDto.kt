package com.example.uade.rememberapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Estructura del JSON que devuelve el modelo de Groq (Llama 3.3) al estructurar un recordatorio.
 */
data class GroqReminderAnalysisDto(
    @SerializedName("summary_badge")
    val summaryBadge: String? = null,

    @SerializedName("is_checklist_candidate")
    val isChecklistCandidate: Boolean = false,

    @SerializedName("reminders")
    val reminders: List<GroqParsedReminderDto> = emptyList(),
)

data class GroqParsedReminderDto(
    @SerializedName("title")
    val title: String = "",

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("type")
    val type: String = "Note",

    @SerializedName("trigger_type")
    val triggerType: String? = null, // "TIME", "PLACE", "NONE"

    @SerializedName("trigger_iso")
    val triggerIso: String? = null,

    @SerializedName("trigger_display")
    val triggerDisplay: String? = null, // ej. "Mañana 09:00", "Hoy 22:00", "Sin fecha"

    @SerializedName("place_name")
    val placeName: String? = null, // ej. "Casa", "Al pasar por Súper"

    @SerializedName("priority")
    val priority: String? = null, // ej. "Alta", "Normal"

    @SerializedName("list_name")
    val listName: String? = null, // ej. "Súper", "Viaje"

    @SerializedName("items")
    val items: List<String> = emptyList(),

    @SerializedName("separate")
    val separate: Boolean = false, // true si se sugiere crear aparte (03.D)
)

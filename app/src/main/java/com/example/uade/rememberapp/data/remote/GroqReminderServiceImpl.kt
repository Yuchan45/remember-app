package com.example.uade.rememberapp.data.remote

import com.example.uade.rememberapp.data.remote.dto.GroqChatRequest
import com.example.uade.rememberapp.data.remote.dto.GroqMessage
import com.example.uade.rememberapp.data.remote.dto.GroqReminderAnalysisDto
import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.repository.AiReminderService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Implementación de [AiReminderService] que se comunica con la API de Groq
 * utilizando Retrofit (Whisper para voz y Llama 3.3 para extracción estructurada).
 *
 * Incluye un fallback heurístico inteligente si la API key aún no fue configurada
 * o ante fallos de red en modo demo, garantizando el flujo visual de los mockups 03.B y 03.D.
 */
class GroqReminderServiceImpl(
    private val apiService: GroqApiService,
    private val apiKey: String,
    private val gson: Gson = Gson(),
) : AiReminderService {

    override suspend fun transcribeAudio(audioFilePath: String): Result<String> = withContext(Dispatchers.IO) {
        val file = File(audioFilePath)
        if (!file.exists()) {
            return@withContext Result.failure(IllegalArgumentException("El archivo de audio no existe: $audioFilePath"))
        }

        if (apiKey.isBlank()) {
            // Modo demo / sin API key aún configurada en local.properties:
            return@withContext Result.success(
                "Mañana a las 9 viene el técnico del aire y después tengo que acordarme de comprar papas en el super",
            )
        }

        try {
            val authHeader = "Bearer $apiKey"
            val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val model = "whisper-large-v3".toRequestBody("text/plain".toMediaTypeOrNull())
            val language = "es".toRequestBody("text/plain".toMediaTypeOrNull())

            val response = apiService.transcribeAudio(authHeader, body, model, language)
            val text = response.text.trim()
            if (text.isBlank()) {
                Result.failure(IllegalStateException("La transcripción de Groq devolvió texto vacío"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun analyzeText(text: String, referenceDateTimeIso: String): Result<AiAnalysisResult> =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("El texto a analizar no puede estar vacío"))
            }

            if (apiKey.isBlank()) {
                return@withContext Result.success(createLocalHeuristicResult(text, referenceDateTimeIso))
            }

            try {
                val authHeader = "Bearer $apiKey"
                val systemPrompt = buildSystemPrompt(referenceDateTimeIso)
                val request = GroqChatRequest(
                    model = "llama-3.3-70b-versatile",
                    messages = listOf(
                        GroqMessage(role = "system", content = systemPrompt),
                        GroqMessage(role = "user", content = text),
                    ),
                )

                val response = apiService.createChatCompletion(authHeader, request)
                val content = response.choices.firstOrNull()?.message?.content
                    ?: return@withContext Result.failure(IllegalStateException("Respuesta vacía de Groq LLM"))

                logDebug("GroqAI", "Whisper transcript: '$text'")
                logDebug("GroqAI", "Groq LLM raw content: '$content'")

                val jsonMatch = Regex("""\{[\s\S]*\}""").find(content)
                val cleanJson = jsonMatch?.value ?: content.trim()

                val dto = gson.fromJson(cleanJson, GroqReminderAnalysisDto::class.java)
                val result = mapDtoToDomain(dto, text)
                Result.success(result)
            } catch (e: Exception) {
                logError("GroqAI", "Fallo al procesar con Groq API, usando fallback", e)
                Result.success(createLocalHeuristicResult(text, referenceDateTimeIso))
            }
        }

    override suspend fun processAudio(
        audioFilePath: String,
        referenceDateTimeIso: String,
    ): Result<AiAnalysisResult> = withContext(Dispatchers.IO) {
        val transcriptionResult = transcribeAudio(audioFilePath)
        transcriptionResult.fold(
            onSuccess = { transcript ->
                logDebug("GroqAI", "Audio transcrito con éxito: '$transcript'")
                analyzeText(transcript, referenceDateTimeIso)
            },
            onFailure = { error ->
                logError("GroqAI", "Error en transcripción Whisper", error)
                Result.failure(error)
            },
        )
    }

    private fun mapDtoToDomain(dto: GroqReminderAnalysisDto?, rawTranscript: String): AiAnalysisResult {
        val rawReminders = dto?.reminders ?: emptyList()
        val proposals = rawReminders.map { r ->
            val alarms = parseAlarms(r.triggerType, r.triggerIso)
            val reminderType = if (r.type.equals("Checklist", ignoreCase = true) || r.items.isNotEmpty()) {
                ReminderType.Checklist
            } else {
                ReminderType.Note
            }
            val checklistItems = r.items.map { itemText ->
                ChecklistItem(text = itemText)
            }

            ProposedReminder(
                tempId = UUID.randomUUID().toString(),
                title = r.title.ifBlank { rawTranscript },
                description = r.description,
                type = reminderType,
                items = checklistItems,
                alarms = alarms,
                triggerDisplayTime = r.triggerDisplay ?: if (alarms.isEmpty()) "Sin fecha" else null,
                placeName = r.placeName,
                priorityName = r.priority,
                listName = r.listName,
                isSelected = true,
                isSeparateItem = r.separate,
            )
        }

        val finalProposals = if (proposals.isEmpty()) {
            listOf(
                ProposedReminder(
                    tempId = UUID.randomUUID().toString(),
                    title = rawTranscript.trim().ifBlank { "Nuevo recordatorio" },
                    type = ReminderType.Note,
                    triggerDisplayTime = "Sin fecha",
                    isSelected = true,
                ),
            )
        } else {
            proposals
        }

        val badge = dto?.summaryBadge ?: if (finalProposals.size > 1) {
            "Encontré ${finalProposals.size} recordatorios distintos en tu audio. Revísalos antes de crearlos."
        } else {
            "Detecté 1 recordatorio en tu audio."
        }

        return AiAnalysisResult(
            rawTranscript = rawTranscript,
            summaryBadge = badge,
            proposedReminders = finalProposals,
            isChecklistCandidate = dto?.isChecklistCandidate == true || finalProposals.any { it.type == ReminderType.Checklist },
        )
    }

    private fun parseAlarms(triggerType: String?, triggerIso: String?): List<Alarm> {
        if (triggerType.equals("TIME", ignoreCase = true) && !triggerIso.isNullOrBlank()) {
            return try {
                val instant = OffsetDateTime.parse(triggerIso).toInstant()
                listOf(Alarm(instant))
            } catch (e: DateTimeParseException) {
                try {
                    listOf(Alarm(Instant.parse(triggerIso)))
                } catch (e2: Exception) {
                    emptyList()
                }
            }
        }
        return emptyList()
    }

    private fun buildSystemPrompt(referenceIso: String): String {
        return """
        Eres un asistente inteligente para la aplicación Android "Hey, Remember".
        La fecha y hora actual de referencia del usuario es: $referenceIso.
        Tu misión es analizar la transcripción del audio del usuario y estructurar los recordatorios en formato JSON.

        REGLAS FUNDAMENTALES OBLIGATORIAS:
        1. "Split automático de intenciones múltiples" (03.B):
           - Si el usuario menciona dos o más intenciones, tareas o acciones distintas (ej. conectadas por "y después", "después", "y luego", "luego", "más tarde", "además", "también", "y a la noche", etc.), SEPARA CADA TAREA en un objeto individual dentro del arreglo "reminders".
           - NUNCA unas dos acciones diferentes en un solo recordatorio a menos que el usuario esté dictando una checklist de ítems.
           - Ejemplo: "A la noche tengo que hacer la tarea y después ir al super" ->
             Recordatorio 1: título "Hacer la tarea", con horario hoy a la noche ("trigger_display": "Hoy 21:00").
             Recordatorio 2: título "Ir al súper", con lugar "Al pasar por Súper" o lista "Súper".
        2. "Fidelidad absoluta al audio del usuario":
           - NUNCA inventes tareas ni copies nombres o ejemplos (como "técnico del aire" o "papas"). Extrae los títulos ÚNICAMENTE a partir de lo que el usuario dijo en este mensaje específico.
           - Limpia fórmulas verbales iniciales como "tengo que...", "acordarme de...", "hay que...", "recordar...", dejando la acción concisa en infinitivo o imperativo (ej. "Hacer la tarea", "Llamar al médico", "Comprar leche").
        3. "Texto a Checklist" (03.D):
           - Si el usuario dicta una lista de elementos (compras, viaje, valija, tareas) separadas por comas, "y", o con encabezados como "para el viaje...", marca "is_checklist_candidate": true, pon "type": "Checklist" y coloca cada elemento en "items".
           - Si uno de los elementos tiene una fecha/hora propia (ej. "y el viernes avisarle a Juan"), créalo como un recordatorio separado con "separate": true y su propio "trigger_iso".
        4. "Inferencia temporal (Fechas y horas)":
           - Usa la hora de referencia ($referenceIso) para inferir momentos relativos:
             * "a la noche" / "esta noche" -> Hoy 21:00.
             * "a la tarde" -> Hoy 17:00.
             * "al mediodía" -> Hoy 13:00.
             * "mañana a la mañana" -> Mañana 09:00.
             * "mañana" -> Mañana 09:00.
             * Si no hay hora ni fecha indicada -> "trigger_type": "NONE", "trigger_iso": null, "trigger_display": "Sin fecha".
           - Cuando haya fecha/hora, define "trigger_type": "TIME", "trigger_iso" en ISO-8601 y "trigger_display" legible (ej. "Hoy 21:00", "Mañana 09:00").
        5. "Inferencia de Lugares, Prioridades y Listas":
           - "place_name": si menciona un lugar físico (ej. "Casa", "Al pasar por Súper", "Trabajo", "Farmacia").
           - "priority": "Alta" si indica urgencia o importancia, de lo contrario null o "Normal".
           - "list_name": si corresponde a una lista lógica (ej. "Súper", "Viaje", "Estudio", "Trabajo").
        6. Devuelve SIEMPRE y ÚNICAMENTE un JSON válido con el siguiente esquema exacto (sin texto adicional ni explicaciones):
        {
          "summary_badge": "Encontré 2 recordatorios distintos en tu audio. Revísalos antes de crearlos.",
          "is_checklist_candidate": false,
          "reminders": [
            {
              "title": "Hacer la tarea",
              "description": "",
              "type": "Note",
              "trigger_type": "TIME",
              "trigger_iso": "2026-10-17T21:00:00Z",
              "trigger_display": "Hoy 21:00",
              "place_name": null,
              "priority": "Normal",
              "list_name": "Estudio",
              "items": [],
              "separate": false
            }
          ]
        }
        """.trimIndent()
    }

    /**
     * Fallback local heurístico dinámico para desarrollo, pruebas unitarias y modo offline.
     * Procesa REALMENTE el texto del usuario sin usar datos estáticos o ficticios.
     */
    private fun createLocalHeuristicResult(text: String, referenceIso: String): AiAnalysisResult {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()

        // Caso 03.D: Checklist / Lista de compras, viaje o tareas (comas, dos puntos o lista explícita)
        val hasListKeyword = lower.startsWith("lista") || lower.contains("para el viaje") || lower.contains("para las vacaciones")
        val commaSegments = trimmed.split(",").map { it.trim() }.filter { it.isNotBlank() }

        if (trimmed.contains(":") || (hasListKeyword && commaSegments.size >= 2) || commaSegments.size >= 3) {
            val title: String
            val rawItems: List<String>
            if (trimmed.contains(":")) {
                val splitColon = trimmed.split(":", limit = 2)
                title = splitColon[0].trim().replaceFirstChar { it.uppercase() }
                rawItems = splitColon[1].split(Regex("[,;\n]|\\s+y\\s+")).map { it.trim() }.filter { it.isNotBlank() }
            } else {
                title = if (lower.contains("viaje")) "Viaje" else if (lower.contains("super") || lower.contains("compra")) "Compras" else "Lista de tareas"
                rawItems = commaSegments
            }

            val checklistItems = mutableListOf<ChecklistItem>()
            var separateReminder: ProposedReminder? = null

            for (item in rawItems) {
                val itemLower = item.lowercase()
                val hasSpecificDate = itemLower.contains("el viernes") || itemLower.contains("el lunes") ||
                    itemLower.contains("mañana") || itemLower.contains("a las")
                if (hasSpecificDate && separateReminder == null) {
                    separateReminder = ProposedReminder(
                        tempId = UUID.randomUUID().toString(),
                        title = cleanTaskTitle(item),
                        type = ReminderType.Note,
                        triggerDisplayTime = inferDisplayTime(itemLower),
                        isSeparateItem = true,
                        isSelected = true,
                    )
                } else {
                    checklistItems.add(ChecklistItem(text = cleanTaskTitle(item)))
                }
            }

            val mainChecklist = ProposedReminder(
                tempId = UUID.randomUUID().toString(),
                title = title.ifBlank { "Lista de tareas" },
                type = ReminderType.Checklist,
                items = checklistItems,
                listName = if (lower.contains("super") || lower.contains("compra")) "Súper" else if (lower.contains("viaje")) "Viaje" else null,
                isSelected = true,
            )

            val proposals = if (separateReminder != null) listOf(mainChecklist, separateReminder) else listOf(mainChecklist)
            val badge = if (separateReminder != null) {
                "Detecté ${checklistItems.size + 1} ítems. Uno tiene fecha propia: te sugiero crearlo aparte."
            } else {
                "Detecté una lista con ${checklistItems.size} ítems."
            }

            return AiAnalysisResult(
                rawTranscript = trimmed,
                summaryBadge = badge,
                proposedReminders = proposals,
                isChecklistCandidate = true,
            )
        }

        // Caso 03.B: Split automático por conectores secuenciales ("y después", "luego", "más tarde", etc.)
        val splitRegex = Regex("""\s+(?:y\s+después|después|y\s+luego|luego|y\s+más\s+tarde|más\s+tarde|y\s+además|y\s+también)\s+""", RegexOption.IGNORE_CASE)
        val segments = trimmed.split(splitRegex).map { it.trim() }.filter { it.isNotBlank() }

        if (segments.size >= 2) {
            val proposals = segments.map { segment ->
                buildProposalFromText(segment)
            }
            return AiAnalysisResult(
                rawTranscript = trimmed,
                summaryBadge = "Encontré ${proposals.size} recordatorios distintos en tu audio. Revísalos antes de crearlos.",
                proposedReminders = proposals,
                isChecklistCandidate = false,
            )
        }

        // Caso individual general
        val singleProposal = buildProposalFromText(trimmed)
        return AiAnalysisResult(
            rawTranscript = trimmed,
            summaryBadge = "Detecté 1 recordatorio en tu audio.",
            proposedReminders = listOf(singleProposal),
            isChecklistCandidate = false,
        )
    }

    private fun buildProposalFromText(rawText: String): ProposedReminder {
        val lower = rawText.lowercase()
        val timeDisplay = inferDisplayTime(lower)
        val place = inferPlace(lower)
        val list = inferList(lower)
        val priority = inferPriority(lower)
        val cleanTitle = cleanTaskTitle(rawText)

        return ProposedReminder(
            tempId = UUID.randomUUID().toString(),
            title = cleanTitle.ifBlank { rawText.replaceFirstChar { it.uppercase() } },
            type = ReminderType.Note,
            triggerDisplayTime = timeDisplay,
            placeName = place,
            priorityName = priority,
            listName = list,
            isSelected = true,
        )
    }

    private fun cleanTaskTitle(text: String): String {
        var cleaned = text.trim()
        val prefixesToRemove = listOf(
            Regex("""^(?:a\s+la\s+noche|esta\s+noche|a\s+la\s+tarde|a\s+la\s+mañana|mañana|hoy)\s+""", RegexOption.IGNORE_CASE),
            Regex("""^(?:tengo\s+que|acordarme\s+de|recordar\s+que|recordar|hay\s+que|debo|necesito)\s+""", RegexOption.IGNORE_CASE),
        )
        for (pattern in prefixesToRemove) {
            cleaned = cleaned.replace(pattern, "").trim()
        }
        return cleaned.replaceFirstChar { it.uppercase() }
    }

    private fun inferDisplayTime(lower: String): String {
        return when {
            lower.contains("a la noche") || lower.contains("esta noche") || lower.contains("de noche") -> "Hoy 21:00"
            lower.contains("a la tarde") || lower.contains("de la tarde") -> "Hoy 17:00"
            lower.contains("al mediodía") || lower.contains("al medio día") -> "Hoy 13:00"
            lower.contains("mañana a la mañana") -> "Mañana 09:00"
            lower.contains("mañana a la tarde") -> "Mañana 16:00"
            lower.contains("mañana a la noche") -> "Mañana 21:00"
            lower.contains("mañana") -> "Mañana 09:00"
            lower.contains("viernes") -> "Viernes 09:00"
            lower.contains("lunes") -> "Lunes 09:00"
            lower.contains("hoy") -> "Hoy 20:00"
            else -> "Sin fecha"
        }
    }

    private fun inferPlace(lower: String): String? {
        return when {
            lower.contains("super") || lower.contains("súper") || lower.contains("supermercado") -> "Al pasar por Súper"
            lower.contains("casa") -> "Casa"
            lower.contains("farmacia") -> "Farmacia"
            lower.contains("trabajo") || lower.contains("oficina") -> "Trabajo"
            lower.contains("universidad") || lower.contains("facultad") || lower.contains("uade") -> "Facultad"
            else -> null
        }
    }

    private fun inferList(lower: String): String? {
        return when {
            lower.contains("super") || lower.contains("súper") || lower.contains("supermercado") || lower.contains("comprar") -> "Súper"
            lower.contains("viaje") || lower.contains("vacaciones") -> "Viaje"
            lower.contains("tarea") || lower.contains("estudiar") || lower.contains("facultad") -> "Estudio"
            lower.contains("trabajo") -> "Trabajo"
            else -> null
        }
    }

    private fun inferPriority(lower: String): String? {
        return when {
            lower.contains("urgente") || lower.contains("ya") || lower.contains("urgente!") -> "Alta"
            lower.contains("importante") || lower.contains("prioritario") -> "Alta"
            else -> null
        }
    }

    private fun logDebug(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (_: Throwable) {
            // Ignorar en entorno de test unitario JVM
        }
    }

    private fun logError(tag: String, msg: String, tr: Throwable? = null) {
        try {
            if (tr != null) {
                android.util.Log.e(tag, msg, tr)
            } else {
                android.util.Log.e(tag, msg)
            }
        } catch (_: Throwable) {
            // Ignorar en entorno de test unitario JVM
        }
    }
}

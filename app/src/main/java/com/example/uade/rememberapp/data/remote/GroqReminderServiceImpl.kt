package com.example.uade.rememberapp.data.remote

import com.example.uade.rememberapp.data.remote.dto.GroqChatRequest
import com.example.uade.rememberapp.data.remote.dto.GroqMessage
import com.example.uade.rememberapp.data.remote.dto.GroqParsedReminderDto
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
            return@withContext Result.failure(
                IllegalStateException("No se pudo procesar el audio: clave de API de Groq no configurada"),
            )
        }

        if (file.length() < 1200L) {
            return@withContext Result.failure(
                IllegalArgumentException("El archivo de audio está vacío o es demasiado corto"),
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
            if (text.isBlank() || isTranscriptBlankOrNoise(text)) {
                Result.failure(IllegalStateException("No se pudo procesar el audio: el audio está vacío o es ininteligible"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun analyzeText(text: String, referenceDateTimeIso: String): Result<AiAnalysisResult> =
        withContext(Dispatchers.IO) {
            if (text.isBlank() || isTranscriptBlankOrNoise(text)) {
                return@withContext Result.failure(IllegalArgumentException("El texto a analizar está vacío o es ininteligible"))
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

        // Si el LLM devolvió un único recordatorio pero este contiene tareas separables con conectores de acción (ej. "Hacer la tarea e ir al súper")
        val adjustedReminders = if (rawReminders.size == 1 && !rawReminders.first().type.equals("Checklist", ignoreCase = true)) {
            val singleTitle = rawReminders.first().title.ifBlank { rawTranscript }
            val splitSegments = singleTitle.split(TASK_SPLIT_REGEX).map { it.trim() }.filter { it.isNotBlank() }
            if (splitSegments.size >= 2) {
                splitSegments.mapIndexed { idx, segment ->
                    val cleanTitle = cleanTaskTitle(segment)
                    val lower = segment.lowercase()
                    val originalTriggerIso = rawReminders.first().triggerIso
                    val originalTriggerDisplay = rawReminders.first().triggerDisplay
                    val segmentDisplayTime = inferDisplayTime(lower).let { if (it == "Sin fecha" && idx == 0) originalTriggerDisplay ?: it else it }
                    val segmentPlace = inferPlace(lower) ?: if (idx == 0) rawReminders.first().placeName else null
                    val segmentList = inferList(lower) ?: if (idx == 0) rawReminders.first().listName else null

                    GroqParsedReminderDto(
                        title = cleanTitle,
                        description = segment,
                        type = "Note",
                        triggerType = if (segmentDisplayTime != "Sin fecha") "TIME" else "NONE",
                        triggerIso = if (segmentDisplayTime != "Sin fecha" && idx == 0) originalTriggerIso else null,
                        triggerDisplay = segmentDisplayTime,
                        placeName = segmentPlace,
                        priority = rawReminders.first().priority,
                        listName = segmentList,
                        items = emptyList(),
                        separate = false,
                    )
                }
            } else {
                rawReminders
            }
        } else {
            rawReminders
        }

        val proposals = adjustedReminders.map { r ->
            val resolvedTriggerIso = if (r.triggerType.equals("TIME", ignoreCase = true) && r.triggerIso.isNullOrBlank() && !r.triggerDisplay.isNullOrBlank()) {
                inferIsoTime(r.triggerDisplay!!.lowercase(), rawTranscript)
            } else {
                r.triggerIso
            }
            val alarms = parseAlarms(r.triggerType, resolvedTriggerIso ?: r.triggerIso)
            val isChecklist = r.type.equals("Checklist", ignoreCase = true) || r.items.isNotEmpty()
            val reminderType = if (isChecklist) {
                ReminderType.Checklist
            } else {
                ReminderType.Note
            }
            val checklistItems = r.items.map { itemText ->
                ChecklistItem(text = cleanChecklistItem(itemText))
            }

            // Separación limpia de Título vs Descripción
            val (resolvedTitle, resolvedDescription) = resolveTitleAndDescription(
                titleCandidate = r.title,
                descriptionCandidate = r.description,
                rawTranscript = rawTranscript,
                isSingleProposal = adjustedReminders.size == 1,
                isChecklist = isChecklist,
            )

            ProposedReminder(
                tempId = UUID.randomUUID().toString(),
                title = resolvedTitle,
                description = resolvedDescription,
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
            val (fallbackTitle, fallbackDesc) = resolveTitleAndDescription(
                titleCandidate = "",
                descriptionCandidate = null,
                rawTranscript = rawTranscript,
                isSingleProposal = true,
                isChecklist = false,
            )
            listOf(
                ProposedReminder(
                    tempId = UUID.randomUUID().toString(),
                    title = fallbackTitle,
                    description = fallbackDesc,
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

    private fun resolveTitleAndDescription(
        titleCandidate: String?,
        descriptionCandidate: String?,
        rawTranscript: String,
        isSingleProposal: Boolean,
        isChecklist: Boolean,
    ): Pair<String, String?> {
        val trimmedCandidateTitle = titleCandidate?.trim().orEmpty()
        val trimmedCandidateDesc = descriptionCandidate?.trim().orEmpty()

        if (isChecklist) {
            val finalTitle = if (trimmedCandidateTitle.isNotBlank()) {
                trimmedCandidateTitle
            } else {
                "Lista de tareas"
            }
            val finalDesc = if (trimmedCandidateDesc.isNotBlank()) {
                trimmedCandidateDesc
            } else if (rawTranscript.isNotBlank()) {
                rawTranscript.trim()
            } else {
                null
            }
            return Pair(finalTitle, finalDesc)
        }

        // Si el LLM devolvió título y descripción claros y no vacíos:
        if (trimmedCandidateTitle.isNotBlank() && trimmedCandidateDesc.isNotBlank()) {
            return Pair(trimmedCandidateTitle, trimmedCandidateDesc)
        }

        // Si la descripción vino vacía del LLM o si el título es largo:
        if (trimmedCandidateTitle.isNotBlank()) {
            val (shortTitle, extractedDesc) = extractTitleAndDescription(trimmedCandidateTitle)
            val finalDesc = if (!extractedDesc.isNullOrBlank() && extractedDesc != shortTitle) {
                extractedDesc
            } else if (rawTranscript.isNotBlank() && rawTranscript.length > shortTitle.length + 5) {
                rawTranscript.trim()
            } else if (trimmedCandidateDesc.isNotBlank()) {
                trimmedCandidateDesc
            } else {
                rawTranscript.trim().ifBlank { null }
            }
            return Pair(shortTitle, finalDesc)
        }

        // Si el título vino en blanco, usamos el rawTranscript procesado
        val (shortTitle, extractedDesc) = extractTitleAndDescription(rawTranscript)
        val finalDesc = extractedDesc ?: rawTranscript.trim().ifBlank { null }
        return Pair(shortTitle.ifBlank { "Nuevo recordatorio" }, finalDesc)
    }

    private fun extractTitleAndDescription(text: String): Pair<String, String?> {
        val cleaned = cleanTaskTitle(text)
        // Buscamos conectores de detalle o explicación como "para ", "porque ", "que ", "sobre ", "a fin de "
        val explanationRegex = Regex("""\s+(?:para|porque|sobre|a\s+fin\s+de)\s+""", RegexOption.IGNORE_CASE)
        val match = explanationRegex.find(cleaned)

        if (match != null && match.range.first in 5..45) {
            val titlePart = cleaned.substring(0, match.range.first).trim().replaceFirstChar { it.uppercase() }
            val descPart = cleaned.substring(match.range.first).trim().replaceFirstChar { it.uppercase() }
            return Pair(titlePart, descPart)
        }

        // Si tiene más de 6 palabras, tomamos las primeras 5 como título y el resto como descripción
        val words = cleaned.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.size > 6) {
            var titlePart = words.take(5).joinToString(" ").replaceFirstChar { it.uppercase() }
            titlePart = titlePart.replace(Regex("""\s+(?:e|y|de|con|a|en|el|la|al|del|los|las|un|una)$""", RegexOption.IGNORE_CASE), "")
            return Pair(titlePart, cleaned.replaceFirstChar { it.uppercase() })
        }

        // Si es corto, el título es la frase limpia y la descripción contiene el texto original para no dejarla vacía
        return Pair(cleaned.replaceFirstChar { it.uppercase() }, text.trim().ifBlank { null })
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
        1. "Separación estricta entre Título y Descripción":
           - "title": DEBE ser conciso, claro y directo (máximo 3 a 5 palabras, ej. "Comprar en el súper", "Llamar al médico", "Reunión de trabajo", "Hacer la tarea"). NUNCA coloques oraciones largas, explicaciones detalladas ni toda la transcripción en el título.
           - "description": DEBE contener los detalles, explicaciones, motivos, razones o notas secundarias que el usuario mencionó (ej. "Para pedir los análisis de sangre del lunes", "Porque pierde agua el caño de la cocina", "Cosas necesarias para el viaje"). Si el usuario dicta una nota o pensamiento personal, el título es el asunto y la descripción contiene el texto completo de la nota. ¡NUNCA dejes "description" vacía ("") si el audio contiene información adicional o explicativa!

        2. "Texto a Checklist" (03.D):
           - Si el usuario dicta una lista de compras, elementos, ingredientes, tareas o cosas a llevar (ej. "comprar leche, pan y huevos", "para el viaje llevar toalla, protector y ojotas", "necesito comprar azúcar, café y galletitas"):
             * Define "is_checklist_candidate": true.
             * En el recordatorio define "type": "Checklist".
             * Define un "title" conciso (ej. "Compras", "Para el viaje", "Lista de pendientes").
             * En "description" coloca el motivo o contexto si lo hay.
             * Coloca cada ítem individual limpio y capitalizado en el arreglo "items": ["Leche", "Pan", "Huevos"].
             * Si alguno de los elementos tiene una fecha/hora propia (ej. "y el viernes avisarle a Juan"), créalo como un recordatorio separado con "separate": true y su propio "trigger_iso".

        3. "Split automático de intenciones múltiples" (03.B):
           - Si el usuario menciona dos o más tareas o acciones independientes, SEPARA CADA TAREA en un objeto individual dentro del arreglo "reminders".
           - OBLIGATORIO: Esto incluye no solo conectores como "y después", "luego", "más tarde", sino MUY ESPECIALMENTE cuando dos acciones están unidas por "y" o "e" seguidas de un verbo de acción (ej. "hacer la tarea e ir al súper", "comprar el pan y llamar al médico", "pagar la luz y pasar por la farmacia", "a la noche hacer la tarea e ir al super").
           - NUNCA agrupes dos acciones con verbos distintos en un solo recordatorio ("Hacer la tarea e ir al súper" es un error grave: DEBEN ser 2 recordatorios individuales en "reminders":
             Recordatorio 1: "title": "Hacer la tarea", "trigger_display": "Hoy 21:00", "list_name": "Estudio".
             Recordatorio 2: "title": "Ir al súper", "place_name": "Al pasar por Súper", "list_name": "Súper").
           - Ejemplo: "A la noche tengo que hacer la tarea de matemática y después ir al super a comprar verduras" ->
             Recordatorio 1: "title": "Hacer la tarea", "description": "Ejercicios de matemática", "trigger_display": "Hoy 21:00", "list_name": "Estudio".
             Recordatorio 2: "title": "Ir al súper", "description": "Comprar verduras", "place_name": "Al pasar por Súper", "list_name": "Súper".

        4. "Fidelidad absoluta al audio del usuario":
           - NUNCA inventes tareas ni copies nombres de ejemplos. Extrae los datos ÚNICAMENTE de lo que el usuario dijo.
           - Limpia fórmulas verbales iniciales como "tengo que...", "acordarme de...", "hay que...", "recordar...", dejando la acción concisa en infinitivo o imperativo (ej. "Hacer la tarea", "Llamar al médico", "Comprar leche").

        5. "Inferencia temporal (Fechas y horas)":
           - Usa la hora de referencia ($referenceIso) para inferir momentos relativos:
             * "a la noche" / "esta noche" -> Hoy 21:00.
             * "a la tarde" -> Hoy 17:00.
             * "al mediodía" -> Hoy 13:00.
             * "mañana a la mañana" -> Mañana 09:00.
             * "mañana" -> Mañana 09:00.
             * Si no hay hora ni fecha indicada -> "trigger_type": "NONE", "trigger_iso": null, "trigger_display": "Sin fecha".
           - Cuando haya fecha/hora, define "trigger_type": "TIME", "trigger_iso" en ISO-8601 y "trigger_display" legible (ej. "Hoy 21:00", "Mañana 09:00").

        6. "Inferencia de Lugares, Prioridades y Listas":
           - "place_name": si menciona un lugar físico (ej. "Casa", "Al pasar por Súper", "Trabajo", "Farmacia").
           - "priority": "Alta" si indica urgencia o importancia, de lo contrario null o "Normal".
           - "list_name": si corresponde a una lista lógica (ej. "Súper", "Viaje", "Estudio", "Trabajo").

        7. Devuelve SIEMPRE y ÚNICAMENTE un JSON válido con el siguiente esquema exacto (sin texto adicional ni explicaciones):
        {
          "summary_badge": "Encontré 2 recordatorios distintos en tu audio. Revísalos antes de crearlos.",
          "is_checklist_candidate": false,
          "reminders": [
            {
              "title": "Hacer la tarea",
              "description": "Completar los ejercicios de matemática para la facultad",
              "type": "Note",
              "trigger_type": "TIME",
              "trigger_iso": "2026-10-17T21:00:00Z",
              "trigger_display": "Hoy 21:00",
              "place_name": null,
              "priority": "Normal",
              "list_name": "Estudio",
              "items": [],
              "separate": false
            },
            {
              "title": "Ir al súper",
              "description": "Comprar verduras y frutas para la cena",
              "type": "Note",
              "trigger_type": "NONE",
              "trigger_iso": null,
              "trigger_display": "Sin fecha",
              "place_name": "Al pasar por Súper",
              "priority": "Normal",
              "list_name": "Súper",
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

        // Caso 03.D: Checklist / Lista de compras, viaje o tareas
        val isShoppingOrList = lower.contains("comprar") || lower.startsWith("lista") ||
            lower.contains("para el viaje") || lower.contains("para las vacaciones") ||
            lower.contains("llevar") || lower.contains("traer") || lower.contains("necesito")

        // Extraer candidatos a ítems dividiendo por comas, puntos y comas, o conectores secuenciales
        val itemsSource = if (trimmed.contains(":")) {
            trimmed.substringAfter(":").trim()
        } else {
            trimmed.replace(Regex("""^(?:tengo\s+que\s+comprar|hay\s+que\s+comprar|comprar|necesito\s+comprar|necesito|llevar|traer|lista\s*(?:de\s*)?)\s*""", RegexOption.IGNORE_CASE), "").trim()
        }

        val itemCandidates = itemsSource.split(Regex("""[,;
]|\s+y\s+despu[ée]s\s+|\s+despu[ée]s\s+|\s+y\s+luego\s+|\s+luego\s+|\s+y\s+|\s+e\s+""", RegexOption.IGNORE_CASE))
            .map { it.trim().trim('.') }
            .filter { it.isNotBlank() && it.length > 1 }

        val hasChecklistIntent = trimmed.contains(":") || (isShoppingOrList && itemCandidates.size >= 2) || itemCandidates.size >= 3

        if (hasChecklistIntent) {
            val temporalTitle = when {
                lower.contains("a la noche") || lower.contains("esta noche") || lower.contains("de noche") -> "Qué hacer a la noche"
                lower.contains("a la tarde") || lower.contains("de la tarde") -> "Qué hacer a la tarde"
                lower.contains("al mediodía") || lower.contains("al medio día") -> "Qué hacer al mediodía"
                lower.contains("mañana a la noche") -> "Qué hacer mañana a la noche"
                lower.contains("mañana a la tarde") -> "Qué hacer mañana a la tarde"
                lower.contains("mañana a la mañana") -> "Qué hacer mañana a la mañana"
                lower.contains("mañana") -> "Qué hacer mañana"
                lower.contains("hoy") -> "Qué hacer hoy"
                else -> null
            }

            val title: String = if (trimmed.contains(":")) {
                trimmed.substringBefore(":").trim().replaceFirstChar { it.uppercase() }
            } else if (temporalTitle != null) {
                temporalTitle
            } else if (lower.contains("super") || lower.contains("compra") || lower.contains("supermercado")) {
                "Compras"
            } else if (lower.contains("viaje")) {
                "Para el viaje"
            } else if (lower.contains("vacaciones")) {
                "Vacaciones"
            } else {
                "Lista de tareas"
            }

            val checklistItems = mutableListOf<ChecklistItem>()
            var separateReminder: ProposedReminder? = null

            for (item in itemCandidates) {
                val itemLower = item.lowercase()
                val hasSpecificDate = itemLower.contains("el viernes") || itemLower.contains("el lunes") ||
                    itemLower.contains("mañana") || itemLower.contains("a las")
                if (hasSpecificDate && separateReminder == null) {
                    val (itemTitle, itemDesc) = extractTitleAndDescription(item)
                    separateReminder = ProposedReminder(
                        tempId = UUID.randomUUID().toString(),
                        title = itemTitle,
                        description = itemDesc,
                        type = ReminderType.Note,
                        triggerDisplayTime = inferDisplayTime(itemLower),
                        alarms = inferAlarms(itemLower, referenceIso),
                        isSeparateItem = true,
                        isSelected = true,
                    )
                } else {
                    checklistItems.add(ChecklistItem(text = cleanChecklistItem(item)))
                }
            }

            val timeDisplay = inferDisplayTime(lower)
            val alarms = inferAlarms(lower, referenceIso)

            val mainChecklist = ProposedReminder(
                tempId = UUID.randomUUID().toString(),
                title = title.ifBlank { "Lista de tareas" },
                description = trimmed,
                type = ReminderType.Checklist,
                items = checklistItems,
                alarms = alarms,
                triggerDisplayTime = if (timeDisplay != "Sin fecha") timeDisplay else null,
                placeName = if (lower.contains("super") || lower.contains("súper")) "Al pasar por Súper" else null,
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

        // Caso 03.B: Split automático por conectores secuenciales ("y después", "luego", "más tarde", "e ir", "y comprar", etc.)
        val segments = trimmed.split(TASK_SPLIT_REGEX).map { it.trim() }.filter { it.isNotBlank() }

        if (segments.size >= 2) {
            val proposals = segments.map { segment ->
                buildProposalFromText(segment, referenceIso)
            }
            return AiAnalysisResult(
                rawTranscript = trimmed,
                summaryBadge = "Encontré ${proposals.size} recordatorios distintos en tu audio. Revísalos antes de crearlos.",
                proposedReminders = proposals,
                isChecklistCandidate = false,
            )
        }

        // Caso individual general
        val singleProposal = buildProposalFromText(trimmed, referenceIso)
        return AiAnalysisResult(
            rawTranscript = trimmed,
            summaryBadge = "Detecté 1 recordatorio en tu audio.",
            proposedReminders = listOf(singleProposal),
            isChecklistCandidate = false,
        )
    }

    private fun buildProposalFromText(rawText: String, referenceIso: String): ProposedReminder {
        val lower = rawText.lowercase()
        val timeDisplay = inferDisplayTime(lower)
        val alarms = inferAlarms(lower, referenceIso)
        val place = inferPlace(lower)
        val list = inferList(lower)
        val priority = inferPriority(lower)
        val (cleanTitle, description) = extractTitleAndDescription(rawText)

        return ProposedReminder(
            tempId = UUID.randomUUID().toString(),
            title = cleanTitle.ifBlank { rawText.replaceFirstChar { it.uppercase() } },
            description = description,
            type = ReminderType.Note,
            alarms = alarms,
            triggerDisplayTime = timeDisplay,
            placeName = place,
            priorityName = priority,
            listName = list,
            isSelected = true,
        )
    }

    private fun cleanTaskTitle(text: String): String {
        var cleaned = text.trim().trimEnd('.', ',', ';')
        val prefixesToRemove = listOf(
            Regex("""^(?:a\s+la\s+noche|esta\s+noche|a\s+la\s+tarde|a\s+la\s+mañana|mañana|hoy)\s+""", RegexOption.IGNORE_CASE),
            Regex("""^(?:tengo\s+que|acordarme\s+de|recordar\s+que|recordar|hay\s+que|debo|necesito)\s+""", RegexOption.IGNORE_CASE),
            Regex("""^(?:y\s+después|después|y\s+luego|luego|y\s+más\s+tarde|más\s+tarde|y\s+también|y\s+además|y|e)\s+""", RegexOption.IGNORE_CASE),
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

    private fun isTranscriptBlankOrNoise(text: String): Boolean {
        if (text.isBlank()) return true
        val trimmed = text.trim()
        val lettersCount = trimmed.count { it.isLetter() }
        if (lettersCount < 2) return true

        val noisePatterns = listOf(
            Regex("""^\[.*\]$"""),
            Regex("""^\(.*\)$"""),
            Regex("""^\*.*\*$"""),
            Regex("""^[\p{Punct}\s]+$"""),
            Regex("""(?i)^\[?(?:m[uú]sica|ruido|sonido|silencio|ininteligible|aplausos|risas|tos|suspiro|campana|pitido|bip|est[aá]tica)\]?\.?$"""),
            Regex("""(?i).*(?:subt[íi]tulos\s+(?:por|realizados)|amara\.org|gracias\s+por\s+ver|suscr[íi]bete|like\s+y\s+suscr[íi]bete).*"""),
        )
        if (noisePatterns.any { it.matches(trimmed) }) return true

        val fillerWords = setOf("eh", "ah", "uh", "um", "oh", "mm", "mmm", "shh", "ay")
        val words = trimmed.lowercase().split(Regex("""\W+""")).filter { it.isNotBlank() }
        if (words.isNotEmpty() && words.all { it in fillerWords }) return true

        return false
    }

    private fun cleanChecklistItem(item: String): String {
        var text = item.trim().trimEnd('.', ',', ';')

        // 1. Quitar marcadores temporales iniciales
        val timeRegex = Regex(
            """^(?:a\s+la\s+noche|esta\s+noche|de\s+noche|a\s+la\s+tarde|de\s+la\s+tarde|al\s+mediod[ií]a|a\s+la\s+mañana|mañana\s+a\s+la\s+noche|mañana\s+a\s+la\s+tarde|mañana\s+a\s+la\s+mañana|mañana|hoy)\s+""",
            RegexOption.IGNORE_CASE,
        )
        text = text.replace(timeRegex, "").trim()

        // 2. Quitar conectores iniciales
        val connectorRegex = Regex(
            """^(?:y\s+despu[ée]s|despu[ée]s|y\s+luego|luego|y\s+m[áa]s\s+tarde|m[áa]s\s+tarde|y\s+tambi[ée]n|y\s+adem[áa]s|y|e)\s+""",
            RegexOption.IGNORE_CASE,
        )
        text = text.replace(connectorRegex, "").trim()

        // 3. Quitar fórmulas de obligación verbales
        val formulaRegex = Regex(
            """^(?:tengo\s+que|hay\s+que|debo|necesito|acordarme\s+de|recordar\s+que|recordar)\s+""",
            RegexOption.IGNORE_CASE,
        )
        text = text.replace(formulaRegex, "").trim()

        // 4. Quitar envoltura de desplazamiento para compras ("ir al super a/para comprar", "pasar por el super a comprar", etc.)
        val shoppingTripRegex = Regex(
            """^(?:(?:ir|pasar)\s+(?:al|a\s+la|por\s+el|por\s+la)\s+(?:s[uú]per|supermercado|almac[ée]n|kiosco|verduler[íi]a|panader[íi]a|farmacia|chino|tienda)\s+(?:a|para)\s+(?:comprar|buscar|traer|conseguir)\s+|(?:a|para)\s+comprar\s+)""",
            RegexOption.IGNORE_CASE,
        )
        text = text.replace(shoppingTripRegex, "").trim()

        if (text.isBlank()) return item.trim().replaceFirstChar { it.uppercase() }
        return text.replaceFirstChar { it.uppercase() }
    }

    private fun inferAlarms(lower: String, referenceIso: String): List<Alarm> {
        val iso = inferIsoTime(lower, referenceIso) ?: return emptyList()
        return parseAlarms("TIME", iso)
    }

    private fun inferIsoTime(lower: String, referenceIso: String): String? {
        val base = try {
            OffsetDateTime.parse(referenceIso)
        } catch (_: Exception) {
            try {
                Instant.parse(referenceIso).atZone(ZoneId.systemDefault()).toOffsetDateTime()
            } catch (_: Exception) {
                OffsetDateTime.now()
            }
        }

        val target = when {
            lower.contains("mañana a la noche") -> base.plusDays(1).withHour(21).withMinute(0).withSecond(0).withNano(0)
            lower.contains("mañana a la tarde") -> base.plusDays(1).withHour(16).withMinute(0).withSecond(0).withNano(0)
            lower.contains("mañana a la mañana") || lower.contains("mañana") -> base.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0)
            lower.contains("a la noche") || lower.contains("esta noche") || lower.contains("de noche") -> base.withHour(21).withMinute(0).withSecond(0).withNano(0)
            lower.contains("a la tarde") || lower.contains("de la tarde") -> base.withHour(17).withMinute(0).withSecond(0).withNano(0)
            lower.contains("al mediodía") || lower.contains("al medio día") -> base.withHour(13).withMinute(0).withSecond(0).withNano(0)
            lower.contains("hoy") -> base.withHour(20).withMinute(0).withSecond(0).withNano(0)
            else -> null
        }
        return target?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    companion object {
        private const val ACTION_VERBS =
            """(?:tengo\s+que|hay\s+que|debo|necesito|acordarme\s+de|recordar\s+que|recordar|""" +
            """ir\s+a|ir|vamos|voy|hacer|haz|hago|comprar|compra|compro|llamar|llama|llamo|""" +
            """pagar|paga|pago|pasar\s+por|pasar|pasa|paso|buscar|busca|busco|sacar|saca|saco|""" +
            """llevar|lleva|llevo|traer|trae|traigo|avisar|avisa|aviso|mandar|manda|mando|""" +
            """enviar|envía|envio|pedir|pide|pido|limpiar|limpia|limpio|cocinar|cocina|cocino|""" +
            """estudiar|estudia|estudio|ordenar|ordena|ordeno|terminar|termina|termino|""" +
            """empezar|comenzar|arreglar|reparar|entregar|visitar|cenar|almorzar|desayunar|""" +
            """entrenar|correr|cargar|recargar)"""

        private const val TIME_MARKERS =
            """(?:a\s+la\s+noche|esta\s+noche|de\s+noche|a\s+la\s+tarde|de\s+la\s+tarde|""" +
            """a\s+la\s+mañana|mañana|hoy|al\s+mediodía|a\s+las\s+\d+)"""

        val TASK_SPLIT_REGEX = Regex(
            """\s+(?:y\s+después|después|y\s+luego|luego|y\s+más\s+tarde|más\s+tarde|y\s+además|y\s+también|""" +
            """y\s+a\s+la\s+noche|y\s+a\s+la\s+tarde|y\s+a\s+la\s+mañana|y\s+a\s+las\s+\d+|y\s+mañana|""" +
            """(?:y|e)\s+(?=(?:$ACTION_VERBS|$TIME_MARKERS)\b))\s*""",
            RegexOption.IGNORE_CASE,
        )
    }
}

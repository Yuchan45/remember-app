package com.example.uade.rememberapp.data.remote

import com.example.uade.rememberapp.data.remote.dto.GroqChatRequest
import com.example.uade.rememberapp.data.remote.dto.GroqMessage
import com.example.uade.rememberapp.data.remote.dto.GroqParsedReminderDto
import com.example.uade.rememberapp.data.remote.dto.GroqReminderAnalysisDto
import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.model.AiProcessingError
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.repository.AiReminderService
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

/**
 * Implementación de [AiReminderService] que se comunica con la API de Groq
 * utilizando Retrofit (Whisper para voz y gpt-oss-120b para extracción estructurada).
 *
 * Incluye un fallback heurístico inteligente si la API key aún no fue configurada
 * o ante fallos de red en modo demo, garantizando el flujo visual de los mockups 03.B y 03.D.
 */
private const val TRANSCRIPTION_PROMPT =
    "Recordatorio en español: recordame, mañana, pasado mañana, hoy, a la mañana, a la tarde, " +
        "a la noche, a las ocho, lunes, martes, comprar, llamar, pagar, turno, médico."

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
            logError("GroqAI", "Falta groq.api.key en local.properties: no se puede transcribir", null)
            return@withContext Result.failure(AiProcessingError.MissingApiKey())
        }

        if (file.length() < 1200L) {
            return@withContext Result.failure(AiProcessingError.Unintelligible())
        }

        try {
            val authHeader = "Bearer $apiKey"
            // audio/mp4: es lo que graba AudioRecorder (AAC en .m4a). Con "audio/*" Groq tiene que
            // adivinar el formato por la extensión.
            val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val text = "text/plain".toMediaTypeOrNull()
            val response = apiService.transcribeAudio(
                authHeader = authHeader,
                file = body,
                model = "whisper-large-v3".toRequestBody(text),
                language = "es".toRequestBody(text),
                // temperature 0: la transcripción más probable, sin "creatividad" (menos frases
                // inventadas en los silencios).
                temperature = "0".toRequestBody(text),
                // El prompt le da contexto a Whisper: palabras que suelen aparecer en un
                // recordatorio, así reconoce mejor horas, días y verbos cortos.
                prompt = TRANSCRIPTION_PROMPT.toRequestBody(text),
            )
            val transcript = response.text.trim()
            logDebug("GroqAI", "Whisper transcript: '$transcript'")
            if (transcript.isBlank() || isTranscriptBlankOrNoise(transcript)) {
                Result.failure(AiProcessingError.Unintelligible())
            } else {
                Result.success(transcript)
            }
        } catch (e: HttpException) {
            logError("GroqAI", "Whisper respondió HTTP ${e.code()}", e)
            Result.failure(
                when (e.code()) {
                    401, 403 -> AiProcessingError.InvalidApiKey(e)
                    // 400/422: Groq no pudo leer el audio (vacío, cortado o formato raro).
                    400, 413, 422 -> AiProcessingError.Unintelligible()
                    else -> AiProcessingError.Network(e)
                },
            )
        } catch (e: IOException) {
            Result.failure(AiProcessingError.Network(e))
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
                    // llama-3.3-70b-versatile se dio de baja en Groq: con ese nombre la API falla y la app caía
                    // siempre al análisis local (más básico). gpt-oss-120b es el modelo de chat más capaz hoy.
                    model = "openai/gpt-oss-120b",
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

        // Se usa tal cual lo que decidió el modelo. Antes, si devolvía un solo recordatorio, acá se
        // volvía a cortar el título por "y" / "e" / "después" (TASK_SPLIT_REGEX): eso partía en dos
        // frases como "ir al súper y comprar carne", que el prompt ya junta a propósito. El corte por
        // conectores sigue solo en el análisis local (createLocalHeuristicResult), sin modelo.
        val adjustedReminders = rawReminders

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

    /**
     * El prompt de sistema para estructurar una nota de voz en recordatorios.
     *
     * Está armado con las prácticas habituales para modelos de lenguaje:
     * - Rol y tarea al principio, en una línea cada uno.
     * - El contexto que el modelo no puede saber solo: la fecha y hora de ahora, con el día de la
     *   semana y la zona horaria (si no, "el lunes" o "a las 9" se resuelven mal).
     * - Un criterio de decisión explícito para lo más difícil (¿uno o varios recordatorios?), con
     *   el porqué, en vez de reglas sueltas por palabra ("separá si hay una y"), que fallaban con
     *   frases como "ir al súper y comprar carne".
     * - Qué va en cada campo, con su formato exacto.
     * - Ejemplos de entrada → salida que cubren los casos dudosos, incluidos los que NO hay que
     *   separar. Los ejemplos usan su propia fecha de referencia, para que el modelo no copie sus
     *   horas.
     * - La salida se pide solo como JSON (además, el pedido usa response_format = json_object).
     */
    private fun buildSystemPrompt(referenceIso: String): String {
        val reference = describeReference(referenceIso)
        return """
        Sos el asistente de "Hey!", una app de recordatorios. Recibís la transcripción de una nota de voz en español rioplatense y la convertís en recordatorios.

        # Contexto
        Ahora es $reference ($referenceIso). Usalo para resolver fechas y horas relativas.

        # Paso 1: ¿cuántos recordatorios?
        Pensá en salidas o momentos, no en verbos.
        - Un recordatorio es algo que el usuario tiene que acordarse de hacer en un momento o en un lugar.
        - Lo que se hace como parte de esa misma salida o momento va DENTRO del mismo recordatorio, no aparte:
          - cosas (productos u objetos) para comprar, llevar o conseguir → en "items" (checklist), aunque sea una sola;
          - trámites o qué hay que hacer allá ("depositar el cheque", "sacar turno", "pagar"), detalles o motivos → en "description", NO en "items".
          Ej.: "ir al supermercado y comprar carne" es UNA salida al supermercado con un ítem (Carne).
        - Separá en varios recordatorios solo si las acciones son independientes: momentos distintos ("a las 9… y a la tarde…"), lugares sin relación, o cosas que no se hacen juntas ("hacer la tarea e ir al súper").
        - Nunca crees un recordatorio por cada producto de una lista.
        - Ante la duda, UN solo recordatorio: el usuario puede dividirlo después, pero juntar es más difícil.

        # Paso 2: cada campo
        - "title": la acción principal, de 2 a 5 palabras, en infinitivo y con mayúscula inicial ("Ir al supermercado", "Llamar al médico", "Sacar turno con el dentista"). Sin fechas ni horas, y sin "tengo que", "acordarme de", "recordame", "hay que".
          - Si es solo una compra sin lugar, el título es la compra: "Comprar leche" (sin items); con dos o más productos, "Compras" con los productos en "items".
        - "description": lo que se hace allá y los detalles o motivos que dijo el usuario, en una frase corta ("Depositar el cheque", "Pedir los análisis del lunes"). Si hay "items", es solo el verbo ("Comprar", "Llevar") sin repetir los ítems. "" si no dijo nada más.
        - "items": cada cosa por separado, con mayúscula inicial, tal como la dijo (con cantidad si la dijo: "2 kg de papas"). [] si no hay.
        - "type": "Checklist" si "items" tiene algo; si no, "Note".
        - "trigger_type": "TIME" si dijo un día o una hora; si no, "NONE".
        - "trigger_iso": fecha y hora en ISO-8601 con el MISMO offset que la hora de referencia (ej. 2026-11-07T21:00:00-03:00). Nunca uses "Z". null si "trigger_type" es "NONE".
          - Hora sin día: la próxima vez que llegue esa hora. Si son las 15:30, "a las 9" es hoy a las 21:00 y "a las 16" es hoy a las 16:00; si son las 08:00, "a las 9" es hoy a las 09:00.
          - Momentos sin hora: "a la mañana" 09:00, "al mediodía" 13:00, "a la tarde" 17:00, "a la noche"/"esta noche" 21:00.
          - Día sin hora ("mañana", "el lunes", "el 15"): ese día a las 09:00. Un día de la semana es el próximo que viene (si hoy es lunes, "el lunes" es el de la semana que viene).
          - Una fecha u hora dicha una vez vale para todas las acciones de la frase que la siguen ("y después", "y", "también"), salvo que se diga otra.
        - "trigger_display": cómo se lee la fecha: "Hoy 21:00" si es hoy, "Mañana 09:00" si es mañana, y si no "Lun 9/11 09:00"; "Sin fecha" si no tiene.
        - "place_name": el lugar al que va o donde tiene que estar ("Supermercado", "Farmacia", "Trabajo"); null si no nombró ninguno.
        - "priority": "Alta" solo si dijo que es urgente, importante o "sin falta"; si no, "Normal".
        - "list_name": una categoría corta si es obvia ("Compras", "Salud", "Trabajo", "Estudio", "Casa"); si no, null.
        - "separate": siempre false.
        - "is_checklist_candidate": true si algún recordatorio tiene "items".
        - "summary_badge": una frase corta para el usuario, ej. "Encontré 1 recordatorio con 1 ítem." o "Encontré 2 recordatorios."

        # Reglas
        - Usá solo lo que dijo el usuario: no inventes tareas, horas, lugares ni productos.
        - Si la transcripción tiene un error obvio y el sentido es claro, corregilo ("super mercado" → "supermercado").
        - Respondé solo con el JSON, sin texto antes ni después.

        # Formato de salida
        {"summary_badge": string, "is_checklist_candidate": boolean, "reminders": [{"title": string, "description": string, "type": "Note" | "Checklist", "trigger_type": "TIME" | "NONE", "trigger_iso": string | null, "trigger_display": string, "place_name": string | null, "priority": "Normal" | "Alta", "list_name": string | null, "items": [string], "separate": false}]}

        # Ejemplos
        En los ejemplos, "ahora" es el sábado 7 de noviembre de 2026 a las 15:30 (-03:00). Son solo para mostrar el criterio: con el usuario real usá la hora del Contexto.

        Entrada: "a las 9 ir al supermercado y comprar carne"
        Salida: {"summary_badge": "Encontré 1 recordatorio con 1 ítem.", "is_checklist_candidate": true, "reminders": [{"title": "Ir al supermercado", "description": "Comprar", "type": "Checklist", "trigger_type": "TIME", "trigger_iso": "2026-11-07T21:00:00-03:00", "trigger_display": "Hoy 21:00", "place_name": "Supermercado", "priority": "Normal", "list_name": "Compras", "items": ["Carne"], "separate": false}]}

        Entrada: "tengo que pasar por la farmacia a comprar ibuprofeno, curitas y alcohol"
        Salida: {"summary_badge": "Encontré 1 recordatorio con 3 ítems.", "is_checklist_candidate": true, "reminders": [{"title": "Ir a la farmacia", "description": "Comprar", "type": "Checklist", "trigger_type": "NONE", "trigger_iso": null, "trigger_display": "Sin fecha", "place_name": "Farmacia", "priority": "Normal", "list_name": "Salud", "items": ["Ibuprofeno", "Curitas", "Alcohol"], "separate": false}]}

        Entrada: "a las 9 llamar al médico y a la tarde pagar la luz"
        Salida: {"summary_badge": "Encontré 2 recordatorios.", "is_checklist_candidate": false, "reminders": [{"title": "Llamar al médico", "description": "", "type": "Note", "trigger_type": "TIME", "trigger_iso": "2026-11-07T21:00:00-03:00", "trigger_display": "Hoy 21:00", "place_name": null, "priority": "Normal", "list_name": "Salud", "items": [], "separate": false}, {"title": "Pagar la luz", "description": "", "type": "Note", "trigger_type": "TIME", "trigger_iso": "2026-11-07T17:00:00-03:00", "trigger_display": "Hoy 17:00", "place_name": null, "priority": "Normal", "list_name": "Casa", "items": [], "separate": false}]}

        Entrada: "recordame el lunes sacar turno con el dentista, es urgente"
        Salida: {"summary_badge": "Encontré 1 recordatorio.", "is_checklist_candidate": false, "reminders": [{"title": "Sacar turno con el dentista", "description": "", "type": "Note", "trigger_type": "TIME", "trigger_iso": "2026-11-09T09:00:00-03:00", "trigger_display": "Lun 9/11 09:00", "place_name": null, "priority": "Alta", "list_name": "Salud", "items": [], "separate": false}]}

        Entrada: "comprar leche, pan y huevos"
        Salida: {"summary_badge": "Encontré 1 recordatorio con 3 ítems.", "is_checklist_candidate": true, "reminders": [{"title": "Compras", "description": "", "type": "Checklist", "trigger_type": "NONE", "trigger_iso": null, "trigger_display": "Sin fecha", "place_name": null, "priority": "Normal", "list_name": "Compras", "items": ["Leche", "Pan", "Huevos"], "separate": false}]}
        """.trimIndent()
    }

    /**
     * La hora de referencia como la lee una persona, ej. "sábado 7 de noviembre de 2026, 15:30
     * (UTC-03:00)". El modelo calcula mejor "el lunes" o "pasado mañana" sabiendo qué día es hoy;
     * con solo el ISO tendría que deducir el día de la semana, y ahí se equivoca.
     */
    private fun describeReference(referenceIso: String): String = try {
        val reference = OffsetDateTime.parse(referenceIso)
        val formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-AR"))
        "${reference.format(formatter)} (UTC${reference.offset.id.replace("Z", "+00:00")})"
    } catch (e: DateTimeParseException) {
        referenceIso
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

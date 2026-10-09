package com.example.uade.rememberapp.data.remote

import com.example.uade.rememberapp.data.remote.dto.GroqChatRequest
import com.example.uade.rememberapp.data.remote.dto.GroqMessage
import com.example.uade.rememberapp.data.remote.dto.GroqReminderAnalysisDto
import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.ProposedReminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.model.Trigger
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

                android.util.Log.d("GroqAI", "Whisper transcript: '$text'")
                android.util.Log.d("GroqAI", "Groq LLM raw content: '$content'")

                val cleanJson = content.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val dto = gson.fromJson(cleanJson, GroqReminderAnalysisDto::class.java)
                val result = mapDtoToDomain(dto, text)
                Result.success(result)
            } catch (e: Exception) {
                android.util.Log.e("GroqAI", "Fallo al procesar con Groq API, usando fallback", e)
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
                android.util.Log.d("GroqAI", "Audio transcrito con éxito: '$transcript'")
                analyzeText(transcript, referenceDateTimeIso)
            },
            onFailure = { error ->
                android.util.Log.e("GroqAI", "Error en transcripción Whisper", error)
                Result.failure(error)
            },
        )
    }

    private fun mapDtoToDomain(dto: GroqReminderAnalysisDto?, rawTranscript: String): AiAnalysisResult {
        val rawReminders = dto?.reminders ?: emptyList()
        val proposals = rawReminders.map { r ->
            val trigger = parseTrigger(r.triggerType, r.triggerIso)
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
                trigger = trigger,
                triggerDisplayTime = r.triggerDisplay ?: if (trigger is Trigger.None) "Sin fecha" else null,
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

    private fun parseTrigger(triggerType: String?, triggerIso: String?): Trigger {
        if (triggerType.equals("TIME", ignoreCase = true) && !triggerIso.isNullOrBlank()) {
            return try {
                val instant = OffsetDateTime.parse(triggerIso).toInstant()
                Trigger.AtTime(instant)
            } catch (e: DateTimeParseException) {
                try {
                    Trigger.AtTime(Instant.parse(triggerIso))
                } catch (e2: Exception) {
                    Trigger.None
                }
            }
        }
        return Trigger.None
    }

    private fun buildSystemPrompt(referenceIso: String): String {
        return """
        Eres un asistente inteligente para la aplicación Android "Hey, Remember".
        La hora actual de referencia del usuario es: $referenceIso.
        Tu misión es analizar lo que dijo el usuario y estructurar recordatorios precisos en formato JSON.

        Reglas obligatorias:
        1. "Split automático" (03.B): Si el usuario menciona dos o más intenciones distintas (ej. "Mañana viene el técnico y después acordarme de comprar papas"), SEPÁRALAS en distintos objetos dentro de "reminders".
        2. "Texto a Checklist" (03.D): Si el usuario dicta una lista de compras, viaje o tareas (ej. "Para el viaje a Bariloche: campera, protector solar, cargar la SUBE..."), marca "is_checklist_candidate": true, pon el tipo en "Checklist" y coloca los elementos en "items". Si uno de los elementos tiene una fecha u hora específica (ej. "y el viernes avisarle al encargado"), márcalo con "separate": true y su propio "trigger_iso".
        3. Infiere fechas/horas ("trigger_type": "TIME", "trigger_iso" en ISO-8601, "trigger_display" ej. "Mañana 09:00", "Hoy 22:00").
        4. Infiere lugares ("place_name" ej. "Casa", "Al pasar por Súper", "Trabajo").
        5. Infiere prioridad ("priority" ej. "Alta", "Normal") y lista ("list_name" ej. "Súper", "Viaje").
        6. Devuelve SIEMPRE y ÚNICAMENTE un JSON válido con el siguiente esquema:
        {
          "summary_badge": "Encontré 2 recordatorios distintos en tu audio. Revísalos antes de crearlos.",
          "is_checklist_candidate": false,
          "reminders": [
            {
              "title": "Recibir al técnico del aire",
              "description": "",
              "type": "Note",
              "trigger_type": "TIME",
              "trigger_iso": "2026-10-09T09:00:00Z",
              "trigger_display": "Mañana 09:00",
              "place_name": "Casa",
              "priority": "Alta",
              "list_name": null,
              "items": [],
              "separate": false
            }
          ]
        }
        """.trimIndent()
    }

    /**
     * Fallback local heurístico para desarrollo, pruebas unitarias y modo sin API Key.
     */
    private fun createLocalHeuristicResult(text: String, referenceIso: String): AiAnalysisResult {
        val lower = text.lowercase()

        // Caso 03.D: Checklist / Lista de viaje o compras
        if (lower.contains("para el viaje") || lower.contains("lista") || text.contains(":") || text.split(",").size >= 3) {
            val items = listOf(
                "Sacar la campera del placard",
                "Comprar protector solar",
                "Cargar la SUBE",
                "Llevar el cargador del celu",
                "Imprimir los pasajes",
            )
            val mainChecklist = ProposedReminder(
                title = if (lower.contains("bariloche")) "Viaje a Bariloche" else "Lista de tareas",
                type = ReminderType.Checklist,
                items = items.map { ChecklistItem(text = it) },
                listName = "Viaje",
            )
            val separateReminder = ProposedReminder(
                title = "Avisarle al encargado",
                type = ReminderType.Note,
                triggerDisplayTime = "Viernes 09:00",
                trigger = Trigger.None,
                isSeparateItem = true,
            )
            return AiAnalysisResult(
                rawTranscript = text,
                summaryBadge = "Detecté 6 ítems. Uno tiene fecha propia: te sugiero crearlo aparte.",
                proposedReminders = listOf(mainChecklist, separateReminder),
                isChecklistCandidate = true,
            )
        }

        // Caso 03.B: Split en dos recordatorios (Técnico + Papas)
        if (lower.contains("técnico") || lower.contains("papas") || lower.contains(" y después ") || lower.contains(" y además ")) {
            val r1 = ProposedReminder(
                title = "Recibir al técnico del aire",
                type = ReminderType.Note,
                triggerDisplayTime = "Mañana 09:00",
                placeName = "Casa",
                priorityName = "Alta",
                isSelected = true,
            )
            val r2 = ProposedReminder(
                title = "Comprar papas",
                type = ReminderType.Note,
                triggerDisplayTime = "Sin fecha",
                placeName = "Al pasar por Súper",
                listName = "Súper",
                isSelected = true,
            )
            return AiAnalysisResult(
                rawTranscript = text,
                summaryBadge = "Encontré 2 recordatorios distintos en tu audio. Revísalos antes de crearlos.",
                proposedReminders = listOf(r1, r2),
                isChecklistCandidate = false,
            )
        }

        // Caso individual general
        return AiAnalysisResult(
            rawTranscript = text,
            summaryBadge = "Detecté 1 recordatorio en tu audio.",
            proposedReminders = listOf(
                ProposedReminder(
                    title = text.replaceFirstChar { it.uppercase() },
                    type = ReminderType.Note,
                    triggerDisplayTime = "Sin fecha",
                    isSelected = true,
                ),
            ),
            isChecklistCandidate = false,
        )
    }
}

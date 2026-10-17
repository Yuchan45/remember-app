package com.example.uade.rememberapp.data.remote

import com.example.uade.rememberapp.data.remote.dto.GroqChatRequest
import com.example.uade.rememberapp.data.remote.dto.GroqChatResponse
import com.example.uade.rememberapp.data.remote.dto.GroqChoice
import com.example.uade.rememberapp.data.remote.dto.GroqMessage
import com.example.uade.rememberapp.data.remote.dto.GroqTranscriptionResponse
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroqReminderServiceImplTest {

    private val fakeApiService = object : GroqApiService {
        override suspend fun transcribeAudio(
            authHeader: String,
            file: MultipartBody.Part,
            model: RequestBody,
            language: RequestBody?,
        ): GroqTranscriptionResponse = GroqTranscriptionResponse("fake")

        override suspend fun createChatCompletion(
            authHeader: String,
            request: GroqChatRequest,
        ): GroqChatResponse = GroqChatResponse()
    }

    @Test
    fun `analyzeText en modo heuristico divide dinamicamente tarea y super`() = runBlocking {
        val service = GroqReminderServiceImpl(fakeApiService, apiKey = "", gson = Gson())

        val text = "A la noche tengo que hacer la tarea y después ir al super"
        val result = service.analyzeText(text, "2026-10-17T15:00:00Z")

        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        assertEquals(2, analysis.proposedReminders.size)
        assertEquals("Hacer la tarea", analysis.proposedReminders[0].title)
        assertEquals("Hoy 21:00", analysis.proposedReminders[0].triggerDisplayTime)
        assertEquals("Ir al super", analysis.proposedReminders[1].title)
        assertEquals("Al pasar por Súper", analysis.proposedReminders[1].placeName)
    }

    @Test
    fun `analyzeText en modo heuristico detecta checklist dinamica de viaje`() = runBlocking {
        val service = GroqReminderServiceImpl(fakeApiService, apiKey = "", gson = Gson())

        val text = "Para el viaje a Bariloche: sacar la campera del placard, comprar protector solar, cargar la SUBE"
        val result = service.analyzeText(text, "2026-10-17T15:00:00Z")

        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        assertTrue(analysis.isChecklistCandidate)
        assertTrue(analysis.proposedReminders.isNotEmpty())
        val checklist = analysis.proposedReminders[0]
        assertEquals(3, checklist.items.size)
        assertEquals("Sacar la campera del placard", checklist.items[0].text)
        assertEquals("Comprar protector solar", checklist.items[1].text)
        assertEquals("Cargar la SUBE", checklist.items[2].text)
    }

    @Test
    fun `analyzeText con respuesta de Groq LLM extrae JSON correctamente incluso con texto envolvente`() = runBlocking {
        val jsonPayload = """
        ```json
        {
          "summary_badge": "Encontré 2 recordatorios distintos en tu audio. Revísalos antes de crearlos.",
          "is_checklist_candidate": false,
          "reminders": [
            {
              "title": "Hacer la tarea",
              "type": "Note",
              "trigger_type": "TIME",
              "trigger_iso": "2026-10-17T21:00:00Z",
              "trigger_display": "Hoy 21:00",
              "priority": "Normal",
              "list_name": "Estudio"
            },
            {
              "title": "Ir al súper",
              "type": "Note",
              "trigger_type": "NONE",
              "trigger_display": "Sin fecha",
              "place_name": "Al pasar por Súper",
              "list_name": "Súper"
            }
          ]
        }
        ```
        """.trimIndent()

        val mockApi = object : GroqApiService {
            override suspend fun transcribeAudio(
                authHeader: String,
                file: MultipartBody.Part,
                model: RequestBody,
                language: RequestBody?,
            ): GroqTranscriptionResponse = GroqTranscriptionResponse("fake")

            override suspend fun createChatCompletion(
                authHeader: String,
                request: GroqChatRequest,
            ): GroqChatResponse = GroqChatResponse(
                choices = listOf(GroqChoice(GroqMessage(role = "assistant", content = jsonPayload))),
            )
        }

        val service = GroqReminderServiceImpl(mockApi, apiKey = "gsk_test", gson = Gson())
        val result = service.analyzeText("A la noche tengo que hacer la tarea y después ir al super", "2026-10-17T15:00:00Z")

        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        assertEquals(2, analysis.proposedReminders.size)
        assertEquals("Hacer la tarea", analysis.proposedReminders[0].title)
        assertEquals("Hoy 21:00", analysis.proposedReminders[0].triggerDisplayTime)
        assertEquals("Ir al súper", analysis.proposedReminders[1].title)
        assertEquals("Al pasar por Súper", analysis.proposedReminders[1].placeName)
    }
}

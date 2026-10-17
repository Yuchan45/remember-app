package com.example.uade.rememberapp.data.remote

import com.example.uade.rememberapp.data.remote.dto.GroqChatRequest
import com.example.uade.rememberapp.data.remote.dto.GroqChatResponse
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
    fun `analyzeText en modo fallback heuristico detecta split de 03B`() = runBlocking {
        val service = GroqReminderServiceImpl(fakeApiService, apiKey = "", gson = Gson())

        val text = "Mañana a las 9 viene el técnico del aire y después tengo que acordarme de comprar papas en el super"
        val result = service.analyzeText(text, "2026-10-08T20:00:00Z")

        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        assertEquals(2, analysis.proposedReminders.size)
        assertEquals("Recibir al técnico del aire", analysis.proposedReminders[0].title)
        assertEquals("Comprar papas", analysis.proposedReminders[1].title)
        assertEquals("Casa", analysis.proposedReminders[0].placeName)
        assertEquals("Alta", analysis.proposedReminders[0].priorityName)
    }

    @Test
    fun `analyzeText en modo fallback heuristico detecta checklist de 03D`() = runBlocking {
        val service = GroqReminderServiceImpl(fakeApiService, apiKey = "", gson = Gson())

        val text = "Para el viaje a Bariloche: sacar la campera del placard, comprar protector solar, cargar la SUBE"
        val result = service.analyzeText(text, "2026-10-08T20:00:00Z")

        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        assertTrue(analysis.isChecklistCandidate)
        assertTrue(analysis.proposedReminders.isNotEmpty())
        assertTrue(analysis.proposedReminders.any { it.isSeparateItem })
    }
}

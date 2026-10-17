package com.example.uade.rememberapp.data.remote

import com.example.uade.rememberapp.data.remote.dto.GroqChatRequest
import com.example.uade.rememberapp.data.remote.dto.GroqChatResponse
import com.example.uade.rememberapp.data.remote.dto.GroqTranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Cliente Retrofit para interactuar con la API oficial de Groq:
 * 1. Whisper API (audio/transcriptions) para convertir voz a texto.
 * 2. Llama 3.3 (chat/completions) para extraer y estructurar recordatorios en JSON.
 */
interface GroqApiService {

    @Multipart
    @POST("openai/v1/audio/transcriptions")
    suspend fun transcribeAudio(
        @Header("Authorization") authHeader: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("language") language: RequestBody? = null,
    ): GroqTranscriptionResponse

    @POST("openai/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: GroqChatRequest,
    ): GroqChatResponse
}

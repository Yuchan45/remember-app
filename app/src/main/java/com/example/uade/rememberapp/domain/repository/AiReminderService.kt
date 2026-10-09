package com.example.uade.rememberapp.domain.repository

import com.example.uade.rememberapp.domain.model.AiAnalysisResult

/**
 * Contrato del servicio de IA para procesar voz y texto estructurado con Groq.
 * Pertenece a la capa de dominio: no tiene dependencias de Android.
 */
interface AiReminderService {

    /**
     * Envía el archivo de audio a la API Whisper de Groq y devuelve la transcripción textual.
     */
    suspend fun transcribeAudio(audioFilePath: String): Result<String>

    /**
     * Envía un texto (o transcripción) al LLM de Groq para extraer y estructurar recordatorios.
     */
    suspend fun analyzeText(text: String, referenceDateTimeIso: String): Result<AiAnalysisResult>

    /**
     * Pipeline completo: transcribe el audio y luego analiza el texto obtenido.
     */
    suspend fun processAudio(audioFilePath: String, referenceDateTimeIso: String): Result<AiAnalysisResult>
}

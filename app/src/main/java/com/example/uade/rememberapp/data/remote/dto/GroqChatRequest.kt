package com.example.uade.rememberapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroqChatRequest(
    @SerializedName("model")
    val model: String = "llama-3.3-70b-versatile",
    @SerializedName("messages")
    val messages: List<GroqMessage>,
    @SerializedName("response_format")
    val responseFormat: GroqResponseFormat? = GroqResponseFormat(type = "json_object"),
    @SerializedName("temperature")
    val temperature: Double = 0.1,
)

data class GroqMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String,
)

data class GroqResponseFormat(
    @SerializedName("type")
    val type: String = "json_object",
)

package com.example.uade.rememberapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroqChatResponse(
    @SerializedName("choices")
    val choices: List<GroqChoice> = emptyList(),
)

data class GroqChoice(
    @SerializedName("message")
    val message: GroqMessage,
)

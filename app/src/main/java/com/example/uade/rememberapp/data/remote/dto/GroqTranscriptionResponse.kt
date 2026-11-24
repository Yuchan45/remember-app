package com.example.uade.rememberapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroqTranscriptionResponse(
    @SerializedName("text")
    val text: String = "",
)

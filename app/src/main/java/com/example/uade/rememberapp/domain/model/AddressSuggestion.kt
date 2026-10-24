package com.example.uade.rememberapp.domain.model

/**
 * Resultado devuelto por la búsqueda de direcciones o la ubicación actual.
 */
data class AddressSuggestion(
    val addressLine: String,
    val latitude: Double,
    val longitude: Double,
    val featureName: String? = null,
)

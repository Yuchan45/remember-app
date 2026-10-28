package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.AddressSuggestion
import com.example.uade.rememberapp.domain.repository.AddressSearchService

/**
 * Obtiene las coordenadas y dirección de la ubicación actual del dispositivo.
 */
class GetCurrentLocationUseCase(
    private val addressSearchService: AddressSearchService,
) {
    suspend operator fun invoke(): AddressSuggestion? =
        addressSearchService.getCurrentLocation()
}

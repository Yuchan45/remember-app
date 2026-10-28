package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.AddressSuggestion
import com.example.uade.rememberapp.domain.repository.AddressSearchService

/**
 * Busca sugerencias de coordenadas y direcciones formateadas para una consulta de texto.
 */
class SearchAddressUseCase(
    private val addressSearchService: AddressSearchService,
) {
    suspend operator fun invoke(query: String): List<AddressSuggestion> =
        addressSearchService.search(query)
}

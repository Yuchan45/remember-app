package com.example.uade.rememberapp.domain.repository

import com.example.uade.rememberapp.domain.model.AddressSuggestion

/**
 * Servicio para buscar coordenadas a partir de una dirección (geocodificación)
 * o resolver la dirección actual del dispositivo.
 */
interface AddressSearchService {
    /** Busca posibles coincidencias a partir de un texto libre de dirección. */
    suspend fun search(query: String): List<AddressSuggestion>

    /** Obtiene la ubicación actual del dispositivo y su dirección aproximada si hay permisos. */
    suspend fun getCurrentLocation(): AddressSuggestion?
}

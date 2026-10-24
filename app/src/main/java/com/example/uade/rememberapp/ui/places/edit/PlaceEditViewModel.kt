package com.example.uade.rememberapp.ui.places.edit

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.AddressSuggestion
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceKind
import com.example.uade.rememberapp.domain.usecase.GetCurrentLocationUseCase
import com.example.uade.rememberapp.domain.usecase.SavePlaceUseCase
import com.example.uade.rememberapp.domain.usecase.SearchAddressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaceEditUiState(
    val placeId: Long = 0,
    val name: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val formattedAddress: String? = null,
    val radiusMeters: Int = 150,
    val kind: PlaceKind = PlaceKind.Other,
    val addressQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<AddressSuggestion> = emptyList(),
    val hasSearched: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    @get:StringRes val errorMessageRes: Int? = null,
    val hasLocationPermission: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && latitude != null && longitude != null && !isSaving
    val isNew: Boolean get() = placeId == 0L
    val hasSelectedLocation: Boolean get() = latitude != null && longitude != null
}

class PlaceEditViewModel(
    private val savePlaceUseCase: SavePlaceUseCase,
    private val searchAddressUseCase: SearchAddressUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceEditUiState())
    val uiState: StateFlow<PlaceEditUiState> = _uiState.asStateFlow()

    fun init(place: Place? = null) {
        if (place != null) {
            _uiState.value = PlaceEditUiState(
                placeId = place.id,
                name = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                radiusMeters = place.radiusMeters,
                kind = place.kind,
            )
        } else {
            _uiState.value = PlaceEditUiState()
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, errorMessageRes = null) }
    }

    fun onKindChanged(kind: PlaceKind) {
        _uiState.update { it.copy(kind = kind) }
    }

    fun onRadiusChanged(meters: Int) {
        _uiState.update { it.copy(radiusMeters = meters) }
    }

    fun onAddressQueryChanged(query: String) {
        _uiState.update { it.copy(addressQuery = query, hasSearched = false) }
    }

    fun onSearchAddress() {
        val query = _uiState.value.addressQuery.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessageRes = null) }
            val results = searchAddressUseCase(query)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = results,
                    hasSearched = true,
                )
            }
        }
    }

    fun onAddressSuggestionSelected(suggestion: AddressSuggestion) {
        _uiState.update { current ->
            val suggestedName = if (current.name.isBlank()) suggestion.featureName.orEmpty() else current.name
            current.copy(
                name = suggestedName,
                latitude = suggestion.latitude,
                longitude = suggestion.longitude,
                formattedAddress = suggestion.addressLine,
                searchResults = emptyList(),
                addressQuery = "",
                hasSearched = false,
                errorMessageRes = null,
            )
        }
    }

    fun onUseCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessageRes = null) }
            val current = getCurrentLocationUseCase()
            if (current != null) {
                _uiState.update { state ->
                    val suggestedName = if (state.name.isBlank()) current.featureName.orEmpty() else state.name
                    state.copy(
                        name = suggestedName,
                        latitude = current.latitude,
                        longitude = current.longitude,
                        formattedAddress = current.addressLine,
                        isSearching = false,
                        errorMessageRes = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        errorMessageRes = R.string.places_edit_error_no_location,
                    )
                }
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted && _uiState.value.latitude == null) {
            onUseCurrentLocation()
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessageRes = R.string.places_edit_error_no_name) }
            return
        }
        val lat = state.latitude
        val lng = state.longitude
        if (lat == null || lng == null) {
            _uiState.update { it.copy(errorMessageRes = R.string.places_edit_error_no_location) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            savePlaceUseCase(
                Place(
                    id = state.placeId,
                    name = state.name.trim(),
                    latitude = lat,
                    longitude = lng,
                    radiusMeters = state.radiusMeters,
                    kind = state.kind,
                ),
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                PlaceEditViewModel(
                    savePlaceUseCase = app.container.savePlaceUseCase,
                    searchAddressUseCase = app.container.searchAddressUseCase,
                    getCurrentLocationUseCase = app.container.getCurrentLocationUseCase,
                )
            }
        }
    }
}

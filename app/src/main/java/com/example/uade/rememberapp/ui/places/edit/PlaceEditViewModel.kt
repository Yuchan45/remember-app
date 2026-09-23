package com.example.uade.rememberapp.ui.places.edit

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaceEditUiState(
    val name: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = 150,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
)

/** TODO: recibir SavePlaceUseCase y el proveedor de ubicación actual. */
class PlaceEditViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceEditUiState())
    val uiState: StateFlow<PlaceEditUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) = Unit

    fun onLocationPicked(latitude: Double, longitude: Double) = Unit

    fun onRadiusChanged(meters: Int) = Unit

    fun onSave() = Unit
}

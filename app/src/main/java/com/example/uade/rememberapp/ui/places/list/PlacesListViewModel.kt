package com.example.uade.rememberapp.ui.places.list

import androidx.lifecycle.ViewModel
import com.example.uade.rememberapp.domain.model.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlacesListUiState(
    val isLoading: Boolean = false,
    val places: List<Place> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && places.isEmpty()
}

/** TODO: recibir ObservePlacesUseCase y DeletePlaceUseCase por constructor. */
class PlacesListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlacesListUiState())
    val uiState: StateFlow<PlacesListUiState> = _uiState.asStateFlow()

    fun onPlaceDeleted(id: Long) = Unit
}

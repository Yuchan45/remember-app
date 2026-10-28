package com.example.uade.rememberapp.ui.places.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.usecase.DeletePlaceUseCase
import com.example.uade.rememberapp.domain.usecase.ObservePlacesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlacesListUiState(
    val isLoading: Boolean = false,
    val places: List<Place> = emptyList(),
    val editingPlace: Place? = null,
    val isCreatingNewPlace: Boolean = false,
    val placeToDelete: Place? = null,
) {
    val isEmpty: Boolean get() = !isLoading && places.isEmpty()
}

class PlacesListViewModel(
    private val observePlacesUseCase: ObservePlacesUseCase,
    private val deletePlaceUseCase: DeletePlaceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlacesListUiState(isLoading = true))
    val uiState: StateFlow<PlacesListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePlacesUseCase().collect { places ->
                _uiState.update { it.copy(isLoading = false, places = places) }
            }
        }
    }

    fun onAddNewPlaceClick() {
        _uiState.update { it.copy(isCreatingNewPlace = true, editingPlace = null) }
    }

    fun onEditPlaceClick(place: Place) {
        _uiState.update { it.copy(editingPlace = place, isCreatingNewPlace = false) }
    }

    fun onDismissSheet() {
        _uiState.update { it.copy(isCreatingNewPlace = false, editingPlace = null) }
    }

    fun onRequestDeletePlace(place: Place) {
        _uiState.update { it.copy(placeToDelete = place) }
    }

    fun onCancelDeletePlace() {
        _uiState.update { it.copy(placeToDelete = null) }
    }

    fun onConfirmDeletePlace() {
        val place = _uiState.value.placeToDelete ?: return
        _uiState.update { it.copy(placeToDelete = null) }
        viewModelScope.launch {
            deletePlaceUseCase(place.id)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                PlacesListViewModel(
                    observePlacesUseCase = app.container.observePlacesUseCase,
                    deletePlaceUseCase = app.container.deletePlaceUseCase,
                )
            }
        }
    }
}

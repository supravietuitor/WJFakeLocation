// MapViewModel.kt
package com.steadywj.wjfakelocation.manager.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steadywj.wjfakelocation.data.model.SelectedLocation
import com.steadywj.wjfakelocation.data.repository.FavoritesRepository
import com.steadywj.wjfakelocation.data.repository.PreferencesRepository
import com.steadywj.wjfakelocation.manager.map.utils.AMapManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val favoritesRepository: FavoritesRepository,
    private val aMapManager: AMapManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _selectedLocation = MutableStateFlow<SelectedLocation?>(null)
    val selectedLocation: StateFlow<SelectedLocation?> = _selectedLocation.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun selectLocation(latitude: Double, longitude: Double, address: String? = null) {
        viewModelScope.launch {
            val location = SelectedLocation(latitude, longitude, address)
            _selectedLocation.value = location
            preferencesRepository.updateSelectedLocation(location)
        }
    }

    fun startFakeLocation() {
        viewModelScope.launch {
            preferencesRepository.updateIsPlaying(true)
            _isPlaying.value = true
        }
    }

    fun stopFakeLocation() {
        viewModelScope.launch {
            preferencesRepository.updateIsPlaying(false)
            _isPlaying.value = false
        }
    }

    fun toggleFakeLocation() {
        if (_isPlaying.value) {
            stopFakeLocation()
        } else {
            startFakeLocation()
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                searchQuery = query,
                errorMessage = null
            )

            try {
                aMapManager.geocodeAddress(query).collect { result ->
                    result.onSuccess { latLng ->
                        selectLocation(latLng.latitude, latLng.longitude, latLng.address)
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Search failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Search failed"
                )
            }
        }
    }

    fun clearSearch() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                searchQuery = "",
                isLoading = false,
                errorMessage = null
            )
        }
    }
}

data class MapUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val showAddFavoriteDialog: Boolean = false
)

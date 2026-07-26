// MapViewModel.kt
package com.steadywj.wjfakelocation.manager.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steadywj.wjfakelocation.data.model.SelectedLocation
import com.steadywj.wjfakelocation.data.repository.FavoritesRepository
import com.steadywj.wjfakelocation.data.repository.PreferencesRepository
import com.steadywj.wjfakelocation.manager.map.common.AMapManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * åœ°å›¾ ViewModel
 * ç®¡ç†åœ°å›¾ç•Œé¢çš„çŠ¶æ€å’Œä¸šåŠ¡é€»è¾‘
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val favoritesRepository: FavoritesRepository,
    private val aMapManager: AMapManager
) : ViewModel() {

    /** UI çŠ¶æ€?*/
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** é€‰ä¸­çš„ä½ç½?*/
    private val _selectedLocation = MutableStateFlow<SelectedLocation?>(null)
    val selectedLocation: StateFlow<SelectedLocation?> = _selectedLocation.asStateFlow()

    /** æ˜¯å¦æ­£åœ¨è¿è¡Œï¼ˆä¼ªé€ ä¸­ï¼?*/
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /**
     * é€‰æ‹©ä½ç½®
     * @param latitude çº¬åº¦
     * @param longitude ç»åº¦
     * @param address åœ°å€ï¼ˆå¯é€‰ï¼‰
     */
    fun selectLocation(latitude: Double, longitude: Double, address: String? = null) {
        viewModelScope.launch {
            val location = SelectedLocation(latitude, longitude, address)
            _selectedLocation.value = location
            preferencesRepository.updateSelectedLocation(location)
        }
    }

    /**
     * å¯åŠ¨è™šæ‹Ÿå®šä½
     */
    fun startFakeLocation() {
        viewModelScope.launch {
            preferencesRepository.updateIsPlaying(true)
            _isPlaying.value = true
        }
    }

    /**
     * åœæ­¢è™šæ‹Ÿå®šä½
     */
    fun stopFakeLocation() {
        viewModelScope.launch {
            preferencesRepository.updateIsPlaying(false)
            _isPlaying.value = false
        }
    }

    /**
     * åˆ‡æ¢è™šæ‹Ÿå®šä½çŠ¶æ€?
     */
    fun toggleFakeLocation() {
        if (_isPlaying.value) {
            stopFakeLocation()
        } else {
            startFakeLocation()
        }
    }

    /**
     * æœç´¢ä½ç½®ï¼ˆé«˜å¾·åœ°å›¾åœ°ç†ç¼–ç ï¼‰
     * @param query æœç´¢å…³é”®è¯?
     */
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
                // å®žçŽ°é«˜å¾·åœ°å›¾æœç´¢
                // TODO: æ³¨å…¥ AMapManager å¹¶è°ƒç”?geocodeAddress()
                /*
                aMapManager.geocodeAddress(query).collect { result ->
                    result.onSuccess { latLng ->
                        selectLocation(latLng.latitude, latLng.longitude, latLng.address)
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "æœç´¢å¤±è´¥"
                        )
                    }
                }
                */

                // ä¸´æ—¶å ä½å®žçŽ°
                delay(500L)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "æœç´¢å¤±è´¥"
                )
            }
        }
    }

    /**
     * æ¸…é™¤æœç´¢çŠ¶æ€?
     */
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

/**
 * åœ°å›¾ UI çŠ¶æ€?
 * @property isLoading åŠ è½½çŠ¶æ€?
 * @property searchQuery æœç´¢å…³é”®è¯?
 * @property errorMessage é”™è¯¯æ¶ˆæ¯
 * @property showAddFavoriteDialog æ˜¾ç¤ºæ·»åŠ æ”¶è—å¯¹è¯æ¡?
 */
data class MapUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val showAddFavoriteDialog: Boolean = false
)

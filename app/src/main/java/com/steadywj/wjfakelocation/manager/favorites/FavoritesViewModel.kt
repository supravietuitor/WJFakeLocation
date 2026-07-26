// FavoritesViewModel.kt
package com.steadywj.wjfakelocation.manager.favorites.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steadywj.wjfakelocation.data.model.FavoriteLocation
import com.steadywj.wjfakelocation.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * æ”¶è—å¤?ViewModel
 * ç®¡ç†æ”¶è—å¤¹ç•Œé¢çš„çŠ¶æ€å’Œä¸šåŠ¡é€»è¾‘
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    /** æ‰€æœ‰æ”¶è—é¡¹ï¼ˆå“åº”å¼æ•°æ®æµï¼‰ */
    val allFavorites: StateFlow<List<FavoriteLocation>> = favoritesRepository.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** UI çŠ¶æ€?*/
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    /**
     * æœç´¢æ”¶è—é¡?
     * @param query æœç´¢å…³é”®è¯?
     */
    fun searchFavorites(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    searchQuery = ""
                )
            } else {
                favoritesRepository.searchFavorites(query).collect { favorites ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        searchQuery = query
                    )
                }
            }
        }
    }

    /**
     * æ·»åŠ æ”¶è—é¡?
     * @param name åç§°
     * @param latitude çº¬åº¦
     * @param longitude ç»åº¦
     * @param address åœ°å€
     * @param category åˆ†ç±»
     */
    fun addFavorite(name: String, latitude: Double, longitude: Double, address: String?, category: String) {
        viewModelScope.launch {
            val favorite = FavoriteLocation(
                name = name,
                latitude = latitude,
                longitude = longitude,
                address = address,
                category = category
            )
            favoritesRepository.insertFavorite(favorite)
            _uiState.value = _uiState.value.copy(showSuccessMessage = "å·²æ·»åŠ åˆ°æ”¶è—")
        }
    }

    /**
     * æ›´æ–°æ”¶è—é¡?
     * @param favorite æ”¶è—é¡?
     */
    fun updateFavorite(favorite: FavoriteLocation) {
        viewModelScope.launch {
            val updated = favorite.copy(updatedAt = System.currentTimeMillis())
            favoritesRepository.updateFavorite(updated)
            _uiState.value = _uiState.value.copy(showSuccessMessage = "Done")
        }
    }

    /**
     * åˆ é™¤æ”¶è—é¡?
     * @param favorite æ”¶è—é¡?
     */
    fun deleteFavorite(favorite: FavoriteLocation) {
        viewModelScope.launch {
            favoritesRepository.deleteFavorite(favorite)
            _uiState.value = _uiState.value.copy(showSuccessMessage = "Done")
        }
    }

    /**
     * æ¸…é™¤æ¶ˆæ¯æç¤º
     */
    fun clearMessage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showSuccessMessage = null)
        }
    }
}

/**
 * æ”¶è—å¤?UI çŠ¶æ€?
 * @property isLoading åŠ è½½çŠ¶æ€?
 * @property searchQuery æœç´¢å…³é”®è¯?
 * @property showSuccessMessage æˆåŠŸæ¶ˆæ¯
 * @property showEditDialog æ˜¾ç¤ºç¼–è¾‘å¯¹è¯æ¡?
 * @property editingFavorite æ­£åœ¨ç¼–è¾‘çš„æ”¶è—é¡¹
 */
data class FavoritesUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val showSuccessMessage: String? = null,
    val showEditDialog: Boolean = false,
    val editingFavorite: FavoriteLocation? = null
)

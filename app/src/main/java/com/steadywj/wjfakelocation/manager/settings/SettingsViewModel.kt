// SettingsViewModel.kt
package com.steadywj.wjfakelocation.manager.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steadywj.wjfakelocation.data.model.LocationSettings
import com.steadywj.wjfakelocation.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val settings: StateFlow<LocationSettings> = preferencesRepository.settings

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateAccuracy(enabled: Boolean, value: Double) {
        viewModelScope.launch {
            val current = settings.value
            preferencesRepository.updateSettings(
                current.copy(
                    useAccuracy = enabled,
                    accuracy = value
                )
            )
        }
    }

    fun updateAltitude(enabled: Boolean, value: Double) {
        viewModelScope.launch {
            val current = settings.value
            preferencesRepository.updateSettings(
                current.copy(
                    useAltitude = enabled,
                    altitude = value
                )
            )
        }
    }

    fun updateRandomize(enabled: Boolean, radius: Double) {
        viewModelScope.launch {
            val current = settings.value
            preferencesRepository.updateSettings(
                current.copy(
                    useRandomize = enabled,
                    randomizeRadius = radius
                )
            )
        }
    }

    fun updateSpeed(enabled: Boolean, value: Float) {
        viewModelScope.launch {
            val current = settings.value
            preferencesRepository.updateSettings(
                current.copy(
                    useSpeed = enabled,
                    speed = value
                )
            )
        }
    }

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.saveApiKey(apiKey)
            _uiState.value = _uiState.value.copy(showSuccessMessage = "API Key saved")
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            preferencesRepository.clearApiKey()
            _uiState.value = _uiState.value.copy(showSuccessMessage = "API Key cleared")
        }
    }

    fun saveProfile(name: String) {
        viewModelScope.launch {
            preferencesRepository.saveProfile(name, settings.value)
            _uiState.value = _uiState.value.copy(showSuccessMessage = "Profile saved: $name")
        }
    }

    fun loadProfile(name: String) {
        viewModelScope.launch {
            val profile = preferencesRepository.loadProfile(name)
            profile?.let {
                preferencesRepository.updateSettings(it)
                _uiState.value = _uiState.value.copy(showSuccessMessage = "Profile loaded: $name")
            }
        }
    }

    fun clearMessage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showSuccessMessage = null)
        }
    }
}

data class SettingsUiState(
    val showSuccessMessage: String? = null,
    val showApiKeyDialog: Boolean = false,
    val showProfileDialog: Boolean = false
)
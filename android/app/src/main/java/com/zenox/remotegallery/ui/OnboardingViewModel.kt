package com.zenox.remotegallery.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.remotegallery.core.AppContainer
import com.zenox.remotegallery.core.AppResult
import com.zenox.remotegallery.data.local.prefs.SettingsRepository
import com.zenox.remotegallery.data.remote.dto.DeviceAuthResponse
import com.zenox.remotegallery.util.PermissionUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val registrationResult: DeviceAuthResponse? = null
)

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(OnboardingUiState())
    val ui: StateFlow<OnboardingUiState> = _ui.asStateFlow()
    val settings = container.settingsRepository.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setPermissionsExplained() { viewModelScope.launch { container.settingsRepository.setPermissionsExplained(true) } }
    fun completeOnboarding() { viewModelScope.launch { container.settingsRepository.setOnboardingCompleted(true) } }

    fun register(ownerEmail: String, deviceName: String, serverUrl: String?) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null, message = null) }
            if (!serverUrl.isNullOrBlank()) container.settingsRepository.setServerUrl(serverUrl.trim())
            val existingUuid = settings.value?.deviceUuid
            val existingSecret = container.tokenStore.getDeviceSecret()
            when (val res = container.deviceRepository.register(ownerEmail.trim(), deviceName.trim().ifBlank { android.os.Build.MODEL ?: "Android Device" }, existingUuid, existingSecret)) {
                is AppResult.Success -> _ui.update { it.copy(isLoading = false, registrationResult = res.data, message = "Device registered and authenticated") }
                is AppResult.Error -> _ui.update { it.copy(isLoading = false, error = res.message) }
                else -> _ui.update { it.copy(isLoading = false) }
            }
        }
    }

    fun authenticate(deviceUuid: String, deviceSecret: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            when (val res = container.deviceRepository.authenticate(deviceUuid, deviceSecret)) {
                is AppResult.Success -> _ui.update { it.copy(isLoading = false, registrationResult = res.data, message = "Authenticated") }
                is AppResult.Error -> _ui.update { it.copy(isLoading = false, error = res.message) }
                else -> _ui.update { it.copy(isLoading = false) }
            }
        }
    }

    fun grantConsent(adminEmail: String?) {
        viewModelScope.launch {
            container.settingsRepository.setConsent(true, adminEmail)
            container.settingsRepository.setOnboardingCompleted(true)
            container.settingsRepository.setRemoteAccessEnabled(true)
            _ui.update { it.copy(message = "Remote access consent granted") }
            try { container.deviceRepository.updateStatus("ONLINE") } catch (_: Exception) {}
        }
    }
}

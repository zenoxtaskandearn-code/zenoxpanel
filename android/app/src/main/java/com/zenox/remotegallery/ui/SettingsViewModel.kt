package com.zenox.remotegallery.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.remotegallery.core.AppContainer
import com.zenox.remotegallery.worker.WorkScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings = container.settingsRepository.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val raw = container.settingsRepository.rawFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setTheme(mode: String) = viewModelScope.launch { container.settingsRepository.setTheme(mode) }
    fun setServerUrl(url: String?) = viewModelScope.launch { container.settingsRepository.setServerUrl(url) }
    fun syncNow(ctx: Context) = viewModelScope.launch {
        container.mediaIndexRepository.indexAllAccessibleMedia()
        container.galleryRepository.syncDirtyMetadata(500)
        WorkScheduler.enqueueImmediateSync(ctx)
    }
    fun logout(ctx: Context) = viewModelScope.launch {
        container.settingsRepository.setRemoteAccessEnabled(false)
        container.webSocketManager.disconnect()
        WorkScheduler.stopRemoteAccessService(ctx)
        container.deviceRepository.logoutLocal()
    }
    fun disconnectDevice(ctx: Context) = viewModelScope.launch {
        container.settingsRepository.setRemoteAccessEnabled(false)
        container.settingsRepository.setConsent(false, null)
        try { container.deviceRepository.updateStatus("OFFLINE") } catch (_: Exception) {}
        container.webSocketManager.disconnect()
        WorkScheduler.stopRemoteAccessService(ctx)
    }
    fun revokeAccess(ctx: Context) = viewModelScope.launch {
        disconnectDevice(ctx)
    }
}

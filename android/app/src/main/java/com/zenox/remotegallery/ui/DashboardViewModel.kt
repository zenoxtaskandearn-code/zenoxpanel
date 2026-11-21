package com.zenox.remotegallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.remotegallery.core.AppContainer
import com.zenox.remotegallery.core.AppResult
import com.zenox.remotegallery.worker.WorkScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(val syncing: Boolean = false, val message: String? = null, val error: String? = null)

class DashboardViewModel(private val container: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(DashboardUiState())
    val ui = _ui.asStateFlow()
    val settings = container.settingsRepository.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val rawSettings = container.settingsRepository.rawFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val imageCount = container.galleryRepository.observeImageCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val videoCount = container.galleryRepository.observeVideoCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val activeOps = container.galleryRepository.observeActiveOperations().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val wsState = container.webSocketManager.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DISCONNECTED")

    init {
        viewModelScope.launch {
            settings.collect { s ->
                if (s != null && s.registered && s.remoteAccessConsentGranted && s.remoteAccessEnabled) {
                    container.webSocketManager.connect()
                    WorkScheduler.ensurePeriodicWork(container.appContext)
                } else {
                    container.webSocketManager.disconnect()
                }
            }
        }
    }

    fun onAppContext(ctx: android.content.Context) { WorkScheduler.ensurePeriodicWork(ctx) }

    fun syncNow(ctx: android.content.Context) {
        viewModelScope.launch {
            _ui.update { it.copy(syncing = true, error = null, message = null) }
            val idx = container.mediaIndexRepository.indexAllAccessibleMedia()
            val sync = container.galleryRepository.syncDirtyMetadata(500)
            val ops = container.galleryRepository.fetchPendingOperations()
            val msg = buildString {
                append("Indexed changes: ${idx.getOrElse { 0 }}. ")
                if (sync is AppResult.Success) append("Synced: ${sync.data}. ")
                if (ops is AppResult.Success) append("Pending ops: ${ops.data.size}.")
            }
            _ui.update { it.copy(syncing = false, message = msg.ifBlank { "Sync complete" }, error = (sync as? AppResult.Error)?.message ?: (ops as? AppResult.Error)?.message) }
            WorkScheduler.enqueueImmediateSync(ctx)
        }
    }

    fun setRemoteAccessEnabled(enabled: Boolean, ctx: android.content.Context) {
        viewModelScope.launch {
            container.settingsRepository.setRemoteAccessEnabled(enabled)
            if (enabled) {
                container.deviceRepository.updateStatus("ONLINE")
                container.webSocketManager.connect()
                WorkScheduler.startRemoteAccessService(ctx)
            } else {
                container.deviceRepository.updateStatus("AWAY")
                container.webSocketManager.disconnect()
                WorkScheduler.stopRemoteAccessService(ctx)
            }
        }
    }
}

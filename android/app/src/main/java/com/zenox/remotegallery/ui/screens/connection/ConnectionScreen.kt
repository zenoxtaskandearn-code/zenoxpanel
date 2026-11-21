package com.zenox.remotegallery.ui.screens.connection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenox.remotegallery.core.AppContainer
import com.zenox.remotegallery.data.local.prefs.SettingsRepository
import com.zenox.remotegallery.ui.AppViewModelFactory
import com.zenox.remotegallery.ui.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConnectionScreen(appContainer: AppContainer) {
    val vm: DashboardViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val settings by vm.settings.collectAsStateWithLifecycle()
    val raw by vm.rawSettings.collectAsStateWithLifecycle()
    val ws by vm.wsState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val deviceUuid = settings?.deviceUuid ?: "-"
    val deviceDbId = raw?.get(SettingsRepository.DEVICE_DB_ID)?.toString() ?: "-"
    val deviceName = raw?.get(SettingsRepository.DEVICE_NAME) ?: "-"
    val owner = raw?.get(SettingsRepository.OWNER_EMAIL) ?: settings?.authorizedAdminEmail ?: "-"
    val lastSync = settings?.lastSyncAt?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it)) } ?: "Never"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Device connection", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RowItem("Registered account", owner)
            RowItem("Device name", deviceName)
            RowItem("Device UUID", deviceUuid)
            RowItem("Device record ID", deviceDbId)
            RowItem("Server", settings?.serverUrlOverride ?: com.zenox.remotegallery.core.ServerConfig.baseUrl)
            RowItem("Connection status", ws)
            RowItem("Registration status", if (settings?.registered == true) "Registered" else "Not registered")
            RowItem("Last successful sync", lastSync)
        } }
        Button(onClick = { vm.syncNow(ctx) }, modifier = Modifier.fillMaxWidth()) { Text("Test sync / heartbeat") }
        Text("Note: WebSocket connection is maintained when remote access is enabled and the app/service is active, subject to Android background/network restrictions.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun RowItem(k: String, v: String) {
    Column { Text(k, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Text(v, style = MaterialTheme.typography.bodyMedium) }
}

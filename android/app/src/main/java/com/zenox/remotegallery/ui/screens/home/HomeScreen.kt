package com.zenox.remotegallery.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenox.remotegallery.core.AppContainer
import com.zenox.remotegallery.data.local.prefs.SettingsRepository
import com.zenox.remotegallery.ui.AppViewModelFactory
import com.zenox.remotegallery.ui.DashboardViewModel
import com.zenox.remotegallery.util.PermissionUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(appContainer: AppContainer, onNavigate: (String) -> Unit) {
    val vm: DashboardViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val raw by vm.rawSettings.collectAsStateWithLifecycle()
    val images by vm.imageCount.collectAsStateWithLifecycle()
    val videos by vm.videoCount.collectAsStateWithLifecycle()
    val ws by vm.wsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val perm = PermissionUtils.permissionSummary(context)
    val lastSync = settings?.lastSyncAt?.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it)) } ?: "Never"
    val deviceName = raw?.get(SettingsRepository.DEVICE_NAME) ?: "Not registered"
    val owner = raw?.get(SettingsRepository.OWNER_EMAIL) ?: settings?.authorizedAdminEmail ?: "-"

    LaunchedEffect(Unit) { vm.onAppContext(context) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        if (ui.syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
        ui.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), "Images", images.toString(), Icons.Default.Image)
            StatCard(Modifier.weight(1f), "Videos", videos.toString(), Icons.Default.VideoLibrary)
        }
        InfoCard("Connection & device", listOf(
            "Device registration" to if (settings?.registered == true) "Registered" else "Not registered",
            "Device name" to deviceName,
            "Owner/admin" to owner,
            "Server" to (settings?.serverUrlOverride ?: com.zenox.remotegallery.core.ServerConfig.baseUrl),
            "Connection status" to ws,
            "Server status" to if (ws == "CONNECTED") "Reachable" else "Not connected"
        ))
        InfoCard("Access & sync", listOf(
            "Remote access" to if (settings?.remoteAccessEnabled == true && settings?.remoteAccessConsentGranted == true) "Enabled" else "Disabled",
            "Consent" to if (settings?.remoteAccessConsentGranted == true) "Granted" else "Not granted",
            "Permission status" to perm,
            "Last synchronization" to lastSync
        ))

        Button(onClick = { vm.syncNow(context) }, modifier = Modifier.fillMaxWidth(), enabled = !ui.syncing) {
            Icon(Icons.Default.Sync, null); Spacer(Modifier.width(8.dp)); Text("Sync & index now")
        }
        OutlinedButton(onClick = { onNavigate(com.zenox.remotegallery.ui.navigation.Routes.RemoteAccess) }, modifier = Modifier.fillMaxWidth()) {
            Text("Manage remote access")
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun InfoCard(title: String, rows: List<Pair<String, String>>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(v, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

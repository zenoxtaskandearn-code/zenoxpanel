package com.zenox.remotegallery.ui.screens.remoteaccess

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
import com.zenox.remotegallery.ui.AppViewModelFactory
import com.zenox.remotegallery.ui.DashboardViewModel
import com.zenox.remotegallery.ui.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemoteAccessScreen(appContainer: AppContainer) {
    val dash: DashboardViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val svm: SettingsViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val settings by dash.settings.collectAsStateWithLifecycle()
    val ws by dash.wsState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val enabled = settings?.remoteAccessEnabled == true && settings?.remoteAccessConsentGranted == true
    val consentTime = settings?.consentTimestamp?.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it)) } ?: "N/A"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Remote access", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Text("Remote access enabled"); Text(if (enabled) "Active when connected" else "Disabled", style = MaterialTheme.typography.bodySmall) }
                Switch(checked = settings?.remoteAccessEnabled == true, onCheckedChange = { dash.setRemoteAccessEnabled(it, ctx) }, enabled = settings?.remoteAccessConsentGranted == true)
            }
            Divider()
            Text("Authorized administrator: ${settings?.authorizedAdminEmail ?: "Not set"}")
            Text("Consent granted: ${if (settings?.remoteAccessConsentGranted == true) "Yes ($consentTime)" else "No"}")
            Text("Session/connection: $ws")
            Text("Status: ${if (enabled) "Available for authorized requests" else "Not available"}", color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Consent information", style = MaterialTheme.typography.titleSmall)
                Text("• Only accessible media under your Android permission selection can be indexed/accessed.")
                Text("• Operations requiring system approval will prompt Android dialogs.")
                Text("• You retain full control and can revoke access anytime.")
            }
        }
        OutlinedButton(onClick = { dash.setRemoteAccessEnabled(false, ctx) }, modifier = Modifier.fillMaxWidth()) { Text("Disconnect / pause remote access") }
        Button(onClick = { svm.revokeAccess(ctx) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Revoke access & consent") }
    }
}

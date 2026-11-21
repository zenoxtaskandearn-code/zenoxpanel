package com.zenox.remotegallery.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenox.remotegallery.data.local.prefs.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(onContinue: () -> Unit, onSetExplained: () -> Unit = {}, settingsRepository: SettingsRepository) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("Remote Gallery", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Secure, owner-approved remote access to your photos and videos.", style = MaterialTheme.typography.titleMedium)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy and control", style = MaterialTheme.typography.titleMedium)
                    Text("• Media stays on this device; the server coordinates requests and metadata.")
                    Text("• You explicitly approve pairing and remote access consent.")
                    Text("• You can disconnect or revoke access at any time in Settings/Remote Access.")
                    Text("• Sensitive actions require your permissions and Android system confirmations where applicable.")
                }
            }
        }
        Button(onClick = {
            scope.launch { settingsRepository.setPermissionsExplained(false); onContinue() }
        }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

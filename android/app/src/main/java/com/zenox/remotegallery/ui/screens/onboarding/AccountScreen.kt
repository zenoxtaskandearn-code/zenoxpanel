package com.zenox.remotegallery.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenox.remotegallery.core.AppContainer
import com.zenox.remotegallery.ui.AppViewModelFactory
import com.zenox.remotegallery.ui.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(appContainer: AppContainer, onPaired: () -> Unit, onBack: () -> Unit) {
    val vm: OnboardingViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var ownerEmail by remember(settings) { mutableStateOf(settings?.authorizedAdminEmail ?: "") }
    var deviceName by remember { mutableStateOf(android.os.Build.MODEL ?: "Android Device") }
    var registrationCode by remember { mutableStateOf("") } // UX "registration code"; mapped to pairing context/notes for backend adaptation
    var serverUrl by remember(settings) { mutableStateOf(settings?.serverUrlOverride ?: "") }
    var showAdvanced by remember { mutableStateOf(false) }

    LaunchedEffect(ui.registrationResult) { if (ui.registrationResult != null) onPaired() }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Device pairing & registration", style = MaterialTheme.typography.headlineMedium)
        Text("Pair this device with an authorized administrator account. Registration requires explicit owner approval on the server side; this app authenticates using device credentials over a secure connection.")
        OutlinedTextField(ownerEmail, { ownerEmail = it }, label = { Text("Administrator/owner email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(deviceName, { deviceName = it }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(registrationCode, { registrationCode = it }, label = { Text("Registration/pairing code (if provided)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        TextButton(onClick = { showAdvanced = !showAdvanced }) { Text(if (showAdvanced) "Hide server settings" else "Server settings") }
        if (showAdvanced) {
            OutlinedTextField(serverUrl, { serverUrl = it }, label = { Text("Server URL (dev/prod override)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("https://your-render-backend") })
            Text("Use configured BuildConfig URLs by default. Override only when needed.", style = MaterialTheme.typography.bodySmall)
        }
        if (!ui.error.isNullOrBlank()) Text(ui.error!!, color = MaterialTheme.colorScheme.error)
        if (!ui.message.isNullOrBlank()) Text(ui.message!!, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val codeHint = registrationCode.trim()
                val email = if (codeHint.contains("@") && ownerEmail.isBlank()) codeHint else ownerEmail.trim()
                vm.register(email, deviceName, serverUrl.ifBlank { null })
            },
            enabled = !ui.isLoading && (ownerEmail.isNotBlank() || registrationCode.isNotBlank()),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (ui.isLoading) "Connecting..." else "Register & pair device") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !ui.isLoading) { Text("Back") }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Note", style = MaterialTheme.typography.titleSmall)
                Text("This client adapts to the available backend API. If your deployment uses a separate pairing-code issuance flow, enter the owner email and approved device details to complete registration.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

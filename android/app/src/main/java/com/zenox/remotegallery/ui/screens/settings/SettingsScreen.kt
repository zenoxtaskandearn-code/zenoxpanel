package com.zenox.remotegallery.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import com.zenox.remotegallery.ui.SettingsViewModel
import com.zenox.remotegallery.ui.navigation.Routes
import com.zenox.remotegallery.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(appContainer: AppContainer, onRequireOnboarding: (String) -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val settings by vm.settings.collectAsStateWithLifecycle()
    val raw by vm.raw.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var serverUrl by remember(settings?.serverUrlOverride) { mutableStateOf(settings?.serverUrlOverride ?: "") }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val perm = PermissionUtils.permissionSummary(ctx)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Server", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(serverUrl, { serverUrl = it }, label = { Text("Server URL override") }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(com.zenox.remotegallery.core.ServerConfig.baseUrl) })
            Text("BuildConfig defaults: dev=${com.zenox.remotegallery.BuildConfig.DEV_BASE_URL}, prod=${com.zenox.remotegallery.BuildConfig.PROD_BASE_URL}", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { vm.setServerUrl(serverUrl.ifBlank { null }) }) { Text("Save server URL") }
        } }

        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            val mode = settings?.themeMode ?: "system"
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "system", onClick = { vm.setTheme("system") }, label = { Text("System") })
                FilterChip(selected = mode == "light", onClick = { vm.setTheme("light") }, label = { Text("Light") })
                FilterChip(selected = mode == "dark", onClick = { vm.setTheme("dark") }, label = { Text("Dark") })
            }
        } }

        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Permissions & privacy", style = MaterialTheme.typography.titleMedium)
            Text("Current media access: $perm")
            Text("Remote access consent: ${if (settings?.remoteAccessConsentGranted == true) "Granted" else "Not granted"}")
            if (Build.VERSION.SDK_INT >= 34 && PermissionUtils.isSelectedAccessOnly(ctx)) {
                Text("Selected-photo access is active. Only chosen items can be indexed/accessed.", color = MaterialTheme.colorScheme.primary)
            }
            OutlinedButton(onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", ctx.packageName, null) }
                ctx.startActivity(intent)
            }) { Text("Manage app permissions") }
            OutlinedButton(onClick = { onRequireOnboarding(Routes.Consent) }, enabled = settings?.registered == true) { Text("Review consent / remote access") }
        } }

        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Synchronization", style = MaterialTheme.typography.titleMedium)
            Text("Periodic metadata sync and pending-request checks run via WorkManager when network is available.")
            Button(onClick = { vm.syncNow(ctx) }) { Text("Run sync/index now") }
        } }

        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Account & device", style = MaterialTheme.typography.titleMedium)
            Text("Device: ${raw?.get(SettingsRepository.DEVICE_NAME) ?: "-"}")
            Text("Owner: ${raw?.get(SettingsRepository.OWNER_EMAIL) ?: settings?.authorizedAdminEmail ?: "-"}")
            OutlinedButton(onClick = { vm.disconnectDevice(ctx) }) { Text("Disconnect device / disable access") }
            Button(onClick = { showLogoutConfirm = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Logout (clear local session)") }
        } }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout") },
            text = { Text("This clears local tokens and registration state on this device. You can pair again later.") },
            confirmButton = { TextButton(onClick = { vm.logout(ctx); showLogoutConfirm = false; onRequireOnboarding(Routes.Welcome) }) { Text("Logout") } },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } }
        )
    }
}

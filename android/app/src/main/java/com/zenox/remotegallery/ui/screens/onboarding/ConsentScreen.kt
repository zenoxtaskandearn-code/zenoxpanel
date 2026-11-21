package com.zenox.remotegallery.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenox.remotegallery.core.AppContainer
import com.zenox.remotegallery.ui.AppViewModelFactory
import com.zenox.remotegallery.ui.OnboardingViewModel

@Composable
fun ConsentScreen(appContainer: AppContainer, onAccepted: () -> Unit) {
    val vm: OnboardingViewModel = viewModel(factory = AppViewModelFactory(appContainer))
    val settings by vm.settings.collectAsStateWithLifecycle()
    var checked by remember { mutableStateOf(false) }
    val adminEmail = settings?.authorizedAdminEmail

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Remote access consent", style = MaterialTheme.typography.headlineMedium)
        Text("Review and explicitly opt in before enabling remote gallery access.")
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("What can be accessed", style = MaterialTheme.typography.titleMedium)
            Text("• Photos and videos accessible under your current Android permission selection (full or selected).")
            Text("• Media metadata (name, type, size, dates, dimensions/duration) for indexing and synchronization.")
            Text("• On-demand previews and authorized operations only when requested and consent is active.")
        } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Who can access", style = MaterialTheme.typography.titleMedium)
            Text("• The authorized administrator account associated with this device pairing${if (!adminEmail.isNullOrBlank()) ": $adminEmail" else "."}")
            Text("• Access is subject to backend authentication, device authorization, and active consent.")
        } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("When access is active & your controls", style = MaterialTheme.typography.titleMedium)
            Text("• Active only while remote access is enabled and this device is connected.")
            Text("• You can disconnect, disable remote access, or revoke consent anytime from Remote Access/Settings.")
            Text("• Android system dialogs will be shown for operations requiring system approval (e.g. delete/edit requests).")
        } }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { checked = it })
            Spacer(Modifier.width(8.dp))
            Text("I explicitly consent to enable remote gallery access for this device under the conditions above.")
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = { vm.grantConsent(adminEmail); onAccepted() }, enabled = checked, modifier = Modifier.fillMaxWidth()) {
            Text("Enable remote access")
        }
    }
}

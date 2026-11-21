package com.zenox.remotegallery.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenox.remotegallery.util.PermissionUtils

@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var summary by remember { mutableStateOf(PermissionUtils.permissionSummary(context)) }
    val permissions = PermissionUtils.requiredMediaPermissions()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        summary = PermissionUtils.permissionSummary(context)
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Media permissions", style = MaterialTheme.typography.headlineMedium)
        Text("Remote Gallery needs access to photos and videos to index metadata and fulfill authorized remote requests.")
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Why access is needed")
            Text("• Discover and index accessible media (images/videos) from this device.")
            Text("• Show local gallery and sync metadata with your approved server.")
            Text("• Retrieve previews or perform authorized operations you approve.")
        } }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Android permission choices")
                if (Build.VERSION.SDK_INT >= 34) {
                    Text("• Android 14+: you may grant selected photos/videos or full access. If you choose selected access, only those items can be indexed and accessed remotely.")
                } else if (Build.VERSION.SDK_INT >= 33) {
                    Text("• Android 13+: grant Photos and Videos access (READ_MEDIA_IMAGES / READ_MEDIA_VIDEO).")
                } else {
                    Text("• Grant media/storage access so the app can read photos and videos.")
                }
                Text("Current status: $summary")
            }
        }
        Button(onClick = { launcher.launch(permissions) }, modifier = Modifier.fillMaxWidth()) { Text("Grant media permissions") }
        if (Build.VERSION.SDK_INT >= 33) {
            OutlinedButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) { Text("Allow notifications (recommended)") }
        }
        Spacer(Modifier.weight(1f))
        val canContinue = PermissionUtils.hasAnyMediaAccess(context)
        if (!canContinue) {
            Text("Media access is required to continue. You can choose selected items on supported Android versions.", color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = { summary = PermissionUtils.permissionSummary(context); onContinue() }, enabled = canContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

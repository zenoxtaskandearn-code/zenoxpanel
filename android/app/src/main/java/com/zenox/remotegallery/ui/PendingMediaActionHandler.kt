package com.zenox.remotegallery.ui

import android.app.Activity
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.zenox.remotegallery.core.AppResult
import com.zenox.remotegallery.data.repository.DeleteAction
import com.zenox.remotegallery.data.repository.EditAction
import com.zenox.remotegallery.data.repository.PendingMediaActionStore
import kotlinx.coroutines.launch

@Composable
fun PendingMediaActionHandler(container: com.zenox.remotegallery.core.AppContainer) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(PendingMediaActionStore.get()) }
    var message by remember { mutableStateOf<String?>(null) }

    // Poll store lightly while app is foreground
    LaunchedEffect(Unit) {
        while (true) {
            pending = PendingMediaActionStore.get()
            kotlinx.coroutines.delay(1500)
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val action = pending as? DeleteAction ?: return@rememberLauncherForActivityResult
        scope.launch {
            if (res.resultCode == Activity.RESULT_OK) {
                container.galleryRepository.updateOperationStatus(action.operationId, "COMPLETED", "Delete approved and completed on device")
                container.mediaIndexRepository.indexAllAccessibleMedia()
                container.galleryRepository.syncDirtyMetadata(200)
                message = "Delete operation completed"
            } else {
                container.galleryRepository.updateOperationStatus(action.operationId, "FAILED", failureReason = "User denied delete request")
                message = "Delete operation denied"
            }
            PendingMediaActionStore.consume(action.operationId)
            pending = null
        }
    }

    val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val action = pending as? EditAction ?: return@rememberLauncherForActivityResult
        scope.launch {
            if (res.resultCode == Activity.RESULT_OK) {
                // After write approval, perform simple supported edits (e.g. rename) where possible
                val resultMsg = tryPerformEdit(ctx, action)
                container.galleryRepository.updateOperationStatus(action.operationId, "COMPLETED", resultMsg)
                container.mediaIndexRepository.indexAllAccessibleMedia()
                container.galleryRepository.syncDirtyMetadata(200)
                message = "Edit operation completed"
            } else {
                container.galleryRepository.updateOperationStatus(action.operationId, "FAILED", failureReason = "User denied edit request")
                message = "Edit operation denied"
            }
            PendingMediaActionStore.consume(action.operationId)
            pending = null
        }
    }

    LaunchedEffect(pending) {
        when (val p = pending) {
            is DeleteAction -> {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        val pi = MediaStore.createDeleteRequest(ctx.contentResolver, listOf(p.uri))
                        deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                    } else {
                        // Best effort on older devices
                        val rows = ctx.contentResolver.delete(p.uri, null, null)
                        if (rows > 0) {
                            container.galleryRepository.updateOperationStatus(p.operationId, "COMPLETED", "Deleted on device")
                        } else {
                            container.galleryRepository.updateOperationStatus(p.operationId, "FAILED", failureReason = "Unable to delete media on this Android version without system approval flow")
                        }
                        PendingMediaActionStore.consume(p.operationId)
                        pending = null
                    }
                } catch (se: SecurityException) {
                    container.galleryRepository.updateOperationStatus(p.operationId, "FAILED", failureReason = "SecurityException: ${se.message}")
                    PendingMediaActionStore.consume(p.operationId); pending = null
                } catch (e: Exception) {
                    container.galleryRepository.updateOperationStatus(p.operationId, "FAILED", failureReason = e.message ?: "Delete failed")
                    PendingMediaActionStore.consume(p.operationId); pending = null
                }
            }
            is EditAction -> {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        val pi = MediaStore.createWriteRequest(ctx.contentResolver, listOf(p.uri))
                        writeLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                    } else {
                        val resultMsg = tryPerformEdit(ctx, p)
                        container.galleryRepository.updateOperationStatus(p.operationId, "COMPLETED", resultMsg)
                        PendingMediaActionStore.consume(p.operationId); pending = null
                    }
                } catch (se: SecurityException) {
                    container.galleryRepository.updateOperationStatus(p.operationId, "FAILED", failureReason = "SecurityException: ${se.message}")
                    PendingMediaActionStore.consume(p.operationId); pending = null
                } catch (e: Exception) {
                    container.galleryRepository.updateOperationStatus(p.operationId, "FAILED", failureReason = e.message ?: "Edit failed")
                    PendingMediaActionStore.consume(p.operationId); pending = null
                }
            }
            else -> {}
        }
    }

    if (message != null) {
        AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }, title = { Text("Remote operation") }, text = { Text(message!!) })
    }
}

private fun tryPerformEdit(context: android.content.Context, action: EditAction): String {
    val p = action.payload
    val rename = p["newName"] ?: p["fileName"]
    if (!rename.isNullOrBlank()) {
        return try {
            val values = android.content.ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, rename) }
            val rows = context.contentResolver.update(action.uri, values, null, null)
            if (rows > 0) "Edit approved. Renamed media on device." else "Edit approved, but rename not supported/applied for this item."
        } catch (e: Exception) {
            "Edit approved by user, but automatic change could not be applied: ${e.message}"
        }
    }
    return "Edit approved by user. No automatic metadata transformation was requested/supported for this item; Android authorization completed."
}

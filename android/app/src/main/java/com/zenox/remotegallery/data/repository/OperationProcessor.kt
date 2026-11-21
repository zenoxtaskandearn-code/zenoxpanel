package com.zenox.remotegallery.data.repository

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.zenox.remotegallery.MainActivity
import com.zenox.remotegallery.core.AppResult
import com.zenox.remotegallery.data.local.db.MediaDao
import com.zenox.remotegallery.data.local.db.OperationLogEntity
import com.zenox.remotegallery.data.local.prefs.SettingsRepository
import com.zenox.remotegallery.data.remote.dto.OperationView
import com.zenox.remotegallery.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream

class OperationProcessor(
    private val context: Context,
    private val galleryRepository: GalleryRepository,
    private val mediaDao: MediaDao,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun processPendingOperations(): Result<Int> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.registered || !settings.remoteAccessConsentGranted || !settings.remoteAccessEnabled) {
            return@withContext Result.success(0)
        }
        if (!PermissionUtils.hasAnyMediaAccess(context)) return@withContext Result.success(0)
        when (val res = galleryRepository.fetchPendingOperations()) {
            is AppResult.Success -> {
                var handled = 0
                for (op in res.data) {
                    if (op.status in listOf("PENDING")) {
                        handleOperation(op)
                        handled++
                    } else if (op.status in listOf("ACKNOWLEDGED", "IN_PROGRESS")) {
                        // may require UI continuation; report state
                    }
                }
                Result.success(handled)
            }
            is AppResult.Error -> Result.failure(IllegalStateException(res.message))
            else -> Result.success(0)
        }
    }

    suspend fun handleOperation(op: OperationView): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            galleryRepository.ackOperation(op.id, "Received on device")
            val settings = settingsRepository.settingsFlow.first()
            if (!settings.remoteAccessConsentGranted || !settings.remoteAccessEnabled) {
                galleryRepository.updateOperationStatus(op.id, "FAILED", failureReason = "Remote access consent disabled on device")
                return@withContext AppResult.Error("Consent disabled")
            }
            when (op.operationType.uppercase()) {
                "PREVIEW" -> handlePreview(op)
                "DELETE" -> handleDelete(op)
                "EDIT" -> handleEdit(op)
                else -> {
                    galleryRepository.updateOperationStatus(op.id, "FAILED", failureReason = "Unsupported operation: ${op.operationType}")
                    AppResult.Error("Unsupported operation")
                }
            }
        } catch (e: Exception) {
            galleryRepository.updateOperationStatus(op.id, "FAILED", failureReason = e.message ?: "Operation failed")
            AppResult.Error(e.message ?: "Operation failed", cause = e)
        }
    }

    private suspend fun handlePreview(op: OperationView): AppResult<Unit> {
        galleryRepository.updateOperationStatus(op.id, "IN_PROGRESS", "Preparing secure on-device preview")
        // Try to resolve media: prefer local metadata mapping by backend/external id not directly known; use payload hint if provided
        val payload = parsePayload(op.payload)
        val localUriStr = payload["contentUri"] ?: payload["localUri"]
        val mediaStoreId = payload["mediaStoreId"]?.toLongOrNull()
        val mediaType = payload["mediaType"] ?: "IMAGE"
        val entity = when {
            !localUriStr.isNullOrBlank() -> null
            mediaStoreId != null -> mediaDao.findByMediaStoreId(mediaStoreId, mediaType.uppercase())
            else -> null
        }
        val uri = when {
            !localUriStr.isNullOrBlank() -> localUriStr.toUri()
            entity != null -> entity.contentUri.toUri()
            else -> null
        }
        if (uri == null) {
            galleryRepository.updateOperationStatus(op.id, "FAILED", failureReason = "Requested media not found on device index")
            return AppResult.Error("Media not found")
        }
        val accessible = canAccessUri(uri)
        if (!accessible) {
            galleryRepository.updateOperationStatus(op.id, "FAILED", failureReason = "Media not accessible under current permissions")
            return AppResult.Error("Not accessible")
        }
        val info = buildPreviewInfo(uri, op)
        // Backend has no binary relay endpoint; report completed with on-device retrieval details.
        galleryRepository.updateOperationStatus(
            op.id,
            "COMPLETED",
            "Preview prepared on device. $info Backend media relay endpoint unavailable; admin dashboard can display metadata/contract state."
        )
        return AppResult.Success(Unit)
    }

    private suspend fun handleDelete(op: OperationView): AppResult<Unit> {
        val payload = parsePayload(op.payload)
        val mediaStoreId = payload["mediaStoreId"]?.toLongOrNull()
        val mediaType = (payload["mediaType"] ?: "IMAGE").uppercase()
        val uriStr = payload["contentUri"]
        val entity = if (mediaStoreId != null) mediaDao.findByMediaStoreId(mediaStoreId, mediaType) else null
        val uri = uriStr?.toUri() ?: entity?.contentUri?.toUri()
        if (uri == null) {
            galleryRepository.updateOperationStatus(op.id, "FAILED", failureReason = "Media not found for delete")
            return AppResult.Error("Not found")
        }
        // Deletion requires user confirmation via system dialog. Mark in-progress and create request intent for UI/service to launch.
        galleryRepository.updateOperationStatus(op.id, "IN_PROGRESS", "Delete requires user confirmation in Android system dialog. Open Remote Gallery to approve.")
        PendingMediaActionStore.set(DeleteAction(op.id, uri))
        notifyUserActionRequired("Delete approval required", "Approve media deletion requested by administrator")
        return AppResult.Success(Unit)
    }

    private suspend fun handleEdit(op: OperationView): AppResult<Unit> {
        val payload = parsePayload(op.payload)
        val mediaStoreId = payload["mediaStoreId"]?.toLongOrNull()
        val mediaType = (payload["mediaType"] ?: "IMAGE").uppercase()
        val uriStr = payload["contentUri"]
        val entity = if (mediaStoreId != null) mediaDao.findByMediaStoreId(mediaStoreId, mediaType) else null
        val uri = uriStr?.toUri() ?: entity?.contentUri?.toUri()
        if (uri == null) {
            galleryRepository.updateOperationStatus(op.id, "FAILED", failureReason = "Media not found for edit")
            return AppResult.Error("Not found")
        }
        val action = payload["action"] ?: "EDIT"
        galleryRepository.updateOperationStatus(op.id, "IN_PROGRESS", "Edit requires user confirmation in Android system dialog. Open Remote Gallery to approve.")
        PendingMediaActionStore.set(EditAction(op.id, uri, mediaType, action, payload))
        notifyUserActionRequired("Edit approval required", "Approve media edit requested by administrator")
        return AppResult.Success(Unit)
    }

    private fun canAccessUri(uri: Uri): Boolean = try {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (_: Exception) { false }

    private fun buildPreviewInfo(uri: Uri, op: OperationView): String {
        return try {
            val type = context.contentResolver.getType(uri) ?: "unknown"
            if (type.startsWith("video")) {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, uri)
                val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                mmr.release()
                "Video accessible (mime=$type, durationMs=$dur, size=${w}x${h})."
            } else {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                "Image accessible (mime=$type, size=${opts.outWidth}x${opts.outHeight})."
            }
        } catch (e: Exception) {
            "Media accessible."
        }
    }

    private fun parsePayload(payload: String?): Map<String, String> {
        if (payload.isNullOrBlank()) return emptyMap()
        return try {
            val obj = json.parseToJsonElement(payload).jsonObject
            obj.mapValues { it.value.jsonPrimitive.content }
        } catch (_: Exception) { emptyMap() }
    }

    private fun notifyUserActionRequired(title: String, text: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "remote_actions"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(android.app.NotificationChannel(channelId, "Remote action approvals", android.app.NotificationManager.IMPORTANCE_HIGH))
            }
            val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            val pi = PendingIntent.getActivity(context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
            val n = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.zenox.remotegallery.R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .build()
            nm.notify(2002, n)
        } catch (_: Exception) {}
    }
}

sealed interface PendingMediaAction { val operationId: Long }
data class DeleteAction(override val operationId: Long, val uri: Uri) : PendingMediaAction
data class EditAction(override val operationId: Long, val uri: Uri, val mediaType: String, val action: String, val payload: Map<String, String>) : PendingMediaAction

object PendingMediaActionStore {
    @Volatile private var pending: PendingMediaAction? = null
    fun set(action: PendingMediaAction?) { pending = action }
    fun get(): PendingMediaAction? = pending
    fun consume(id: Long) { if (pending?.operationId == id) pending = null }
}

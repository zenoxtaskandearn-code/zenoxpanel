package com.zenox.remotegallery.data.repository

import com.zenox.remotegallery.core.AppResult
import com.zenox.remotegallery.data.local.db.MediaDao
import com.zenox.remotegallery.data.local.db.MediaItemEntity
import com.zenox.remotegallery.data.local.db.OperationDao
import com.zenox.remotegallery.data.local.db.PendingOperationEntity
import com.zenox.remotegallery.data.local.prefs.SettingsRepository
import com.zenox.remotegallery.data.remote.api.ApiClient
import com.zenox.remotegallery.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant

class GalleryRepository(
    private val apiClient: ApiClient,
    private val mediaDao: MediaDao,
    private val operationDao: OperationDao,
    private val settingsRepository: SettingsRepository
) {
    fun observeMedia(): Flow<List<MediaItemEntity>> = mediaDao.observeAll()
    fun observeImages(): Flow<List<MediaItemEntity>> = mediaDao.observeByType("IMAGE")
    fun observeVideos(): Flow<List<MediaItemEntity>> = mediaDao.observeByType("VIDEO")
    fun observeImageCount(): Flow<Int> = mediaDao.observeImageCount()
    fun observeVideoCount(): Flow<Int> = mediaDao.observeVideoCount()
    fun observeActiveOperations(): Flow<List<PendingOperationEntity>> = operationDao.observeActive()

    suspend fun syncDirtyMetadata(batchSize: Int = 100): AppResult<Int> = withContext(Dispatchers.IO) {
        try {
            val dirty = mediaDao.getDirty(batchSize)
            if (dirty.isEmpty()) return@withContext AppResult.Success(0)
            val items = dirty.map { it.toSyncItem() }
            val res = apiClient.deviceApi().syncMedia(DeviceMediaSyncRequest(items))
            if (res.isSuccessful) {
                mediaDao.markClean(dirty.map { it.id })
                settingsRepository.setLastSync(System.currentTimeMillis())
                AppResult.Success(items.size)
            } else {
                AppResult.Error("Sync failed (${res.code()})", res.code())
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Sync failed", cause = e)
        }
    }

    suspend fun fetchPendingOperations(): AppResult<List<OperationView>> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.deviceApi().pendingOperations()
            if (res.isSuccessful) {
                val ops = res.body().orEmpty()
                ops.forEach { operationDao.upsert(it.toEntity()) }
                AppResult.Success(ops)
            } else AppResult.Error("Failed to fetch operations (${res.code()})", res.code())
        } catch (e: Exception) { AppResult.Error(e.message ?: "Network error", cause = e) }
    }

    suspend fun ackOperation(id: Long, message: String? = null): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.deviceApi().ackOperation(id, DeviceAckRequest(message))
            if (res.isSuccessful) { res.body()?.let { operationDao.upsert(it.toEntity()) }; AppResult.Success(Unit) }
            else AppResult.Error("Ack failed (${res.code()})", res.code())
        } catch (e: Exception) { AppResult.Error(e.message ?: "Ack failed", cause = e) }
    }

    suspend fun updateOperationStatus(id: Long, status: String, message: String? = null, failureReason: String? = null): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.deviceApi().updateOperationStatus(id, DeviceStatusUpdateRequest(status, message, failureReason))
            if (res.isSuccessful) { res.body()?.let { operationDao.upsert(it.toEntity()) }; AppResult.Success(Unit) }
            else AppResult.Error("Status update failed (${res.code()})", res.code())
        } catch (e: Exception) { AppResult.Error(e.message ?: "Status update failed", cause = e) }
    }

    private fun MediaItemEntity.toSyncItem(): MediaSyncItem = MediaSyncItem(
        externalId = "${mediaType}:${mediaStoreId}",
        mediaType = mediaType,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        durationMs = durationMs,
        capturedAt = dateAddedSec?.let { Instant.ofEpochSecond(it).toString() } ?: dateModifiedSec?.let { Instant.ofEpochSecond(it).toString() },
        thumbnailAvailable = !deletedOnDevice,
        streamAvailable = !deletedOnDevice,
        thumbnailRef = contentUri,
        streamRef = contentUri,
        checksum = null,
        deletedOnDevice = deletedOnDevice
    )

    private fun OperationView.toEntity(): PendingOperationEntity = PendingOperationEntity(
        remoteId = id,
        deviceId = deviceId,
        mediaBackendId = mediaId,
        operationType = operationType,
        payload = payload,
        status = status,
        requestedAt = requestedAt,
        expiresAt = expiresAt,
        requiresUserAction = operationType in setOf("EDIT", "DELETE"),
        localMessage = deviceAckMessage,
        updatedAt = System.currentTimeMillis()
    )
}

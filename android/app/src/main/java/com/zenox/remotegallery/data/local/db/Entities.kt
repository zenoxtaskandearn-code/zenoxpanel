package com.zenox.remotegallery.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [Index(value = ["mediaStoreId", "mediaType"], unique = true), Index("deletedOnDevice"), Index("dateModifiedSec")]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreId: Long,
    val mediaType: String, // IMAGE or VIDEO
    val fileName: String?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val dateAddedSec: Long?,
    val dateModifiedSec: Long?,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    val contentUri: String,
    val deletedOnDevice: Boolean = false,
    val lastIndexedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = true
)

@Entity(tableName = "operation_logs")
data class OperationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteOperationId: Long?,
    val operationType: String,
    val mediaStoreId: Long?,
    val status: String,
    val message: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pending_operations", indices = [Index(value = ["remoteId"], unique = true)])
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: Long,
    val deviceId: Long,
    val mediaBackendId: Long?,
    val operationType: String,
    val payload: String?,
    val status: String,
    val requestedAt: String?,
    val expiresAt: String?,
    val requiresUserAction: Boolean = false,
    val localMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

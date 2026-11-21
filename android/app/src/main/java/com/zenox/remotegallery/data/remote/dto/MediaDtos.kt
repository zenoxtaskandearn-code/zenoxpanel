package com.zenox.remotegallery.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MediaSyncItem(
    val externalId: String,
    val mediaType: String,
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val capturedAt: String? = null,
    val thumbnailAvailable: Boolean? = null,
    val streamAvailable: Boolean? = null,
    val thumbnailRef: String? = null,
    val streamRef: String? = null,
    val checksum: String? = null,
    val deletedOnDevice: Boolean? = null
)

@Serializable
data class DeviceMediaSyncRequest(val items: List<MediaSyncItem>)

@Serializable
data class OperationView(
    val id: Long,
    val deviceId: Long,
    val adminId: Long? = null,
    val mediaId: Long? = null,
    val operationType: String,
    val payload: String? = null,
    val status: String,
    val failureReason: String? = null,
    val requestedAt: String? = null,
    val expiresAt: String? = null,
    val acknowledgedAt: String? = null,
    val completedAt: String? = null,
    val deviceAckMessage: String? = null
)

@Serializable
data class DeviceAckRequest(val message: String? = null, val status: String? = null)

@Serializable
data class DeviceStatusUpdateRequest(
    val status: String,
    val message: String? = null,
    val failureReason: String? = null
)

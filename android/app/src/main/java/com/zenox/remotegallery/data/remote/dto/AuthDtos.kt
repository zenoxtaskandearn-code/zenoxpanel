package com.zenox.remotegallery.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegisterRequest(
    val deviceUuid: String,
    val name: String,
    val deviceType: String? = "ANDROID",
    val appVersion: String? = null,
    val deviceSecret: String? = null
)

@Serializable
data class DeviceAuthRequest(
    val deviceUuid: String,
    val deviceSecret: String
)

@Serializable
data class DeviceStatusRequest(val status: String)

@Serializable
data class HeartbeatRequest(val appVersion: String? = null)

@Serializable
data class PermissionView(
    val canView: Boolean = true,
    val canStream: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val canSyncMetadata: Boolean = true
)

@Serializable
data class ConsentView(
    val id: Long? = null,
    val granted: Boolean = false,
    val scope: String? = null,
    val grantedAt: String? = null,
    val revokedAt: String? = null,
    val expiresAt: String? = null
)

@Serializable
data class DeviceView(
    val id: Long,
    val deviceUuid: String,
    val name: String,
    val deviceType: String? = null,
    val status: String? = null,
    val revoked: Boolean = false,
    val lastSeenAt: String? = null,
    val lastKnownIp: String? = null,
    val appVersion: String? = null,
    val createdAt: String? = null,
    val permissions: PermissionView? = null,
    val latestConsent: ConsentView? = null
)

@Serializable
data class DeviceAuthResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 3600,
    val device: DeviceView
)

@Serializable
data class ApiErrorResponse(
    val timestamp: String? = null,
    val status: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null
)

package com.zenox.remotegallery.data.repository

import com.zenox.remotegallery.core.AppResult
import com.zenox.remotegallery.data.local.prefs.SettingsRepository
import com.zenox.remotegallery.data.local.prefs.TokenStore
import com.zenox.remotegallery.data.remote.api.ApiClient
import com.zenox.remotegallery.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.util.UUID

class DeviceRepository(
    private val apiClient: ApiClient,
    private val tokenStore: TokenStore,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun register(ownerEmail: String, deviceName: String, existingUuid: String?, existingSecret: String?): AppResult<DeviceAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val uuid = existingUuid?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            val secret = existingSecret?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString().replace("-", "")
            val req = DeviceRegisterRequest(
                deviceUuid = uuid,
                name = deviceName,
                deviceType = "ANDROID",
                appVersion = com.zenox.remotegallery.BuildConfig.VERSION_NAME,
                deviceSecret = secret
            )
            val res = apiClient.authApi().register(req, ownerEmail.ifBlank { null })
            handleAuthResponse(res, fallbackUuid = uuid, fallbackSecret = secret, ownerEmail = ownerEmail, deviceName = deviceName)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Registration failed", cause = e)
        }
    }

    suspend fun authenticate(deviceUuid: String, deviceSecret: String): AppResult<DeviceAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val req = DeviceAuthRequest(deviceUuid, deviceSecret)
            val res = apiClient.authApi().auth(req)
            handleAuthResponse(res, fallbackUuid = deviceUuid, fallbackSecret = deviceSecret, ownerEmail = null, deviceName = null)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Authentication failed", cause = e)
        }
    }

    private suspend fun handleAuthResponse(res: Response<DeviceAuthResponse>, fallbackUuid: String, fallbackSecret: String, ownerEmail: String?, deviceName: String?): AppResult<DeviceAuthResponse> {
        return if (res.isSuccessful) {
            val body = res.body() ?: return AppResult.Error("Empty response")
            tokenStore.save(body.accessToken, body.refreshToken, fallbackSecret)
            val prefs = settingsRepository.rawFlow.first()
            val resolvedOwner = ownerEmail?.takeIf { it.isNotBlank() } ?: prefs[SettingsRepository.OWNER_EMAIL] ?: ""
            val resolvedName = body.device.name.ifBlank { deviceName?.takeIf { it.isNotBlank() } ?: prefs[SettingsRepository.DEVICE_NAME] ?: "Android Device" }
            settingsRepository.setRegistered(
                deviceUuid = body.device.deviceUuid.ifBlank { fallbackUuid },
                deviceDbId = body.device.id,
                deviceName = resolvedName,
                ownerEmail = resolvedOwner
            )
            AppResult.Success(body)
        } else {
            val msg = parseError(res)
            AppResult.Error(msg, res.code())
        }
    }

    suspend fun heartbeat(): AppResult<Unit> = withContext(Dispatchers.IO) { safeCall { apiClient.deviceApi().heartbeat(HeartbeatRequest(com.zenox.remotegallery.BuildConfig.VERSION_NAME)) } }
    suspend fun updateStatus(status: String): AppResult<Unit> = withContext(Dispatchers.IO) { safeCall { apiClient.deviceApi().status(DeviceStatusRequest(status)) } }

    suspend fun logoutLocal() {
        tokenStore.clear()
        settingsRepository.clearRegistration()
    }

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): AppResult<Unit> = try {
        val res = call()
        if (res.isSuccessful) AppResult.Success(Unit) else AppResult.Error(parseError(res), res.code())
    } catch (e: Exception) { AppResult.Error(e.message ?: "Network error", cause = e) }

    private fun <T> parseError(res: Response<T>): String {
        val raw = try { res.errorBody()?.string() } catch (_: Exception) { null }
        if (!raw.isNullOrBlank()) {
            try { val e = json.decodeFromString(ApiErrorResponse.serializer(), raw); if (!e.message.isNullOrBlank()) return e.message } catch (_: Exception) {}
            return raw.take(200)
        }
        return "Request failed (${res.code()})"
    }
}



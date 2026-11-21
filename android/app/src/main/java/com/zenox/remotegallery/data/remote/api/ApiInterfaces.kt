package com.zenox.remotegallery.data.remote.api

import com.zenox.remotegallery.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface DeviceAuthApi {
    @POST("api/device/register")
    suspend fun register(
        @Body request: DeviceRegisterRequest,
        @Header("X-Owner-Email") ownerEmail: String? = null
    ): Response<DeviceAuthResponse>

    @POST("api/device/auth")
    suspend fun auth(@Body request: DeviceAuthRequest): Response<DeviceAuthResponse>

    @POST("api/device/login")
    suspend fun login(@Body request: DeviceAuthRequest): Response<DeviceAuthResponse>
}

interface DeviceApi {
    @POST("api/device/heartbeat")
    suspend fun heartbeat(@Body request: HeartbeatRequest = HeartbeatRequest()): Response<Map<String, String>>

    @POST("api/device/status")
    suspend fun status(@Body request: DeviceStatusRequest): Response<Map<String, String>>

    @POST("api/device/media/sync")
    suspend fun syncMedia(@Body request: DeviceMediaSyncRequest): Response<Map<String, String>>

    @GET("api/device/operations/pending")
    suspend fun pendingOperations(): Response<List<OperationView>>

    @POST("api/device/operations/{id}/ack")
    suspend fun ackOperation(@Path("id") id: Long, @Body body: DeviceAckRequest = DeviceAckRequest()): Response<OperationView>

    @POST("api/device/operations/{id}/status")
    suspend fun updateOperationStatus(@Path("id") id: Long, @Body body: DeviceStatusUpdateRequest): Response<OperationView>
}

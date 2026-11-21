package com.zenox.remotegallery.data.remote.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.zenox.remotegallery.core.ServerConfig
import com.zenox.remotegallery.data.local.prefs.SettingsRepository
import com.zenox.remotegallery.data.local.prefs.TokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiClient(
    private val tokenStore: TokenStore,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    private fun baseUrl(): String {
        val override = runBlocking { settingsRepository.settingsFlow.first().serverUrlOverride }
        val raw = (override?.takeIf { it.isNotBlank() } ?: ServerConfig.baseUrl).trim()
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("Authorization")
    }

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(logging)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun retrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl())
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    fun authApi(): DeviceAuthApi = retrofit().create(DeviceAuthApi::class.java)
    fun deviceApi(): DeviceApi = retrofit().create(DeviceApi::class.java)

    fun currentWsUrl(): String {
        val base = baseUrl().removeSuffix("/")
        return when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://") + "/ws"
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://") + "/ws"
            else -> ServerConfig.wsUrl
        }
    }
}

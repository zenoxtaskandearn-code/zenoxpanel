package com.zenox.remotegallery.data.remote.ws

import android.util.Log
import com.zenox.remotegallery.data.local.prefs.SettingsRepository
import com.zenox.remotegallery.data.local.prefs.TokenStore
import com.zenox.remotegallery.data.remote.api.ApiClient
import com.zenox.remotegallery.data.repository.GalleryRepository
import com.zenox.remotegallery.data.repository.OperationProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class WebSocketManager(
    private val apiClient: ApiClient,
    private val tokenStore: TokenStore,
    private val settingsRepository: SettingsRepository,
    private val operationProcessor: OperationProcessor,
    private val galleryRepository: GalleryRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var shouldRun = false
    private val subId = AtomicInteger(0)
    private val _state = MutableStateFlow("DISCONNECTED")
    val state: StateFlow<String> = _state

    fun connect() {
        shouldRun = true
        if (_state.value == "CONNECTING" || _state.value == "CONNECTED") return
        open()
    }

    fun disconnect() {
        shouldRun = false
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        try { webSocket?.close(1000, "Client disconnect") } catch (_: Exception) {}
        webSocket = null
        _state.value = "DISCONNECTED"
    }

    private fun open() {
        val token = tokenStore.getAccessToken() ?: run { _state.value = "DISCONNECTED"; return }
        _state.value = "CONNECTING"
        val url = apiClient.currentWsUrl()
        val req = Request.Builder().url(url).build()
        webSocket = apiClient.okHttp.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val connectFrame = buildFrame("CONNECT", mapOf("accept-version" to "1.2", "heart-beat" to "10000,10000", "Authorization" to "Bearer $token"), "")
                webSocket.send(connectFrame)
            }
            override fun onMessage(webSocket: WebSocket, text: String) { handleFrame(text) }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { handleFrame(bytes.utf8()) }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { try { webSocket.close(code, reason) } catch (_: Exception) {} }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { onLost() }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { Log.w("WS", "failure: ${t.message}"); onLost() }
        })
    }

    private fun onLost() {
        heartbeatJob?.cancel()
        webSocket = null
        _state.value = "DISCONNECTED"
        if (shouldRun) scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(5000)
            if (shouldRun) open()
        }
    }

    private fun handleFrame(frame: String) {
        val normalized = frame.replace("\r\n", "\n")
        val parts = normalized.split("\n\n", limit = 2)
        val headerPart = parts.getOrNull(0) ?: return
        val body = parts.getOrNull(1)?.trimEnd('\u0000') ?: ""
        val lines = headerPart.split("\n")
        val command = lines.firstOrNull()?.trim().orEmpty()
        when (command) {
            "CONNECTED" -> {
                _state.value = "CONNECTED"
                scope.launch { subscribeDefaults() }
                startHeartbeat()
            }
            "MESSAGE" -> {
                scope.launch {
                    // Any relevant event -> refresh pending operations / sync status
                    try { galleryRepository.fetchPendingOperations() } catch (_: Exception) {}
                    try { operationProcessor.processPendingOperations() } catch (_: Exception) {}
                }
            }
            "ERROR" -> {
                Log.w("WS", "STOMP error: $body")
            }
        }
    }

    private suspend fun subscribeDefaults() {
        val prefs = settingsRepository.rawFlow.first()
        val deviceDbId = prefs[SettingsRepository.DEVICE_DB_ID]
        val ws = webSocket ?: return
        // generic topics
        sendSubscribe(ws, "/topic/operations")
        sendSubscribe(ws, "/topic/gallery")
        if (deviceDbId != null && deviceDbId > 0) {
            sendSubscribe(ws, "/topic/devices/$deviceDbId/operations")
            sendSubscribe(ws, "/topic/devices/$deviceDbId/gallery")
            sendSubscribe(ws, "/topic/devices/$deviceDbId")
        }
    }

    private fun sendSubscribe(ws: WebSocket, destination: String) {
        val id = "sub-${subId.incrementAndGet()}"
        ws.send(buildFrame("SUBSCRIBE", mapOf("id" to id, "destination" to destination), ""))
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _state.value == "CONNECTED") {
                try { webSocket?.send("\n") } catch (_: Exception) {}
                delay(10000)
            }
        }
    }

    private fun buildFrame(command: String, headers: Map<String, String>, body: String): String {
        val sb = StringBuilder()
        sb.append(command).append('\n')
        headers.forEach { (k, v) -> sb.append(k).append(':').append(v).append('\n') }
        sb.append('\n').append(body).append('\u0000')
        return sb.toString()
    }
}

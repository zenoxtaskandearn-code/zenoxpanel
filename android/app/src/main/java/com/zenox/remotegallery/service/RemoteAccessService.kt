package com.zenox.remotegallery.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zenox.remotegallery.MainActivity
import com.zenox.remotegallery.R
import com.zenox.remotegallery.RemoteGalleryApp
import kotlinx.coroutines.*

class RemoteAccessService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pollJob = null
    companion object {
        const val CHANNEL_ID = "remote_access_active"
        const val NOTIF_ID = 1001
        fun start(context: Context) {
            val i = Intent(context, RemoteAccessService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i) else context.startService(i)
        }
        fun stop(context: Context) { context.stopService(Intent(context, RemoteAccessService::class.java)) }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification("Remote access enabled", "Device is available for authorized gallery operations"))
        val app = application as RemoteGalleryApp
        app.container.webSocketManager.connect()
        scope.launch {
            while (isActive) {
                try { app.container.operationProcessor.processPendingOperations() } catch (_: Exception) {}
                try { app.container.deviceRepository.heartbeat() } catch (_: Exception) {}
                delay(30_000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        try { (application as RemoteGalleryApp).container.webSocketManager.disconnect() } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Remote access", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown when remote gallery access is enabled"
            })
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}

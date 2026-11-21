package com.zenox.remotegallery.worker

import android.content.Context
import androidx.work.*
import com.zenox.remotegallery.RemoteGalleryApp
import com.zenox.remotegallery.core.AppResult
import com.zenox.remotegallery.util.PermissionUtils
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as RemoteGalleryApp
        val settings = app.container.settingsRepository.settingsFlow.first()
        if (!settings.registered) return Result.success()
        if (!PermissionUtils.hasAnyMediaAccess(applicationContext)) return Result.success()
        return try {
            if (settings.remoteAccessConsentGranted && settings.remoteAccessEnabled) {
                try { app.container.deviceRepository.updateStatus("ONLINE") } catch (_: Exception) {}
            }
            app.container.mediaIndexRepository.indexAllAccessibleMedia()
            if (settings.registered) {
                when (val r = app.container.galleryRepository.syncDirtyMetadata(300)) {
                    is AppResult.Error -> return Result.retry()
                    else -> {}
                }
            }
            if (settings.remoteAccessConsentGranted && settings.remoteAccessEnabled) {
                app.container.operationProcessor.processPendingOperations()
            }
            try { app.container.deviceRepository.heartbeat() } catch (_: Exception) {}
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

class OperationPollWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as RemoteGalleryApp
        val settings = app.container.settingsRepository.settingsFlow.first()
        if (!settings.registered || !settings.remoteAccessConsentGranted || !settings.remoteAccessEnabled) return Result.success()
        return try {
            app.container.operationProcessor.processPendingOperations()
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}

object WorkScheduler {
    private const val PERIODIC_SYNC = "periodic_sync"
    private const val PERIODIC_OPS = "periodic_ops"
    fun ensurePeriodicWork(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val sync = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        val ops = PeriodicWorkRequestBuilder<OperationPollWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_SYNC, ExistingPeriodicWorkPolicy.KEEP, sync)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_OPS, ExistingPeriodicWorkPolicy.KEEP, ops)
    }
    fun enqueueImmediateSync(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(context).enqueue(req)
    }
    fun startRemoteAccessService(context: Context) {
        com.zenox.remotegallery.service.RemoteAccessService.start(context)
    }
    fun stopRemoteAccessService(context: Context) {
        com.zenox.remotegallery.service.RemoteAccessService.stop(context)
    }
}

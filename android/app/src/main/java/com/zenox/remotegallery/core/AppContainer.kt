package com.zenox.remotegallery.core

import android.content.Context
import androidx.room.Room
import com.zenox.remotegallery.data.local.db.AppDatabase
import com.zenox.remotegallery.data.local.prefs.SettingsRepository
import com.zenox.remotegallery.data.local.prefs.TokenStore
import com.zenox.remotegallery.data.remote.api.ApiClient
import com.zenox.remotegallery.data.remote.ws.WebSocketManager
import com.zenox.remotegallery.data.repository.DeviceRepository
import com.zenox.remotegallery.data.repository.GalleryRepository
import com.zenox.remotegallery.data.repository.MediaIndexRepository
import com.zenox.remotegallery.data.repository.OperationProcessor

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val settingsRepository = SettingsRepository(appContext)
    val tokenStore = TokenStore(appContext)
    val database: AppDatabase = Room.databaseBuilder(appContext, AppDatabase::class.java, "remote_gallery.db")
        .fallbackToDestructiveMigration()
        .build()

    val apiClient = ApiClient(tokenStore, settingsRepository)
    val deviceRepository = DeviceRepository(apiClient, tokenStore, settingsRepository)
    val galleryRepository = GalleryRepository(apiClient, database.mediaDao(), database.operationDao(), settingsRepository)
    val mediaIndexRepository = MediaIndexRepository(appContext, database.mediaDao())
    val operationProcessor = OperationProcessor(appContext, galleryRepository, database.mediaDao(), settingsRepository)
    val webSocketManager = WebSocketManager(apiClient, tokenStore, settingsRepository, operationProcessor, galleryRepository)
}

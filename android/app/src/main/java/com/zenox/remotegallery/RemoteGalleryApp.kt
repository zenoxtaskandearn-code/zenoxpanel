package com.zenox.remotegallery

import android.app.Application
import androidx.work.Configuration
import com.zenox.remotegallery.core.AppContainer

class RemoteGalleryApp : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}

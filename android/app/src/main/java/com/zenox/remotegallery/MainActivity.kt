package com.zenox.remotegallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenox.remotegallery.ui.RemoteGalleryRoot
import com.zenox.remotegallery.ui.theme.RemoteGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RemoteGalleryApp
        setContent {
            val settings by app.container.settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            RemoteGalleryTheme(themeMode = settings?.themeMode ?: "system") {
                RemoteGalleryRoot(appContainer = app.container)
            }
        }
    }
}

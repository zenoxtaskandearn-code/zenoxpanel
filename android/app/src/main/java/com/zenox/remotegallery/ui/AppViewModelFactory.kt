package com.zenox.remotegallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenox.remotegallery.core.AppContainer

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> OnboardingViewModel(container) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(container) as T
            modelClass.isAssignableFrom(GalleryViewModel::class.java) -> GalleryViewModel(container) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container) as T
            else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
        }
    }
}

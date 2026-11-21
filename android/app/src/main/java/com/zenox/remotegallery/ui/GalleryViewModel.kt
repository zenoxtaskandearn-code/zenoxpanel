package com.zenox.remotegallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenox.remotegallery.core.AppContainer
import com.zenox.remotegallery.data.local.db.MediaItemEntity
import kotlinx.coroutines.flow.*

class GalleryViewModel(private val container: AppContainer) : ViewModel() {
    val media = container.galleryRepository.observeMedia().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val images = container.galleryRepository.observeImages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val videos = container.galleryRepository.observeVideos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _filter = MutableStateFlow("ALL") // ALL, IMAGE, VIDEO
    val filter = _filter.asStateFlow()

    fun setQuery(q: String) { _query.value = q }
    fun setFilter(f: String) { _filter.value = f }
}

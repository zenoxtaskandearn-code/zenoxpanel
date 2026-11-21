package com.zenox.remotegallery.util

object PermissionLogic {
    fun summarize(hasFull: Boolean, hasAny: Boolean, selectedOnly: Boolean): String = when {
        hasFull -> "Full photo and video access granted"
        selectedOnly -> "Limited access: only selected photos/videos are accessible"
        hasAny -> "Partial media access granted"
        else -> "No media access granted"
    }
}

package com.zenox.remotegallery.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {
    fun requiredMediaPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasAnyMediaAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            has(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                has(context, Manifest.permission.READ_MEDIA_VIDEO) ||
                has(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= 33) {
            has(context, Manifest.permission.READ_MEDIA_IMAGES) || has(context, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            has(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun hasFullMediaAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            has(context, Manifest.permission.READ_MEDIA_IMAGES) && has(context, Manifest.permission.READ_MEDIA_VIDEO)
        } else has(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun isSelectedAccessOnly(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= 34 &&
            has(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
            !has(context, Manifest.permission.READ_MEDIA_IMAGES) &&
            !has(context, Manifest.permission.READ_MEDIA_VIDEO)
    }

    fun has(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun permissionSummary(context: Context): String = when {
        hasFullMediaAccess(context) -> "Full photo and video access granted"
        isSelectedAccessOnly(context) -> "Limited access: only selected photos/videos are accessible"
        hasAnyMediaAccess(context) -> "Partial media access granted"
        else -> "No media access granted"
    }
}

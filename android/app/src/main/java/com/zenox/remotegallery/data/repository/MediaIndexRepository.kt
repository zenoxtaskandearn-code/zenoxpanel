package com.zenox.remotegallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.zenox.remotegallery.data.local.db.MediaDao
import com.zenox.remotegallery.data.local.db.MediaItemEntity
import com.zenox.remotegallery.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaIndexRepository(private val context: Context, private val mediaDao: MediaDao) {
    suspend fun indexAllAccessibleMedia(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!PermissionUtils.hasAnyMediaAccess(context)) return@withContext Result.success(0)
            val images = queryImages()
            val videos = queryVideos()
            val all = images + videos
            val existing = mediaDao.getAll()
            val existingKeys = existing.associateBy { it.mediaType to it.mediaStoreId }
            val seen = mutableSetOf<Pair<String, Long>>()
            val toUpsert = mutableListOf<MediaItemEntity>()
            for (item in all) {
                val key = item.mediaType to item.mediaStoreId
                seen.add(key)
                val prev = existingKeys[key]
                val changed = prev == null ||
                    prev.fileName != item.fileName ||
                    prev.sizeBytes != item.sizeBytes ||
                    prev.dateModifiedSec != item.dateModifiedSec ||
                    prev.mimeType != item.mimeType ||
                    prev.width != item.width ||
                    prev.height != item.height ||
                    prev.durationMs != item.durationMs ||
                    prev.deletedOnDevice
                if (changed) {
                    toUpsert.add(item.copy(id = prev?.id ?: 0, dirty = true, deletedOnDevice = false))
                }
            }
            if (toUpsert.isNotEmpty()) mediaDao.upsertAll(toUpsert)

            val deletedImages = existing.filter { it.mediaType == "IMAGE" && (it.mediaType to it.mediaStoreId) !in seen }.map { it.mediaStoreId }
            val deletedVideos = existing.filter { it.mediaType == "VIDEO" && (it.mediaType to it.mediaStoreId) !in seen }.map { it.mediaStoreId }
            if (deletedImages.isNotEmpty()) mediaDao.markDeleted(deletedImages, "IMAGE")
            if (deletedVideos.isNotEmpty()) mediaDao.markDeleted(deletedVideos, "VIDEO")
            Result.success(toUpsert.size + deletedImages.size + deletedVideos.size)
        } catch (se: SecurityException) {
            Result.failure(se)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryImages(): List<MediaItemEntity> = queryMedia(
        collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        mediaType = "IMAGE",
        idCol = MediaStore.Images.Media._ID,
        nameCol = MediaStore.Images.Media.DISPLAY_NAME,
        mimeCol = MediaStore.Images.Media.MIME_TYPE,
        sizeCol = MediaStore.Images.Media.SIZE,
        dateAddedCol = MediaStore.Images.Media.DATE_ADDED,
        dateModCol = MediaStore.Images.Media.DATE_MODIFIED,
        widthCol = MediaStore.Images.Media.WIDTH,
        heightCol = MediaStore.Images.Media.HEIGHT,
        durationCol = null
    )

    private fun queryVideos(): List<MediaItemEntity> = queryMedia(
        collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        mediaType = "VIDEO",
        idCol = MediaStore.Video.Media._ID,
        nameCol = MediaStore.Video.Media.DISPLAY_NAME,
        mimeCol = MediaStore.Video.Media.MIME_TYPE,
        sizeCol = MediaStore.Video.Media.SIZE,
        dateAddedCol = MediaStore.Video.Media.DATE_ADDED,
        dateModCol = MediaStore.Video.Media.DATE_MODIFIED,
        widthCol = MediaStore.Video.Media.WIDTH,
        heightCol = MediaStore.Video.Media.HEIGHT,
        durationCol = MediaStore.Video.Media.DURATION
    )

    private fun queryMedia(
        collection: android.net.Uri,
        mediaType: String,
        idCol: String,
        nameCol: String,
        mimeCol: String,
        sizeCol: String,
        dateAddedCol: String,
        dateModCol: String,
        widthCol: String,
        heightCol: String,
        durationCol: String?
    ): List<MediaItemEntity> {
        val projection = mutableListOf(idCol, nameCol, mimeCol, sizeCol, dateAddedCol, dateModCol, widthCol, heightCol)
        if (durationCol != null) projection.add(durationCol)
        val items = mutableListOf<MediaItemEntity>()
        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            null,
            null,
            "$dateModCol DESC"
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(idCol)
            val nameIdx = c.getColumnIndex(nameCol)
            val mimeIdx = c.getColumnIndex(mimeCol)
            val sizeIdx = c.getColumnIndex(sizeCol)
            val addedIdx = c.getColumnIndex(dateAddedCol)
            val modIdx = c.getColumnIndex(dateModCol)
            val wIdx = c.getColumnIndex(widthCol)
            val hIdx = c.getColumnIndex(heightCol)
            val dIdx = durationCol?.let { c.getColumnIndex(it) } ?: -1
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val uri = ContentUris.withAppendedId(collection, id)
                items.add(
                    MediaItemEntity(
                        mediaStoreId = id,
                        mediaType = mediaType,
                        fileName = if (nameIdx >= 0) c.getString(nameIdx) else null,
                        mimeType = if (mimeIdx >= 0) c.getString(mimeIdx) else null,
                        sizeBytes = if (sizeIdx >= 0) c.getLong(sizeIdx) else null,
                        dateAddedSec = if (addedIdx >= 0) c.getLong(addedIdx) else null,
                        dateModifiedSec = if (modIdx >= 0) c.getLong(modIdx) else null,
                        width = if (wIdx >= 0) c.getInt(wIdx) else null,
                        height = if (hIdx >= 0) c.getInt(hIdx) else null,
                        durationMs = if (dIdx >= 0) c.getLong(dIdx) else null,
                        contentUri = uri.toString(),
                        deletedOnDevice = false,
                        dirty = true
                    )
                )
            }
        }
        return items
    }
}

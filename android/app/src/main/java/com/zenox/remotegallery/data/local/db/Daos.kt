package com.zenox.remotegallery.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE deletedOnDevice = 0 ORDER BY dateModifiedSec DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE deletedOnDevice = 0 AND mediaType = :type ORDER BY dateModifiedSec DESC")
    fun observeByType(type: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items")
    suspend fun getAll(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE mediaStoreId = :id AND mediaType = :type LIMIT 1")
    suspend fun findByMediaStoreId(id: Long, type: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaItemEntity>)

    @Query("UPDATE media_items SET deletedOnDevice = 1, dirty = 1 WHERE mediaStoreId IN (:ids) AND mediaType = :type")
    suspend fun markDeleted(ids: List<Long>, type: String)

    @Query("SELECT COUNT(*) FROM media_items WHERE deletedOnDevice = 0 AND mediaType = 'IMAGE'")
    fun observeImageCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE deletedOnDevice = 0 AND mediaType = 'VIDEO'")
    fun observeVideoCount(): Flow<Int>

    @Query("SELECT * FROM media_items WHERE dirty = 1 LIMIT :limit")
    suspend fun getDirty(limit: Int): List<MediaItemEntity>

    @Query("UPDATE media_items SET dirty = 0 WHERE id IN (:ids)")
    suspend fun markClean(ids: List<Long>)

    @Query("DELETE FROM media_items")
    suspend fun clear()
}

@Dao
interface OperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(op: PendingOperationEntity)

    @Query("SELECT * FROM pending_operations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PendingOperationEntity>>

    @Query("SELECT * FROM pending_operations WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: Long): PendingOperationEntity?

    @Query("SELECT * FROM pending_operations WHERE status IN ('PENDING','ACKNOWLEDGED','IN_PROGRESS') ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<PendingOperationEntity>>

    @Insert
    suspend fun addLog(log: OperationLogEntity)

    @Query("SELECT * FROM operation_logs ORDER BY createdAt DESC LIMIT 100")
    fun observeLogs(): Flow<List<OperationLogEntity>>

    @Query("DELETE FROM pending_operations")
    suspend fun clearOps()
}

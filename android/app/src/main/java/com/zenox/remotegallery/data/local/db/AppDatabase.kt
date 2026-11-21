package com.zenox.remotegallery.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaItemEntity::class, PendingOperationEntity::class, OperationLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun operationDao(): OperationDao
}

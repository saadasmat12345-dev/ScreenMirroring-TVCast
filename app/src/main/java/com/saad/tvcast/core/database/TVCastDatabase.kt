package com.saad.tvcast.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DeviceEntity::class,
        CastingHistoryEntity::class,
        MediaFavoriteEntity::class,
        BrowserHistoryEntity::class,
        BookmarkEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TVCastDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun castingHistoryDao(): CastingHistoryDao
    abstract fun mediaFavoriteDao(): MediaFavoriteDao
    abstract fun browserDao(): BrowserDao
}

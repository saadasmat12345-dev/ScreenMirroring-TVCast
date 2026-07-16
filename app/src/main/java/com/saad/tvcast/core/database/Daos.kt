package com.saad.tvcast.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastConnectedAt DESC, lastSeenAt DESC")
    fun observeRecentDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE isFavorite = 1 ORDER BY name")
    fun observeFavoriteDevices(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: DeviceEntity)

    @Query("UPDATE devices SET isFavorite = :favorite WHERE id = :deviceId")
    suspend fun setFavorite(deviceId: String, favorite: Boolean)

    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun remove(deviceId: String)
}

@Dao
interface CastingHistoryDao {
    @Query("SELECT * FROM casting_history ORDER BY playedAt DESC")
    fun observeHistory(): Flow<List<CastingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CastingHistoryEntity)

    @Delete
    suspend fun delete(item: CastingHistoryEntity)

    @Query("DELETE FROM casting_history")
    suspend fun clear()
}

@Dao
interface MediaFavoriteDao {
    @Query("SELECT * FROM media_favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<MediaFavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM media_favorites WHERE mediaUri = :uri)")
    suspend fun isFavorite(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaFavoriteEntity)

    @Query("DELETE FROM media_favorites WHERE mediaUri = :uri")
    suspend fun remove(uri: String)
}

@Dao
interface BrowserDao {
    @Query("SELECT * FROM browser_history ORDER BY visitedAt DESC LIMIT :limit")
    fun observeHistory(limit: Int = 100): Flow<List<BrowserHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(item: BrowserHistoryEntity)

    @Query("DELETE FROM browser_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM bookmarks ORDER BY title")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(item: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun removeBookmark(url: String)
}

package com.saad.tvcast.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val protocol: String,
    val ipAddress: String?,
    val descriptorUrl: String?,
    val controlUrl: String?,
    val isFavorite: Boolean,
    val isDemo: Boolean,
    val lastSeenAt: Long,
    val lastConnectedAt: Long?
)

@Entity(tableName = "casting_history")
data class CastingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaTitle: String,
    val mediaUri: String,
    val mediaKind: String,
    val deviceId: String?,
    val deviceName: String?,
    val status: String,
    val playedAt: Long
)

@Entity(tableName = "media_favorites")
data class MediaFavoriteEntity(
    @PrimaryKey val mediaUri: String,
    val title: String,
    val mediaKind: String,
    val addedAt: Long
)

@Entity(tableName = "browser_history")
data class BrowserHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String?,
    val url: String,
    val visitedAt: Long
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val url: String,
    val title: String,
    val addedAt: Long
)

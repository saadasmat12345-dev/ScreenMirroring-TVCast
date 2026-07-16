package com.saad.tvcast.core.common

import android.net.Uri

enum class DeviceProtocol { Dlna, Upnp, AndroidMediaRoute, Demo }
enum class DeviceType { Tv, Speaker, Receiver, Browser, Unknown }
enum class ConnectionStatus { Disconnected, Scanning, Connecting, Connected, Lost, Error }
enum class MediaKind { Video, Photo, Music, WebVideo }
enum class MediaSort { Name, Date, Size, Duration }
enum class CastQuality { Auto, High, Medium, Low }
enum class ThemeMode { System, Light, Dark }

data class CastDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val protocol: DeviceProtocol,
    val ipAddress: String?,
    val descriptorUrl: String?,
    val controlUrl: String?,
    val isDemo: Boolean = false,
    val lastSeenAt: Long = System.currentTimeMillis()
)

data class CastConnection(
    val status: ConnectionStatus = ConnectionStatus.Disconnected,
    val device: CastDevice? = null,
    val message: String? = null
)

data class LocalMediaItem(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val kind: MediaKind,
    val mimeType: String?,
    val sizeBytes: Long,
    val durationMillis: Long?,
    val dateAddedMillis: Long,
    val album: String? = null,
    val artist: String? = null,
    val folder: String? = null,
    val isFavorite: Boolean = false
)

data class WebVideoCandidate(
    val url: String,
    val title: String?,
    val mimeType: String?,
    val qualityLabel: String?,
    val isLikelyExpired: Boolean = false,
    val isSupported: Boolean = true,
    val unsupportedReason: String? = null
)

data class CastRequest(
    val mediaUri: Uri,
    val title: String,
    val mimeType: String?,
    val kind: MediaKind,
    val durationMillis: Long? = null
)

data class PremiumEntitlement(
    val isPremium: Boolean = false,
    val activeProductIds: Set<String> = emptySet(),
    val pendingProductIds: Set<String> = emptySet(),
    val lastCheckedAt: Long = 0L,
    val message: String? = null
)

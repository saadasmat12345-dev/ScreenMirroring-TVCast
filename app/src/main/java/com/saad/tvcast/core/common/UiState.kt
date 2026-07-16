package com.saad.tvcast.core.common

sealed interface UiState<out T> {
    data object Initial : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Content<T>(val value: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val error: AppError, val canRetry: Boolean = true) : UiState<Nothing>
    data class PermissionRequired(val permission: PermissionPurpose) : UiState<Nothing>
    data object Offline : UiState<Nothing>
}

sealed interface AppError {
    val message: String

    data class Network(override val message: String) : AppError
    data class Permission(override val message: String) : AppError
    data class Casting(override val message: String) : AppError
    data class Playback(override val message: String) : AppError
    data class Billing(override val message: String) : AppError
    data class Ads(override val message: String) : AppError
    data class Unknown(override val message: String) : AppError
}

enum class PermissionPurpose {
    NearbyDevices,
    Photos,
    Videos,
    Audio,
    Notifications,
    MediaProjection
}

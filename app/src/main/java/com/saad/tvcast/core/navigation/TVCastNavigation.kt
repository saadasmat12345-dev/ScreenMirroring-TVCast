package com.saad.tvcast.core.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.ui.graphics.vector.ImageVector
import com.saad.tvcast.R

sealed class Destination(val route: String) {
    data object Splash : Destination("splash")
    data object Onboarding : Destination("onboarding")
    data object Home : Destination("home")
    data object Browser : Destination("browser")
    data object Library : Destination("library")
    data object Activity : Destination("activity")
    data object Settings : Destination("settings")
    data object Devices : Destination("devices")
    data object Mirroring : Destination("mirroring")
    data object Premium : Destination("premium")
    data object Player : Destination("player?uri={uri}&title={title}") {
        fun route(uri: String, title: String): String =
            "player?uri=${Uri.encode(uri)}&title=${Uri.encode(title)}"
    }
}

data class BottomTab(
    val destination: Destination,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

val bottomTabs = listOf(
    BottomTab(Destination.Home, R.string.home, Icons.Outlined.Home),
    BottomTab(Destination.Browser, R.string.browser, Icons.Outlined.Bookmarks),
    BottomTab(Destination.Library, R.string.library, Icons.Outlined.VideoLibrary),
    BottomTab(Destination.Activity, R.string.activity, Icons.Outlined.WatchLater),
    BottomTab(Destination.Settings, R.string.settings, Icons.Outlined.Settings)
)

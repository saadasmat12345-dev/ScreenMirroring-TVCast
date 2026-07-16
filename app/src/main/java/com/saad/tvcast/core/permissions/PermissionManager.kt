package com.saad.tvcast.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.saad.tvcast.core.common.PermissionPurpose
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PermissionManager {
    fun permissionsFor(purpose: PermissionPurpose): List<String>
    fun hasPermissions(purpose: PermissionPurpose): Boolean
    fun appSettingsIntent(): Intent
}

@Singleton
class AndroidPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionManager {
    override fun permissionsFor(purpose: PermissionPurpose): List<String> = when (purpose) {
        PermissionPurpose.NearbyDevices -> if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.NEARBY_WIFI_DEVICES) else emptyList()
        PermissionPurpose.Photos -> if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.READ_MEDIA_IMAGES) else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        PermissionPurpose.Videos -> if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.READ_MEDIA_VIDEO) else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        PermissionPurpose.Audio -> if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.READ_MEDIA_AUDIO) else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        PermissionPurpose.Notifications -> if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
        PermissionPurpose.MediaProjection -> emptyList()
    }

    override fun hasPermissions(purpose: PermissionPurpose): Boolean =
        permissionsFor(purpose).all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    override fun appSettingsIntent(): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

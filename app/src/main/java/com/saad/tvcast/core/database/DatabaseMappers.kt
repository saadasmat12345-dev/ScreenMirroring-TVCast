package com.saad.tvcast.core.database

import com.saad.tvcast.core.common.CastDevice
import com.saad.tvcast.core.common.DeviceProtocol
import com.saad.tvcast.core.common.DeviceType

fun CastDevice.toEntity(lastConnectedAt: Long? = null, isFavorite: Boolean = false): DeviceEntity =
    DeviceEntity(
        id = id,
        name = name,
        type = type.name,
        protocol = protocol.name,
        ipAddress = ipAddress,
        descriptorUrl = descriptorUrl,
        controlUrl = controlUrl,
        isFavorite = isFavorite,
        isDemo = isDemo,
        lastSeenAt = lastSeenAt,
        lastConnectedAt = lastConnectedAt
    )

fun DeviceEntity.toDevice(): CastDevice =
    CastDevice(
        id = id,
        name = name,
        type = runCatching { DeviceType.valueOf(type) }.getOrDefault(DeviceType.Unknown),
        protocol = runCatching { DeviceProtocol.valueOf(protocol) }.getOrDefault(DeviceProtocol.Upnp),
        ipAddress = ipAddress,
        descriptorUrl = descriptorUrl,
        controlUrl = controlUrl,
        isDemo = isDemo,
        lastSeenAt = lastSeenAt
    )

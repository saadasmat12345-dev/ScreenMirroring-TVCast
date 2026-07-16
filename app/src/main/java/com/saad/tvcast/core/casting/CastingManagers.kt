package com.saad.tvcast.core.casting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Xml
import com.saad.tvcast.core.common.AppError
import com.saad.tvcast.core.common.CastConnection
import com.saad.tvcast.core.common.CastDevice
import com.saad.tvcast.core.common.CastRequest
import com.saad.tvcast.core.common.ConnectionStatus
import com.saad.tvcast.core.common.DeviceProtocol
import com.saad.tvcast.core.common.DeviceType
import com.saad.tvcast.core.common.MediaKind
import com.saad.tvcast.core.common.UiState
import com.saad.tvcast.core.database.DeviceDao
import com.saad.tvcast.core.database.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

interface DeviceDiscoveryManager {
    fun discover(timeoutMillis: Long = 5_000): Flow<UiState<List<CastDevice>>>
}

interface CastConnectionManager {
    val connection: StateFlow<CastConnection>
    suspend fun connect(device: CastDevice): Result<Unit>
    suspend fun disconnect()
}

interface MediaCastManager {
    suspend fun cast(request: CastRequest, device: CastDevice): Result<Unit>
}

interface ScreenMirrorManager {
    fun createMirroringIntent(): Intent?
}

@Singleton
class DlnaDeviceDiscoveryManager @Inject constructor() : DeviceDiscoveryManager {
    override fun discover(timeoutMillis: Long): Flow<UiState<List<CastDevice>>> = flow {
        emit(UiState.Loading)
        val devices = withContext(Dispatchers.IO) { scanSsdp(timeoutMillis) }
        emit(if (devices.isEmpty()) UiState.Empty else UiState.Content(devices))
    }.catch { throwable ->
        emit(UiState.Error(AppError.Network(throwable.message ?: "Device discovery failed")))
    }

    private fun scanSsdp(timeoutMillis: Long): List<CastDevice> {
        val request = """
            M-SEARCH * HTTP/1.1
            HOST: 239.255.255.250:1900
            MAN: "ssdp:discover"
            MX: 2
            ST: urn:schemas-upnp-org:device:MediaRenderer:1
            
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val found = linkedMapOf<String, CastDevice>()
        DatagramSocket().use { socket ->
            socket.soTimeout = 700
            val address = InetAddress.getByName("239.255.255.250")
            socket.send(DatagramPacket(request, request.size, address, 1900))
            val deadline = System.currentTimeMillis() + timeoutMillis
            while (System.currentTimeMillis() < deadline) {
                val buffer = ByteArray(8192)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                runCatching { socket.receive(responsePacket) }.onSuccess {
                    val raw = String(responsePacket.data, 0, responsePacket.length)
                    val headers = parseHeaders(raw)
                    val location = headers["location"] ?: return@onSuccess
                    val descriptor = runCatching { fetchDescriptor(location) }.getOrNull()
                    val id = headers["usn"] ?: location
                    found[id] = CastDevice(
                        id = id,
                        name = descriptor?.friendlyName ?: responsePacket.address.hostAddress ?: "DLNA renderer",
                        type = descriptor?.deviceType ?: DeviceType.Tv,
                        protocol = DeviceProtocol.Dlna,
                        ipAddress = responsePacket.address.hostAddress,
                        descriptorUrl = location,
                        controlUrl = descriptor?.avTransportControlUrl,
                        isDemo = false
                    )
                }
            }
        }
        return found.values.toList()
    }

    private fun parseHeaders(raw: String): Map<String, String> =
        raw.lineSequence()
            .mapNotNull { line ->
                val index = line.indexOf(':')
                if (index <= 0) null else line.substring(0, index).trim().lowercase() to line.substring(index + 1).trim()
            }
            .toMap()

    private fun fetchDescriptor(location: String): DeviceDescriptor {
        val parser = Xml.newPullParser()
        val connection = (URL(location).openConnection() as HttpURLConnection).apply {
            connectTimeout = 3_000
            readTimeout = 3_000
        }
        try {
            connection.inputStream.use { stream ->
                parser.setInput(stream, null)
                var friendlyName: String? = null
                var deviceType: DeviceType = DeviceType.Unknown
                var serviceType: String? = null
                var controlUrl: String? = null
                var inService = false
                var selectedControlUrl: String? = null
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "friendlyName" -> friendlyName = parser.nextText()
                            "deviceType" -> deviceType = parser.nextText().toDeviceType()
                            "service" -> {
                                inService = true
                                serviceType = null
                                controlUrl = null
                            }
                            "serviceType" -> if (inService) serviceType = parser.nextText()
                            "controlURL" -> if (inService) controlUrl = parser.nextText()
                        }
                    } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "service") {
                        if (serviceType?.contains("AVTransport", ignoreCase = true) == true && controlUrl != null) {
                            selectedControlUrl = resolveUrl(location, controlUrl.orEmpty())
                        }
                        inService = false
                    }
                }
                return DeviceDescriptor(
                    friendlyName = friendlyName,
                    deviceType = deviceType,
                    avTransportControlUrl = selectedControlUrl
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun String.toDeviceType(): DeviceType = when {
        contains("MediaRenderer", ignoreCase = true) -> DeviceType.Tv
        contains("Speaker", ignoreCase = true) -> DeviceType.Speaker
        else -> DeviceType.Unknown
    }

    private fun resolveUrl(base: String, path: String): String =
        URI(base).resolve(path).toString()
}

private data class DeviceDescriptor(
    val friendlyName: String?,
    val deviceType: DeviceType,
    val avTransportControlUrl: String?
)

@Singleton
class DemoDeviceDiscoveryManager @Inject constructor() : DeviceDiscoveryManager {
    override fun discover(timeoutMillis: Long): Flow<UiState<List<CastDevice>>> = flow {
        emit(UiState.Loading)
        emit(
            UiState.Content(
                listOf(
                    CastDevice(
                        id = "demo-dlna-tv",
                        name = "Demo Living Room TV",
                        type = DeviceType.Tv,
                        protocol = DeviceProtocol.Demo,
                        ipAddress = "192.0.2.10",
                        descriptorUrl = null,
                        controlUrl = null,
                        isDemo = true
                    )
                )
            )
        )
    }
}

@Singleton
class DefaultCastConnectionManager @Inject constructor(
    private val deviceDao: DeviceDao
) : CastConnectionManager {
    private val _connection = MutableStateFlow(CastConnection())
    override val connection: StateFlow<CastConnection> = _connection

    override suspend fun connect(device: CastDevice): Result<Unit> = runCatching {
        _connection.value = CastConnection(ConnectionStatus.Connecting, device)
        deviceDao.upsert(device.toEntity(lastConnectedAt = System.currentTimeMillis()))
        _connection.value = CastConnection(ConnectionStatus.Connected, device)
    }.onFailure { throwable ->
        _connection.value = CastConnection(ConnectionStatus.Error, device, throwable.message)
    }

    override suspend fun disconnect() {
        _connection.value = CastConnection(ConnectionStatus.Disconnected)
    }
}

@Singleton
class DlnaMediaCastManager @Inject constructor() : MediaCastManager {
    override suspend fun cast(request: CastRequest, device: CastDevice): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val controlUrl = device.controlUrl ?: error("This DLNA renderer did not expose AVTransport control.")
            val mediaUrl = request.mediaUri.toString()
            val scheme = request.mediaUri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                error("Local media requires a renderer-accessible URL. Use phone playback or a direct web media URL for this first release.")
            }
            sendSoap(controlUrl, "SetAVTransportURI", setUriBody(mediaUrl, request.title))
            sendSoap(controlUrl, "Play", playBody())
        }
    }

    private fun sendSoap(controlUrl: String, action: String, body: String) {
        val connection = (URL(controlUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 8_000
            doOutput = true
            setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#$action\"")
        }
        connection.outputStream.use { it.write(body.toByteArray()) }
        val code = connection.responseCode
        if (code !in 200..299) {
            val message = connection.errorStream?.let { stream ->
                BufferedReader(InputStreamReader(stream)).use { it.readText() }
            }.orEmpty()
            error("DLNA action $action failed with HTTP $code. $message")
        }
    }

    private fun setUriBody(url: String, title: String): String = soapEnvelope(
        """
        <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
            <InstanceID>0</InstanceID>
            <CurrentURI>${url.escapeXml()}</CurrentURI>
            <CurrentURIMetaData>${title.escapeXml()}</CurrentURIMetaData>
        </u:SetAVTransportURI>
        """.trimIndent()
    )

    private fun playBody(): String = soapEnvelope(
        """
        <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
            <InstanceID>0</InstanceID>
            <Speed>1</Speed>
        </u:Play>
        """.trimIndent()
    )

    private fun soapEnvelope(content: String): String =
        """<?xml version="1.0" encoding="utf-8"?>
        <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
            <s:Body>$content</s:Body>
        </s:Envelope>
        """.trimIndent()

    private fun String.escapeXml(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}

@Singleton
class AndroidScreenMirrorManager @Inject constructor(
    @ApplicationContext private val context: Context
) : ScreenMirrorManager {
    override fun createMirroringIntent(): Intent? {
        val castIntent = Intent(Settings.ACTION_CAST_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (castIntent.resolveActivity(context.packageManager) != null) {
            castIntent
        } else {
            Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .takeIf { it.resolveActivity(context.packageManager) != null }
        }
    }
}

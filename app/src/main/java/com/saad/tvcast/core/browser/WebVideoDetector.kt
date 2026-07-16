package com.saad.tvcast.core.browser

import com.saad.tvcast.core.common.WebVideoCandidate
import javax.inject.Inject
import javax.inject.Singleton

interface WebVideoDetector {
    fun detectFromRequest(url: String, mimeType: String? = null, title: String? = null): WebVideoCandidate?
}

@Singleton
class DefaultWebVideoDetector @Inject constructor() : WebVideoDetector {
    private val directExtensions = setOf("mp4", "m4v", "webm", "mov", "m3u8", "mp3", "aac", "wav", "ogg")
    private val supportedMimePrefixes = setOf("video/", "audio/", "application/vnd.apple.mpegurl")
    private val protectedSignals = listOf("widevine", "fairplay", "playready", "license", "/drm/")

    override fun detectFromRequest(url: String, mimeType: String?, title: String?): WebVideoCandidate? {
        val normalized = url.substringBefore('#')
        if (protectedSignals.any { normalized.contains(it, ignoreCase = true) }) {
            return WebVideoCandidate(
                url = normalized,
                title = title,
                mimeType = mimeType,
                qualityLabel = null,
                isSupported = false,
                unsupportedReason = "Protected or DRM media is not supported."
            )
        }

        val extension = normalized.substringBefore('?').substringAfterLast('.', missingDelimiterValue = "")
        val mimeLooksSupported = mimeType?.let { mime ->
            supportedMimePrefixes.any { prefix -> mime.startsWith(prefix, ignoreCase = true) }
        } == true
        val extensionLooksSupported = extension.lowercase() in directExtensions

        if (!mimeLooksSupported && !extensionLooksSupported) return null

        return WebVideoCandidate(
            url = normalized,
            title = title,
            mimeType = mimeType,
            qualityLabel = inferQualityLabel(normalized),
            isLikelyExpired = listOf("expires=", "X-Amz-Expires", "signature=").any { normalized.contains(it, true) },
            isSupported = true
        )
    }

    private fun inferQualityLabel(url: String): String? = when {
        url.contains("2160", true) || url.contains("4k", true) -> "4K"
        url.contains("1080", true) -> "1080p"
        url.contains("720", true) -> "720p"
        url.contains("480", true) -> "480p"
        else -> null
    }
}

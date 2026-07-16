package com.saad.tvcast

import com.saad.tvcast.core.browser.DefaultWebVideoDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebVideoDetectorTest {
    private val detector = DefaultWebVideoDetector()

    @Test
    fun detectsDirectVideoUrl() {
        val result = detector.detectFromRequest("https://example.com/movie-1080.mp4", "video/mp4", "Movie")
        assertNotNull(result)
        assertTrue(result!!.isSupported)
    }

    @Test
    fun rejectsNonMediaUrl() {
        assertNull(detector.detectFromRequest("https://example.com/page.html", "text/html", null))
    }

    @Test
    fun flagsProtectedSignals() {
        val result = detector.detectFromRequest("https://example.com/drm/license/movie.mp4", "video/mp4", null)
        assertNotNull(result)
        assertFalse(result!!.isSupported)
    }
}

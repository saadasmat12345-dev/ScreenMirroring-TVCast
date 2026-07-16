package com.saad.tvcast

import com.saad.tvcast.core.ads.AdPlacement
import com.saad.tvcast.core.ads.GoogleMobileAdsManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsManagerTest {
    private val manager = GoogleMobileAdsManager()

    @Test
    fun hidesAdsForPremiumUsers() {
        assertFalse(manager.canShowAd(AdPlacement.HomeNative, isPremium = true, isCastingActive = false))
    }

    @Test
    fun hidesAdsWhileCasting() {
        assertFalse(manager.canShowAd(AdPlacement.HomeNative, isPremium = false, isCastingActive = true))
    }

    @Test
    fun capsInterstitialFrequency() {
        assertTrue(manager.canShowAd(AdPlacement.CompletedActionInterstitial, false, false, nowMillis = 200_000))
        manager.markShown(AdPlacement.CompletedActionInterstitial, nowMillis = 200_000)
        assertFalse(manager.canShowAd(AdPlacement.CompletedActionInterstitial, false, false, nowMillis = 210_000))
    }
}

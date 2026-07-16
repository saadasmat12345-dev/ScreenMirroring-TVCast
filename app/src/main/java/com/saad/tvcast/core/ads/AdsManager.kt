package com.saad.tvcast.core.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AdPlacement { HomeNative, MediaListNative, NonImmersiveBanner, CompletedActionInterstitial, OptionalRewarded, AppOpen }

data class AdRules(
    val enabled: Boolean = true,
    val minInterstitialIntervalMillis: Long = 180_000,
    val appOpenMinIntervalMillis: Long = 21_600_000
)

interface AdsManager {
    val rules: StateFlow<AdRules>
    fun initialize(context: Context)
    fun canShowAd(placement: AdPlacement, isPremium: Boolean, isCastingActive: Boolean, nowMillis: Long = System.currentTimeMillis()): Boolean
    fun markShown(placement: AdPlacement, nowMillis: Long = System.currentTimeMillis())
}

object TestAdUnits {
    const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
}

@Singleton
class GoogleMobileAdsManager @Inject constructor() : AdsManager {
    private val _rules = MutableStateFlow(AdRules())
    override val rules: StateFlow<AdRules> = _rules
    private val lastShown = mutableMapOf<AdPlacement, Long>()
    private var initialized = false

    override fun initialize(context: Context) {
        if (!initialized) {
            MobileAds.initialize(context)
            initialized = true
        }
    }

    override fun canShowAd(placement: AdPlacement, isPremium: Boolean, isCastingActive: Boolean, nowMillis: Long): Boolean {
        val currentRules = rules.value
        if (!currentRules.enabled || isPremium || isCastingActive) return false
        return when (placement) {
            AdPlacement.CompletedActionInterstitial -> nowMillis - (lastShown[placement] ?: 0L) >= currentRules.minInterstitialIntervalMillis
            AdPlacement.AppOpen -> nowMillis - (lastShown[placement] ?: 0L) >= currentRules.appOpenMinIntervalMillis
            else -> true
        }
    }

    override fun markShown(placement: AdPlacement, nowMillis: Long) {
        lastShown[placement] = nowMillis
    }
}

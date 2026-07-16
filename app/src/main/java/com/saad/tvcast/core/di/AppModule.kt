package com.saad.tvcast.core.di

import android.content.Context
import androidx.room.Room
import com.saad.tvcast.BuildConfig
import com.saad.tvcast.core.ads.AdsManager
import com.saad.tvcast.core.ads.GoogleMobileAdsManager
import com.saad.tvcast.core.billing.BillingManager
import com.saad.tvcast.core.billing.GooglePlayBillingManager
import com.saad.tvcast.core.browser.DefaultWebVideoDetector
import com.saad.tvcast.core.browser.WebVideoDetector
import com.saad.tvcast.core.casting.AndroidScreenMirrorManager
import com.saad.tvcast.core.casting.CastConnectionManager
import com.saad.tvcast.core.casting.DefaultCastConnectionManager
import com.saad.tvcast.core.casting.DemoDeviceDiscoveryManager
import com.saad.tvcast.core.casting.DeviceDiscoveryManager
import com.saad.tvcast.core.casting.DlnaDeviceDiscoveryManager
import com.saad.tvcast.core.casting.DlnaMediaCastManager
import com.saad.tvcast.core.casting.MediaCastManager
import com.saad.tvcast.core.casting.ScreenMirrorManager
import com.saad.tvcast.core.database.BrowserDao
import com.saad.tvcast.core.database.CastingHistoryDao
import com.saad.tvcast.core.database.DeviceDao
import com.saad.tvcast.core.database.MediaFavoriteDao
import com.saad.tvcast.core.database.TVCastDatabase
import com.saad.tvcast.core.network.AndroidNetworkMonitor
import com.saad.tvcast.core.network.NetworkMonitor
import com.saad.tvcast.core.permissions.AndroidPermissionManager
import com.saad.tvcast.core.permissions.PermissionManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TVCastDatabase =
        Room.databaseBuilder(context, TVCastDatabase::class.java, "tvcast.db")
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides fun provideDeviceDao(database: TVCastDatabase): DeviceDao = database.deviceDao()
    @Provides fun provideHistoryDao(database: TVCastDatabase): CastingHistoryDao = database.castingHistoryDao()
    @Provides fun provideFavoriteDao(database: TVCastDatabase): MediaFavoriteDao = database.mediaFavoriteDao()
    @Provides fun provideBrowserDao(database: TVCastDatabase): BrowserDao = database.browserDao()

    @Provides
    @Singleton
    fun provideDeviceDiscoveryManager(
        real: DlnaDeviceDiscoveryManager,
        demo: DemoDeviceDiscoveryManager
    ): DeviceDiscoveryManager = if (BuildConfig.ENABLE_DEMO_CASTING) demo else real
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ManagerBindings {
    @Binds abstract fun bindCastConnectionManager(impl: DefaultCastConnectionManager): CastConnectionManager
    @Binds abstract fun bindMediaCastManager(impl: DlnaMediaCastManager): MediaCastManager
    @Binds abstract fun bindScreenMirrorManager(impl: AndroidScreenMirrorManager): ScreenMirrorManager
    @Binds abstract fun bindWebVideoDetector(impl: DefaultWebVideoDetector): WebVideoDetector
    @Binds abstract fun bindBillingManager(impl: GooglePlayBillingManager): BillingManager
    @Binds abstract fun bindAdsManager(impl: GoogleMobileAdsManager): AdsManager
    @Binds abstract fun bindPermissionManager(impl: AndroidPermissionManager): PermissionManager
    @Binds abstract fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
}

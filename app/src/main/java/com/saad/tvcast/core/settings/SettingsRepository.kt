package com.saad.tvcast.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saad.tvcast.BuildConfig
import com.saad.tvcast.core.common.CastQuality
import com.saad.tvcast.core.common.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "tvcast_settings")

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val castQuality: CastQuality = CastQuality.Auto,
    val autoReconnect: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val browserHomepage: String = BuildConfig.BROWSER_DEFAULT_HOME,
    val notificationsEnabled: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val themeMode = stringPreferencesKey("theme_mode")
        val castQuality = stringPreferencesKey("cast_quality")
        val autoReconnect = booleanPreferencesKey("auto_reconnect")
        val keepScreenAwake = booleanPreferencesKey("keep_screen_awake")
        val browserHomepage = stringPreferencesKey("browser_homepage")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            onboardingComplete = prefs[Keys.onboardingComplete] ?: false,
            themeMode = prefs[Keys.themeMode].toEnum(ThemeMode.System),
            castQuality = prefs[Keys.castQuality].toEnum(CastQuality.Auto),
            autoReconnect = prefs[Keys.autoReconnect] ?: false,
            keepScreenAwake = prefs[Keys.keepScreenAwake] ?: true,
            browserHomepage = prefs[Keys.browserHomepage] ?: BuildConfig.BROWSER_DEFAULT_HOME,
            notificationsEnabled = prefs[Keys.notificationsEnabled] ?: false
        )
    }

    suspend fun completeOnboarding() {
        context.settingsDataStore.edit { it[Keys.onboardingComplete] = true }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setCastQuality(quality: CastQuality) {
        context.settingsDataStore.edit { it[Keys.castQuality] = quality.name }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.autoReconnect] = enabled }
    }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.keepScreenAwake] = enabled }
    }

    suspend fun setBrowserHomepage(url: String) {
        context.settingsDataStore.edit { it[Keys.browserHomepage] = url }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.notificationsEnabled] = enabled }
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() } ?: default
}

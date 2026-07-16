package com.saad.tvcast.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.saad.tvcast.BuildConfig
import com.saad.tvcast.R
import com.saad.tvcast.core.common.CastQuality
import com.saad.tvcast.core.common.ThemeMode
import com.saad.tvcast.core.designsystem.component.SectionHeader
import com.saad.tvcast.core.designsystem.component.StatusCard
import com.saad.tvcast.core.settings.AppSettings
import com.saad.tvcast.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setQuality(quality: CastQuality) = viewModelScope.launch { settingsRepository.setCastQuality(quality) }
    fun setAutoReconnect(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAutoReconnect(enabled) }
    fun setKeepAwake(enabled: Boolean) = viewModelScope.launch { settingsRepository.setKeepScreenAwake(enabled) }
    fun setHomepage(value: String) = viewModelScope.launch { settingsRepository.setBrowserHomepage(value) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onPremium: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            SectionHeader(stringResource(R.string.theme))
            SingleChoiceSegmentedButtonRow {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.System -> stringResource(R.string.system)
                                ThemeMode.Light -> stringResource(R.string.light)
                                ThemeMode.Dark -> stringResource(R.string.dark)
                            }
                        )
                    }
                }
            }
        }
        item {
            SectionHeader(stringResource(R.string.default_casting_quality))
            SingleChoiceSegmentedButtonRow {
                CastQuality.entries.forEachIndexed { index, quality ->
                    SegmentedButton(
                        selected = settings.castQuality == quality,
                        onClick = { viewModel.setQuality(quality) },
                        shape = SegmentedButtonDefaults.itemShape(index, CastQuality.entries.size)
                    ) { Text(quality.name) }
                }
            }
        }
        item { ToggleRow(stringResource(R.string.auto_reconnect), settings.autoReconnect, viewModel::setAutoReconnect) }
        item { ToggleRow(stringResource(R.string.keep_screen_awake), settings.keepScreenAwake, viewModel::setKeepAwake) }
        item { ToggleRow(stringResource(R.string.notifications), settings.notificationsEnabled, viewModel::setNotifications) }
        item {
            OutlinedTextField(
                value = settings.browserHomepage,
                onValueChange = viewModel::setHomepage,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.browser_homepage)) }
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPremium) { Text(stringResource(R.string.premium)) }
                TextButton(onClick = {}) { Text(stringResource(R.string.restore_purchase)) }
                TextButton(onClick = {}) { Text(stringResource(R.string.privacy_policy)) }
                TextButton(onClick = {}) { Text(stringResource(R.string.terms_of_use)) }
                TextButton(onClick = {}) { Text(stringResource(R.string.help)) }
                TextButton(onClick = {}) { Text(stringResource(R.string.send_feedback)) }
                TextButton(onClick = {}) { Text(stringResource(R.string.rate_app)) }
                StatusCard(stringResource(R.string.app_version), BuildConfig.VERSION_NAME)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

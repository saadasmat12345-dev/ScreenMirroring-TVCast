package com.saad.tvcast.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.saad.tvcast.R
import com.saad.tvcast.core.ads.AdPlacement
import com.saad.tvcast.core.ads.AdsManager
import com.saad.tvcast.core.casting.CastConnectionManager
import com.saad.tvcast.core.common.CastConnection
import com.saad.tvcast.core.database.CastingHistoryEntity
import com.saad.tvcast.core.database.DeviceDao
import com.saad.tvcast.core.database.CastingHistoryDao
import com.saad.tvcast.core.database.DeviceEntity
import com.saad.tvcast.core.designsystem.component.BannerAd
import com.saad.tvcast.core.designsystem.component.ConnectionCard
import com.saad.tvcast.core.designsystem.component.DeviceRow
import com.saad.tvcast.core.designsystem.component.EmptyPanel
import com.saad.tvcast.core.designsystem.component.FeatureActionCard
import com.saad.tvcast.core.designsystem.component.SectionHeader
import com.saad.tvcast.core.designsystem.component.StatusCard
import com.saad.tvcast.core.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val connection: CastConnection = CastConnection(),
    val recentDevices: List<DeviceEntity> = emptyList(),
    val recentHistory: List<CastingHistoryEntity> = emptyList(),
    val showAds: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectionManager: CastConnectionManager,
    private val deviceDao: DeviceDao,
    private val historyDao: CastingHistoryDao,
    billingManager: BillingManager,
    adsManager: AdsManager
) : ViewModel() {
    val uiState = combine(
        connectionManager.connection,
        deviceDao.observeRecentDevices(),
        historyDao.observeHistory(),
        billingManager.entitlement
    ) { connection, devices, history, entitlement ->
        HomeUiState(
            connection = connection,
            recentDevices = devices.take(4),
            recentHistory = history.take(4),
            showAds = adsManager.canShowAd(
                placement = AdPlacement.HomeNative,
                isPremium = entitlement.isPremium,
                isCastingActive = connection.device != null
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun disconnect() {
        viewModelScope.launch { connectionManager.disconnect() }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onScanDevices: () -> Unit,
    onMirror: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.splash_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ConnectionCard(
                connection = state.connection,
                onScan = onScanDevices,
                onDisconnect = viewModel::disconnect,
                onReconnect = onScanDevices
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FeatureActionCard(stringResource(R.string.screen_mirroring), stringResource(R.string.screen_mirroring_subtitle), Icons.Outlined.ScreenShare, onMirror, Modifier.weight(1f))
                    FeatureActionCard(stringResource(R.string.cast_video), stringResource(R.string.cast_video_subtitle), Icons.Outlined.VideoLibrary, onOpenLibrary, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FeatureActionCard(stringResource(R.string.cast_photo), stringResource(R.string.cast_photo_subtitle), Icons.Outlined.Image, onOpenLibrary, Modifier.weight(1f))
                    FeatureActionCard(stringResource(R.string.cast_music), stringResource(R.string.cast_music_subtitle), Icons.Outlined.MusicNote, onOpenLibrary, Modifier.weight(1f))
                }
                FeatureActionCard(stringResource(R.string.web_video_cast), stringResource(R.string.web_video_cast_subtitle), Icons.Outlined.Web, onOpenBrowser)
            }
        }
        item { BannerAd(visible = state.showAds) }
        item { SectionHeader(stringResource(R.string.recent_devices)) }
        if (state.recentDevices.isEmpty()) {
            item { EmptyPanel(stringResource(R.string.empty_recent_devices)) }
        } else {
            items(state.recentDevices, key = { it.id }) { device ->
                DeviceRow(device.name, listOfNotNull(device.protocol, device.ipAddress).joinToString(" | "), onClick = onScanDevices)
            }
        }
        item { SectionHeader(stringResource(R.string.recently_played)) }
        if (state.recentHistory.isEmpty()) {
            item { EmptyPanel(stringResource(R.string.empty_recent_media)) }
        } else {
            items(state.recentHistory, key = { it.id }) { item ->
                DeviceRow(item.mediaTitle, listOfNotNull(item.mediaKind, item.deviceName, item.status).joinToString(" | "), onClick = onOpenLibrary)
            }
        }
        item {
            StatusCard(
                title = stringResource(R.string.connection_help),
                body = stringResource(R.string.device_troubleshooting),
                icon = Icons.Outlined.Cast
            )
        }
    }
}

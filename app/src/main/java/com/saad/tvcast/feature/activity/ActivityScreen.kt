package com.saad.tvcast.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.saad.tvcast.core.database.CastingHistoryDao
import com.saad.tvcast.core.database.CastingHistoryEntity
import com.saad.tvcast.core.database.DeviceDao
import com.saad.tvcast.core.designsystem.component.DeviceRow
import com.saad.tvcast.core.designsystem.component.EmptyPanel
import com.saad.tvcast.core.designsystem.component.SectionHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val historyDao: CastingHistoryDao,
    deviceDao: DeviceDao
) : ViewModel() {
    val history = historyDao.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val devices = deviceDao.observeRecentDevices().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearHistory() {
        viewModelScope.launch { historyDao.clear() }
    }

    fun remove(item: CastingHistoryEntity) {
        viewModelScope.launch { historyDao.delete(item) }
    }
}

@Composable
fun ActivityScreen(viewModel: ActivityViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(stringResource(R.string.activity), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item { SectionHeader(stringResource(R.string.casting_history)) }
        if (history.isEmpty()) {
            item { EmptyPanel(stringResource(R.string.empty_recent_media)) }
        } else {
            items(history, key = { it.id }) { item ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeviceRow(item.mediaTitle, listOfNotNull(item.mediaKind, item.deviceName, item.status).joinToString(" | "), onClick = { })
                    OutlinedButton(onClick = { viewModel.remove(item) }) { Text(stringResource(R.string.remove)) }
                }
            }
            item { Button(onClick = viewModel::clearHistory) { Text(stringResource(R.string.clear_history)) } }
        }
        item { SectionHeader(stringResource(R.string.recently_connected_devices)) }
        if (devices.isEmpty()) {
            item { EmptyPanel(stringResource(R.string.empty_recent_devices)) }
        } else {
            items(devices, key = { it.id }) { device ->
                DeviceRow(device.name, listOfNotNull(device.protocol, device.ipAddress).joinToString(" | "), onClick = { })
            }
        }
    }
}

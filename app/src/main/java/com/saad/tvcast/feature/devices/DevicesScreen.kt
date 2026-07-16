package com.saad.tvcast.feature.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.saad.tvcast.R
import com.saad.tvcast.core.casting.CastConnectionManager
import com.saad.tvcast.core.casting.DeviceDiscoveryManager
import com.saad.tvcast.core.common.CastDevice
import com.saad.tvcast.core.common.UiState
import com.saad.tvcast.core.designsystem.component.DeviceRow
import com.saad.tvcast.core.designsystem.component.EmptyPanel
import com.saad.tvcast.core.designsystem.component.ErrorPanel
import com.saad.tvcast.core.designsystem.component.LoadingPanel
import com.saad.tvcast.core.designsystem.component.StatusCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val discoveryManager: DeviceDiscoveryManager,
    private val connectionManager: CastConnectionManager
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<CastDevice>>>(UiState.Initial)
    val state: StateFlow<UiState<List<CastDevice>>> = _state

    fun scan() {
        viewModelScope.launch {
            discoveryManager.discover().collect { _state.value = it }
        }
    }

    fun connect(device: CastDevice) {
        viewModelScope.launch { connectionManager.connect(device) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.scan() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.devices)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null) } },
                actions = { IconButton(onClick = viewModel::scan) { Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.refresh)) } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(stringResource(R.string.nearby_devices), style = MaterialTheme.typography.headlineSmall)
            }
            when (val current = state) {
                UiState.Initial, UiState.Loading -> item { LoadingPanel(stringResource(R.string.scanning_devices)) }
                UiState.Empty -> {
                    item { EmptyPanel(stringResource(R.string.no_devices_found)) }
                    item { StatusCard(stringResource(R.string.help), stringResource(R.string.device_troubleshooting), Icons.Outlined.WifiOff) }
                }
                is UiState.Error -> item { ErrorPanel(current.error, onRetry = viewModel::scan) }
                is UiState.Content -> items(current.value, key = { it.id }) { device ->
                    DeviceRow(
                        name = device.name,
                        detail = listOfNotNull(device.protocol.name, device.type.name, device.ipAddress).joinToString(" | "),
                        status = if (device.isDemo) stringResource(R.string.demo_device_label) else stringResource(R.string.connect),
                        onClick = { viewModel.connect(device) }
                    )
                }
                is UiState.PermissionRequired -> item { StatusCard(stringResource(R.string.permission_required), current.permission.name) }
                UiState.Offline -> item { StatusCard(stringResource(R.string.offline), stringResource(R.string.device_troubleshooting)) }
            }
        }
    }
}

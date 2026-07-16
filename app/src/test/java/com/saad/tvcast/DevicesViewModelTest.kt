package com.saad.tvcast

import com.saad.tvcast.core.casting.CastConnectionManager
import com.saad.tvcast.core.casting.DeviceDiscoveryManager
import com.saad.tvcast.core.common.CastConnection
import com.saad.tvcast.core.common.CastDevice
import com.saad.tvcast.core.common.DeviceProtocol
import com.saad.tvcast.core.common.DeviceType
import com.saad.tvcast.core.common.UiState
import com.saad.tvcast.feature.devices.DevicesViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DevicesViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun scanPublishesDiscoveredDevices() = runTest {
        val device = CastDevice(
            id = "device",
            name = "TV",
            type = DeviceType.Tv,
            protocol = DeviceProtocol.Dlna,
            ipAddress = "192.168.1.20",
            descriptorUrl = null,
            controlUrl = null
        )
        val viewModel = DevicesViewModel(
            discoveryManager = object : DeviceDiscoveryManager {
                override fun discover(timeoutMillis: Long): Flow<UiState<List<CastDevice>>> = flowOf(UiState.Content(listOf(device)))
            },
            connectionManager = object : CastConnectionManager {
                override val connection: StateFlow<CastConnection> = MutableStateFlow(CastConnection())
                override suspend fun connect(device: CastDevice): Result<Unit> = Result.success(Unit)
                override suspend fun disconnect() = Unit
            }
        )

        viewModel.scan()

        val state = viewModel.state.value as UiState.Content
        assertEquals("TV", state.value.first().name)
    }
}

package com.saad.tvcast.feature.mirroring

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.saad.tvcast.R
import com.saad.tvcast.core.casting.ScreenMirrorManager
import com.saad.tvcast.core.designsystem.component.StatusCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MirroringViewModel @Inject constructor(
    private val screenMirrorManager: ScreenMirrorManager
) : ViewModel() {
    fun mirroringIntent() = screenMirrorManager.createMirroringIntent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirroringScreen(
    viewModel: MirroringViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_mirroring)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                title = stringResource(R.string.screen_mirroring),
                body = stringResource(R.string.mirroring_notice),
                icon = Icons.Outlined.ScreenShare
            )
            Button(
                onClick = {
                    val intent = viewModel.mirroringIntent()
                    if (intent == null) {
                        message = context.getString(R.string.mirroring_unavailable)
                    } else {
                        runCatching { context.startActivity(intent) }
                            .onFailure { throwable ->
                                message = if (throwable is ActivityNotFoundException) {
                                    context.getString(R.string.mirroring_unavailable)
                                } else {
                                    throwable.message
                                }
                            }
                    }
                }
            ) {
                Text(stringResource(R.string.start_screen_mirroring))
            }
            message?.let { StatusCard(stringResource(R.string.help), it) }
        }
    }
}

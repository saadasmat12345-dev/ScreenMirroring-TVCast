package com.saad.tvcast.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.saad.tvcast.R
import com.saad.tvcast.core.designsystem.component.StatusCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uri: String?,
    title: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(uri) {
        uri?.let {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(it)))
                prepare()
                playWhenReady = true
            }
        }
    }
    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title ?: stringResource(R.string.playback)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (player == null) {
                StatusCard(stringResource(R.string.playback), stringResource(R.string.no_player_media))
            } else {
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = { PlayerView(it).apply { this.player = player } },
                    update = { it.player = player },
                    onRelease = { it.player = null }
                )
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            (context as? Activity)?.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                        }
                    }
                ) {
                    Text(stringResource(R.string.picture_in_picture))
                }
            }
        }
    }
}

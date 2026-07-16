package com.saad.tvcast.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.saad.tvcast.R
import com.saad.tvcast.core.casting.CastConnectionManager
import com.saad.tvcast.core.casting.MediaCastManager
import com.saad.tvcast.core.common.AppError
import com.saad.tvcast.core.common.CastRequest
import com.saad.tvcast.core.common.LocalMediaItem
import com.saad.tvcast.core.common.MediaKind
import com.saad.tvcast.core.common.PermissionPurpose
import com.saad.tvcast.core.common.UiState
import com.saad.tvcast.core.database.CastingHistoryDao
import com.saad.tvcast.core.database.CastingHistoryEntity
import com.saad.tvcast.core.database.MediaFavoriteDao
import com.saad.tvcast.core.database.MediaFavoriteEntity
import com.saad.tvcast.core.designsystem.component.EmptyPanel
import com.saad.tvcast.core.designsystem.component.ErrorPanel
import com.saad.tvcast.core.designsystem.component.LoadingPanel
import com.saad.tvcast.core.designsystem.component.MediaRow
import com.saad.tvcast.core.designsystem.component.StatusCard
import com.saad.tvcast.core.media.LocalMediaRepository
import com.saad.tvcast.core.permissions.PermissionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private data class LibraryTab(val labelRes: Int, val kind: MediaKind?)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val mediaRepository: LocalMediaRepository,
    private val permissionManager: PermissionManager,
    private val favoriteDao: MediaFavoriteDao,
    private val connectionManager: CastConnectionManager,
    private val castManager: MediaCastManager,
    private val historyDao: CastingHistoryDao,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<LocalMediaItem>>>(UiState.Initial)
    val state: StateFlow<UiState<List<LocalMediaItem>>> = _state
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun requiredPurpose(kind: MediaKind?): PermissionPurpose? = when (kind) {
        MediaKind.Video -> PermissionPurpose.Videos
        MediaKind.Photo -> PermissionPurpose.Photos
        MediaKind.Music -> PermissionPurpose.Audio
        else -> null
    }

    fun permissionsFor(kind: MediaKind?): List<String> =
        requiredPurpose(kind)?.let(permissionManager::permissionsFor).orEmpty()

    fun load(kind: MediaKind?) {
        viewModelScope.launch {
            val purpose = requiredPurpose(kind)
            if (purpose != null && !permissionManager.hasPermissions(purpose)) {
                _state.value = UiState.PermissionRequired(purpose)
                return@launch
            }
            _state.value = UiState.Loading
            runCatching {
                when (kind) {
                    MediaKind.Video -> mediaRepository.queryVideos()
                    MediaKind.Photo -> mediaRepository.queryPhotos()
                    MediaKind.Music -> mediaRepository.queryMusic()
                    null -> emptyList()
                    MediaKind.WebVideo -> emptyList()
                }
            }.onSuccess { items ->
                val withFavorites = items.map { item -> item.copy(isFavorite = favoriteDao.isFavorite(item.uri.toString())) }
                _state.value = if (withFavorites.isEmpty()) UiState.Empty else UiState.Content(withFavorites)
            }.onFailure {
                _state.value = UiState.Error(AppError.Unknown(it.message ?: "Media loading failed"))
            }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val favorites = favoriteDao.observeFavorites().first()
            val items = favorites.map {
                LocalMediaItem(
                    id = it.mediaUri,
                    uri = Uri.parse(it.mediaUri),
                    displayName = it.title,
                    kind = runCatching { MediaKind.valueOf(it.mediaKind) }.getOrDefault(MediaKind.Video),
                    mimeType = null,
                    sizeBytes = 0,
                    durationMillis = null,
                    dateAddedMillis = it.addedAt,
                    isFavorite = true
                )
            }
            _state.value = if (items.isEmpty()) UiState.Empty else UiState.Content(items)
        }
    }

    fun toggleFavorite(item: LocalMediaItem) {
        viewModelScope.launch {
            if (favoriteDao.isFavorite(item.uri.toString())) {
                favoriteDao.remove(item.uri.toString())
            } else {
                favoriteDao.upsert(
                    MediaFavoriteEntity(
                        mediaUri = item.uri.toString(),
                        title = item.displayName,
                        mediaKind = item.kind.name,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun cast(item: LocalMediaItem) {
        viewModelScope.launch {
            val device = connectionManager.connection.value.device
            if (device == null) {
                _message.value = context.getString(R.string.connect_tv_first)
                return@launch
            }
            val result = castManager.cast(
                CastRequest(
                    mediaUri = item.uri,
                    title = item.displayName,
                    mimeType = item.mimeType,
                    kind = item.kind,
                    durationMillis = item.durationMillis
                ),
                device
            )
            historyDao.insert(
                CastingHistoryEntity(
                    mediaTitle = item.displayName,
                    mediaUri = item.uri.toString(),
                    mediaKind = item.kind.name,
                    deviceId = device.id,
                    deviceName = device.name,
                    status = if (result.isSuccess) context.getString(R.string.cast_success) else context.getString(R.string.cast_failed),
                    playedAt = System.currentTimeMillis()
                )
            )
            _message.value = result.exceptionOrNull()?.message ?: context.getString(R.string.cast_request_sent)
        }
    }
}

@Composable
fun LibraryScreen(viewModel: LibraryViewModel = hiltViewModel()) {
    val tabs = listOf(
        LibraryTab(R.string.videos, MediaKind.Video),
        LibraryTab(R.string.photos, MediaKind.Photo),
        LibraryTab(R.string.music, MediaKind.Music),
        LibraryTab(R.string.favorites, null)
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val selectedKind = tabs[selectedTab].kind
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (selectedKind == null) viewModel.loadFavorites() else viewModel.load(selectedKind)
    }

    LaunchedEffect(selectedTab) {
        if (selectedKind == null) viewModel.loadFavorites() else viewModel.load(selectedKind)
    }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.library), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(stringResource(tab.labelRes)) }
                )
            }
        }
        message?.let { StatusCard(stringResource(R.string.cast_to_tv), it) }
        when (val current = state) {
            UiState.Initial, UiState.Loading -> LoadingPanel()
            UiState.Empty -> EmptyPanel(stringResource(R.string.no_media_found))
            is UiState.Error -> ErrorPanel(current.error) { if (selectedKind == null) viewModel.loadFavorites() else viewModel.load(selectedKind) }
            is UiState.PermissionRequired -> {
                StatusCard(stringResource(R.string.permission_required), stringResource(R.string.media_permission_body))
                Button(onClick = { launcher.launch(viewModel.permissionsFor(selectedKind).toTypedArray()) }) {
                    Text(stringResource(R.string.allow_access))
                }
            }
            is UiState.Content -> LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.search), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${stringResource(R.string.sort)}: ${stringResource(R.string.recently_played)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(current.value, key = { it.uri.toString() }) { item ->
                    MediaRow(
                        item = item,
                        onPlay = { },
                        onCast = { viewModel.cast(item) },
                        onFavorite = { viewModel.toggleFavorite(item) }
                    )
                }
            }
            UiState.Offline -> StatusCard(stringResource(R.string.offline), stringResource(R.string.no_media_found))
        }
    }
}

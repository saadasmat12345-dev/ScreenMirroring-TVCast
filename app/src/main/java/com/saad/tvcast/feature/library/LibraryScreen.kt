package com.saad.tvcast.feature.library

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.saad.tvcast.core.designsystem.component.ScreenBackground
import com.saad.tvcast.core.designsystem.component.StatusCard
import com.saad.tvcast.core.designsystem.component.formatBytes
import com.saad.tvcast.core.designsystem.component.formatDuration
import com.saad.tvcast.core.media.LocalMediaRepository
import com.saad.tvcast.core.permissions.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
                val withFavorites = items.map { item ->
                    item.copy(isFavorite = favoriteDao.isFavorite(item.uri.toString()))
                }
                _state.value = if (withFavorites.isEmpty()) UiState.Empty else UiState.Content(withFavorites)
            }.onFailure {
                _state.value = UiState.Error(AppError.Unknown(it.message ?: "Media loading failed"))
            }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                favoriteDao.observeFavorites().first().map {
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
            }.onSuccess { items ->
                _state.value = if (items.isEmpty()) UiState.Empty else UiState.Content(items)
            }.onFailure {
                _state.value = UiState.Error(AppError.Unknown(it.message ?: "Favorites loading failed"))
            }
        }
    }

    fun toggleFavorite(item: LocalMediaItem, removeWhenUnfavorited: Boolean) {
        viewModelScope.launch {
            runCatching {
                val wasFavorite = favoriteDao.isFavorite(item.uri.toString())
                if (wasFavorite) {
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
                updateFavoriteInState(item.uri.toString(), nextFavorite = !wasFavorite, removeWhenUnfavorited)
            }.onFailure {
                _message.value = it.message ?: context.getString(R.string.error_state)
            }
        }
    }

    fun cast(item: LocalMediaItem) {
        viewModelScope.launch {
            runCatching {
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
            }.onFailure {
                _message.value = it.message ?: context.getString(R.string.cast_failed)
            }
        }
    }

    private fun updateFavoriteInState(
        mediaUri: String,
        nextFavorite: Boolean,
        removeWhenUnfavorited: Boolean
    ) {
        val current = _state.value as? UiState.Content ?: return
        val updated = current.value.mapNotNull { media ->
            if (media.uri.toString() != mediaUri) {
                media
            } else if (removeWhenUnfavorited && !nextFavorite) {
                null
            } else {
                media.copy(isFavorite = nextFavorite)
            }
        }
        _state.value = if (updated.isEmpty()) UiState.Empty else UiState.Content(updated)
    }
}

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onPlayMedia: (uri: String, title: String) -> Unit = { _, _ -> }
) {
    val tabs = listOf(
        LibraryTab(R.string.videos, MediaKind.Video),
        LibraryTab(R.string.photos, MediaKind.Photo),
        LibraryTab(R.string.music, MediaKind.Music),
        LibraryTab(R.string.favorites, null)
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val selectedKind = tabs[selectedTab].kind
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (selectedKind == null) viewModel.loadFavorites() else viewModel.load(selectedKind)
    }

    LaunchedEffect(selectedTab) {
        searchQuery = ""
        if (selectedKind == null) viewModel.loadFavorites() else viewModel.load(selectedKind)
    }

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                stringResource(R.string.library),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            LibraryHeroCard(kind = selectedKind, totalCount = (state as? UiState.Content)?.value?.size ?: 0)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
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
                is UiState.Error -> ErrorPanel(current.error) {
                    if (selectedKind == null) viewModel.loadFavorites() else viewModel.load(selectedKind)
                }
                is UiState.PermissionRequired -> {
                    StatusCard(
                        stringResource(R.string.permission_required),
                        stringResource(R.string.media_permission_body)
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { launcher.launch(viewModel.permissionsFor(selectedKind).toTypedArray()) }
                    ) {
                        Text(stringResource(R.string.allow_access))
                    }
                }
                is UiState.Content -> {
                    val filteredItems = remember(current.value, searchQuery) {
                        current.value.filterByQuery(searchQuery)
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            LibrarySearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it }
                            )
                        }
                        item {
                            LibrarySummaryRow(
                                count = filteredItems.size,
                                totalCount = current.value.size,
                                selectedKind = selectedKind
                            )
                        }
                        if (filteredItems.isEmpty()) {
                            item { EmptyPanel(stringResource(R.string.no_search_results)) }
                        } else {
                            items(filteredItems, key = { it.uri.toString() }) { item ->
                                LibraryMediaCard(
                                    item = item,
                                    onPlay = { onPlayMedia(item.uri.toString(), item.displayName) },
                                    onCast = { viewModel.cast(item) },
                                    onFavorite = {
                                        viewModel.toggleFavorite(
                                            item = item,
                                            removeWhenUnfavorited = selectedKind == null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                UiState.Offline -> StatusCard(stringResource(R.string.offline), stringResource(R.string.no_media_found))
            }
        }
    }
}

@Composable
private fun LibraryHeroCard(kind: MediaKind?, totalCount: Int) {
    val accent = libraryAccent(kind)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent)))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = accent.copy(alpha = 0.16f),
                contentColor = accent
            ) {
                Icon(kindIcon(kind), contentDescription = null, modifier = Modifier.padding(14.dp).size(28.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = libraryTitle(kind),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = libraryBody(kind),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(stringResource(R.string.media_items_found, totalCount)) }
            )
        }
    }
}

@Composable
private fun LibrarySearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        label = { Text(stringResource(R.string.library_search_hint)) }
    )
}

@Composable
private fun LibrarySummaryRow(count: Int, totalCount: Int, selectedKind: MediaKind?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.media_items_found, count)) }
        )
        AssistChip(
            onClick = {},
            leadingIcon = { Icon(Icons.Outlined.Sort, contentDescription = null, modifier = Modifier.size(18.dp)) },
            label = { Text(stringResource(R.string.sort_recent_first)) }
        )
        if (count != totalCount) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = libraryTitle(selectedKind),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LibraryMediaCard(
    item: LocalMediaItem,
    onPlay: () -> Unit,
    onCast: () -> Unit,
    onFavorite: () -> Unit
) {
    val accent = libraryAccent(item.kind)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaThumbnail(item = item, accent = accent)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = primaryMeta(item),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.secondaryMeta(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onFavorite) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite),
                            tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        onClick = onPlay
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.play), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        onClick = onCast
                    ) {
                        Icon(Icons.Outlined.Cast, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.cast), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(item: LocalMediaItem, accent: Color) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(kindIcon(item.kind), contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        if (item.kind != MediaKind.Photo && item.durationMillis != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp),
                color = Color.Black.copy(alpha = 0.42f),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Text(
                    text = formatDuration(item.durationMillis),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun libraryTitle(kind: MediaKind?): String = when (kind) {
    MediaKind.Video -> stringResource(R.string.videos)
    MediaKind.Photo -> stringResource(R.string.photos)
    MediaKind.Music -> stringResource(R.string.music)
    MediaKind.WebVideo -> stringResource(R.string.web_video_cast)
    null -> stringResource(R.string.favorites)
}

@Composable
private fun libraryBody(kind: MediaKind?): String = when (kind) {
    MediaKind.Video -> stringResource(R.string.library_videos_body)
    MediaKind.Photo -> stringResource(R.string.library_photos_body)
    MediaKind.Music -> stringResource(R.string.library_music_body)
    MediaKind.WebVideo -> stringResource(R.string.web_video_cast_subtitle)
    null -> stringResource(R.string.library_favorites_body)
}

@Composable
private fun libraryAccent(kind: MediaKind?): Color = when (kind) {
    MediaKind.Video -> MaterialTheme.colorScheme.primary
    MediaKind.Photo -> MaterialTheme.colorScheme.tertiary
    MediaKind.Music -> MaterialTheme.colorScheme.secondary
    MediaKind.WebVideo -> MaterialTheme.colorScheme.primary
    null -> MaterialTheme.colorScheme.error
}

private fun kindIcon(kind: MediaKind?): ImageVector = when (kind) {
    MediaKind.Video -> Icons.Outlined.VideoLibrary
    MediaKind.Photo -> Icons.Outlined.PhotoLibrary
    MediaKind.Music -> Icons.Outlined.MusicNote
    MediaKind.WebVideo -> Icons.Outlined.Cast
    null -> Icons.Outlined.Favorite
}

private fun List<LocalMediaItem>.filterByQuery(query: String): List<LocalMediaItem> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return filter { item ->
        item.displayName.contains(normalized, ignoreCase = true) ||
            item.album.orEmpty().contains(normalized, ignoreCase = true) ||
            item.artist.orEmpty().contains(normalized, ignoreCase = true) ||
            item.folder.orEmpty().contains(normalized, ignoreCase = true)
    }
}

@Composable
private fun primaryMeta(item: LocalMediaItem): String {
    return when (item.kind) {
        MediaKind.Video -> item.folder.orEmpty().ifBlank {
            item.mimeType.orEmpty().ifBlank { stringResource(R.string.videos) }
        }
        MediaKind.Photo -> item.folder.orEmpty().ifBlank {
            item.mimeType.orEmpty().ifBlank { stringResource(R.string.photos) }
        }
        MediaKind.Music -> listOfNotNull(item.artist?.takeIf(String::isNotBlank), item.album?.takeIf(String::isNotBlank))
            .joinToString(" - ")
            .ifBlank { item.mimeType.orEmpty().ifBlank { stringResource(R.string.music) } }
        MediaKind.WebVideo -> item.mimeType.orEmpty().ifBlank { stringResource(R.string.web_video_cast) }
    }
}

private fun LocalMediaItem.secondaryMeta(): String {
    val parts = buildList {
        if (sizeBytes > 0) add(formatBytes(sizeBytes))
        durationMillis?.takeIf { it > 0 }?.let { add(formatDuration(it)) }
        formatDate(dateAddedMillis).takeIf(String::isNotBlank)?.let(::add)
    }
    return parts.joinToString(" | ")
}

private fun formatDate(millis: Long): String {
    if (millis <= 0) return ""
    return runCatching {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
    }.getOrDefault("")
}

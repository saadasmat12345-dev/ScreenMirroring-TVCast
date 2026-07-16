package com.saad.tvcast.feature.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.saad.tvcast.BuildConfig
import com.saad.tvcast.R
import com.saad.tvcast.core.browser.WebVideoDetector
import com.saad.tvcast.core.common.WebVideoCandidate
import com.saad.tvcast.core.database.BrowserDao
import com.saad.tvcast.core.database.BrowserHistoryEntity
import com.saad.tvcast.core.designsystem.component.DeviceRow
import com.saad.tvcast.core.designsystem.component.EmptyPanel
import com.saad.tvcast.core.designsystem.component.StatusCard
import com.saad.tvcast.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BrowserUiState(
    val address: String = BuildConfig.BROWSER_DEFAULT_HOME,
    val homepage: String = BuildConfig.BROWSER_DEFAULT_HOME,
    val desktopMode: Boolean = false,
    val detectedVideos: List<WebVideoCandidate> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val detector: WebVideoDetector,
    private val browserDao: BrowserDao,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState
    val history = browserDao.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val bookmarks = browserDao.observeBookmarks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(homepage = settings.browserHomepage)
            }
        }
    }

    fun updateAddress(value: String) {
        _uiState.value = _uiState.value.copy(address = value)
    }

    fun navigateInput(): String {
        val value = uiState.value.address.trim()
        val url = when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.contains('.') && !value.contains(' ') -> "https://$value"
            else -> "https://www.google.com/search?q=${Uri.encode(value)}"
        }
        _uiState.value = _uiState.value.copy(address = url)
        return url
    }

    fun setDesktopMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(desktopMode = enabled)
    }

    fun onPageStarted(url: String?) {
        if (url != null) _uiState.value = _uiState.value.copy(address = url, message = null)
    }

    fun onPageFinished(title: String?, url: String?) {
        if (url == null) return
        viewModelScope.launch {
            browserDao.addHistory(BrowserHistoryEntity(title = title, url = url, visitedAt = System.currentTimeMillis()))
        }
    }

    fun detect(url: String, mimeType: String? = null, title: String? = null) {
        val candidate = detector.detectFromRequest(url, mimeType, title) ?: return
        val existing = uiState.value.detectedVideos
        if (existing.none { it.url == candidate.url }) {
            _uiState.value = uiState.value.copy(detectedVideos = existing + candidate)
        }
    }

    fun blockUrl(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    fun clearData(webView: WebView?) {
        webView?.clearHistory()
        webView?.clearCache(true)
        viewModelScope.launch { browserDao.clearHistory() }
        _uiState.value = _uiState.value.copy(detectedVideos = emptyList())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(viewModel: BrowserViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showVideos by remember { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf(state.homepage) }

    LaunchedEffect(Unit) { pendingUrl = state.homepage }

    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.address,
                onValueChange = viewModel::updateAddress,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(R.string.address_or_search)) }
            )
            Button(onClick = { pendingUrl = viewModel.navigateInput() }) { Text(stringResource(R.string.go)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { webView?.goBack() }) { Icon(Icons.Outlined.ArrowBack, stringResource(R.string.back)) }
            IconButton(onClick = { webView?.goForward() }) { Icon(Icons.Outlined.ArrowForward, stringResource(R.string.forward)) }
            IconButton(onClick = { webView?.reload() }) { Icon(Icons.Outlined.Refresh, stringResource(R.string.refresh)) }
            IconButton(onClick = { pendingUrl = state.homepage }) { Icon(Icons.Outlined.Home, stringResource(R.string.homepage)) }
            TextButton(onClick = { showVideos = true }) { Text("${stringResource(R.string.detected_videos)} (${state.detectedVideos.size})") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.desktop_mode))
            Switch(checked = state.desktopMode, onCheckedChange = viewModel::setDesktopMode)
            TextButton(onClick = { viewModel.clearData(webView) }) { Text(stringResource(R.string.clear_browser_data)) }
        }
        state.message?.let { StatusCard(stringResource(R.string.browser), stringResource(R.string.blocked_url)) }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = {
                WebView(context).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.allowFileAccessFromFileURLs = false
                    settings.allowUniversalAccessFromFileURLs = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val scheme = request.url.scheme?.lowercase()
                            if (scheme != "http" && scheme != "https") {
                                viewModel.blockUrl(context.getString(R.string.blocked_url))
                                return true
                            }
                            return false
                        }

                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            viewModel.detect(request.url.toString(), request.requestHeaders["Accept"], view.title)
                            return null
                        }

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            viewModel.onPageStarted(url)
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            viewModel.onPageFinished(view.title, url)
                            view.evaluateJavascript(
                                "Array.from(document.querySelectorAll('video,source')).map(e=>e.currentSrc||e.src).filter(Boolean).join('\\n')"
                            ) { result ->
                                result.trim('"').replace("\\n", "\n").lineSequence()
                                    .filter { it.isNotBlank() }
                                    .forEach { viewModel.detect(it, null, view.title) }
                            }
                        }

                        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                            handler.cancel()
                        }
                    }
                    loadUrl(pendingUrl)
                }
            },
            update = { view ->
                view.settings.userAgentString = if (state.desktopMode) {
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36"
                } else {
                    WebSettings.getDefaultUserAgent(context)
                }
                if (pendingUrl != view.url) view.loadUrl(pendingUrl)
            }
        )
    }

    if (showVideos) {
        ModalBottomSheet(onDismissRequest = { showVideos = false }) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.detected_videos))
                if (state.detectedVideos.isEmpty()) {
                    EmptyPanel(stringResource(R.string.no_detected_videos))
                } else {
                    state.detectedVideos.forEach { video ->
                        DeviceRow(
                            name = video.title ?: video.url.substringAfterLast('/'),
                            detail = listOfNotNull(video.mimeType, video.qualityLabel, if (video.isSupported) null else video.unsupportedReason).joinToString(" | "),
                            onClick = { }
                        )
                    }
                }
                StatusCard(stringResource(R.string.help), stringResource(R.string.unsupported_protected_media))
            }
        }
    }
}

package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Song
import com.example.data.model.SongSource

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebMusicBrowser(
    onPlayTrackFromWeb: (Song) -> Unit,
    onSaveToPlaylist: (Song) -> Unit,
    onDownloadOffline: (Song) -> Unit,
    modifier: Modifier = Modifier,
    initialUrl: String = "https://m.youtube.com"
) {
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var inputUrlText by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("YouTube Mobile") }
    var isLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val focusManager = LocalFocusManager.current

    // Search Engines & Presets
    var selectedEngine by remember { mutableStateOf("YouTube") } // YouTube, YouTube Music, Google, SoundCloud, Spotify Web
    
    val searchEngines = listOf(
        Triple("YouTube", "https://m.youtube.com/results?search_query=", Color(0xFFFF0000)),
        Triple("YT Music", "https://music.youtube.com/search?q=", Color(0xFFFF2A2A)),
        Triple("Google Music", "https://www.google.com/search?q=listen+song+", Color(0xFF4285F4)),
        Triple("SoundCloud", "https://m.soundcloud.com/search?q=", Color(0xFFFF5500)),
        Triple("Free Music", "https://freemusicarchive.org/search?quicksearch=", Color(0xFF00E5FF))
    )

    fun executeSearch(query: String) {
        focusManager.clearFocus()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val destination = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
            when (selectedEngine) {
                "YouTube" -> "https://m.youtube.com/results?search_query=$encoded"
                "YT Music" -> "https://music.youtube.com/search?q=$encoded"
                "Google Music" -> "https://www.google.com/search?q=listen+song+$encoded"
                "SoundCloud" -> "https://m.soundcloud.com/search?q=$encoded"
                "Free Music" -> "https://freemusicarchive.org/search?quicksearch=$encoded"
                else -> "https://m.youtube.com/results?search_query=$encoded"
            }
        }
        currentUrl = destination
        inputUrlText = destination
        webViewInstance?.loadUrl(destination)
    }

    // Handle back button for web navigation
    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    // Helper to create a song object from the active webpage
    val currentCapturedSong = remember(pageTitle, currentUrl) {
        val cleanTitle = if (pageTitle.isNotBlank() && pageTitle != "about:blank") {
            pageTitle.replace(" - YouTube", "").replace(" | SoundCloud", "").replace(" - Google Search", "")
        } else {
            "Web Track: " + currentUrl.takeLast(25)
        }

        val source = when {
            currentUrl.contains("youtube.com") || currentUrl.contains("youtu.be") -> SongSource.YOUTUBE
            currentUrl.contains("google.com") -> SongSource.GOOGLE_SEARCH
            else -> SongSource.STREAM
        }

        val youtubeId = if (currentUrl.contains("v=")) {
            currentUrl.substringAfter("v=").substringBefore("&")
        } else if (currentUrl.contains("youtu.be/")) {
            currentUrl.substringAfter("youtu.be/").substringBefore("?")
        } else null

        val cover = when (source) {
            SongSource.YOUTUBE -> {
                if (youtubeId != null) "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"
                else "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
            }
            SongSource.GOOGLE_SEARCH -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
            else -> "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
        }

        Song(
            id = "web_${currentUrl.hashCode()}",
            title = cleanTitle,
            artist = if (source == SongSource.YOUTUBE) "YouTube Web Audio" else "Web Audio Stream",
            album = "Web Browser Discovery",
            durationMs = 210000L,
            coverUrl = cover,
            streamUrl = currentUrl,
            source = source,
            youtubeId = youtubeId,
            fileSize = 4200000L,
            genre = "Web Browser"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("web_music_browser")
    ) {
        // Top Browser Navigation Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                // Address Bar & Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isLoading) webViewInstance?.stopLoading()
                            else webViewInstance?.reload()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = "Reload",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // URL / Search Input
                    OutlinedTextField(
                        value = inputUrlText,
                        onValueChange = { inputUrlText = it },
                        singleLine = true,
                        placeholder = { Text("Search songs on $selectedEngine or type URL...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (inputUrlText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { inputUrlText = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { executeSearch(inputUrlText) },
                                    modifier = Modifier.size(32.dp).testTag("browser_search_submit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("browser_url_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            executeSearch(inputUrlText)
                        })
                    )
                }

                // Quick Engine Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Engine:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    searchEngines.forEach { (name, urlPrefix, color) ->
                        val isSelected = selectedEngine == name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedEngine = name
                                if (inputUrlText.isNotBlank() && !inputUrlText.startsWith("http")) {
                                    executeSearch(inputUrlText)
                                } else {
                                    val homeUrl = when (name) {
                                        "YouTube" -> "https://m.youtube.com"
                                        "YT Music" -> "https://music.youtube.com"
                                        "Google Music" -> "https://www.google.com/search?q=popular+songs"
                                        "SoundCloud" -> "https://m.soundcloud.com"
                                        "Free Music" -> "https://freemusicarchive.org"
                                        else -> "https://m.youtube.com"
                                    }
                                    inputUrlText = homeUrl
                                    currentUrl = homeUrl
                                    webViewInstance?.loadUrl(homeUrl)
                                }
                            },
                            label = { Text(name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                // Trending Search Suggestions (1-tap search)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val trendingQueries = listOf(
                        "Top Hits 2024", "Lo-Fi Beats", "Synthwave", "Chill Vibes", "Acoustic Pop", "EDM Party", "Rock Classics"
                    )
                    trendingQueries.forEach { query ->
                        SuggestionChip(
                            onClick = {
                                inputUrlText = query
                                executeSearch(query)
                            },
                            label = { Text(query, fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }
            }
        }

        // Loading Progress Bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadingProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Web Capture Action Bar (Floating at top of webpage)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = currentCapturedSong.title,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Detected Web Media",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Play Now
                    FilledTonalButton(
                        onClick = { onPlayTrackFromWeb(currentCapturedSong) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("web_play_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", fontSize = 11.sp)
                    }

                    // Save to Playlist
                    FilledTonalButton(
                        onClick = { onSaveToPlaylist(currentCapturedSong) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("web_save_playlist_button")
                    ) {
                        Icon(Icons.Outlined.PlaylistAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 11.sp)
                    }

                    // Download Offline
                    FilledTonalButton(
                        onClick = { onDownloadOffline(currentCapturedSong) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("web_download_button")
                    ) {
                        Icon(Icons.Outlined.Download, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // In-App Android WebView
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mediaPlaybackRequiresUserGesture = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            url?.let {
                                currentUrl = it
                                inputUrlText = it
                            }
                            canGoBack = canGoBack()
                            canGoForward = canGoForward()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            view?.title?.let { if (it.isNotBlank()) pageTitle = it }
                            canGoBack = canGoBack()
                            canGoForward = canGoForward()
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val uri = request?.url ?: return false
                            val scheme = uri.scheme?.lowercase() ?: return false
                            if (scheme != "http" && scheme != "https") {
                                return try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ctx.startActivity(intent)
                                    true
                                } catch (e: Exception) {
                                    true // Consume to prevent ActivityNotFoundException crash
                                }
                            }
                            return false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            isLoading = false
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?
                        ): Boolean {
                            // Recover gracefully from chromium renderer crash without crashing host app
                            view?.let {
                                (it.parent as? ViewGroup)?.removeView(it)
                                it.destroy()
                            }
                            return true
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            loadingProgress = newProgress
                            if (newProgress == 100) isLoading = false
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { if (it.isNotBlank()) pageTitle = it }
                        }
                    }

                    loadUrl(currentUrl)
                }.also { webViewInstance = it }
            },
            update = { view ->
                webViewInstance = view
            },
            onRelease = { view ->
                view.stopLoading()
                view.loadUrl("about:blank")
                view.destroy()
            }
        )
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.data.model.SongSource
import com.example.ui.components.SongListItem
import com.example.ui.components.WebMusicBrowser
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.SearchUiState

enum class SearchMode {
    WEB_BROWSER, INSTANT_SEARCH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onSongSelected: (Song, List<Song>) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchMode by remember { mutableStateOf(SearchMode.WEB_BROWSER) }
    var showOpenLinkDialog by remember { mutableStateOf(false) }
    var browserUrlToLoad by remember { mutableStateOf("https://m.youtube.com") }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val sourceFilter by viewModel.searchSourceFilter.collectAsState()
    val searchState by viewModel.searchUiState.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val downloadProgressMap by viewModel.downloadManager.downloadProgressMap.collectAsState()

    val focusManager = LocalFocusManager.current

    val genreKeywords = listOf(
        "Synthwave", "Lo-Fi", "EDM", "Pop", "Rock", "Jazz", "Hip-Hop", "Chillout", "Acoustic", "Classical"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        // Mode Switcher Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (searchMode == SearchMode.WEB_BROWSER) "In-App Music Browser" else "Instant Search",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (searchMode == SearchMode.WEB_BROWSER) "Google & YouTube (No API Needed)" else "Offline & Stream Catalog",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Quick Link Button
                    IconButton(
                        onClick = { showOpenLinkDialog = true },
                        modifier = Modifier.testTag("open_link_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Open Link",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Mode Toggle Button Group
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = searchMode == SearchMode.WEB_BROWSER,
                            onClick = { searchMode = SearchMode.WEB_BROWSER },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Icon(Icons.Outlined.Language, null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Browser", fontSize = 11.sp) },
                            modifier = Modifier.testTag("tab_browser_mode")
                        )
                        SegmentedButton(
                            selected = searchMode == SearchMode.INSTANT_SEARCH,
                            onClick = { searchMode = SearchMode.INSTANT_SEARCH },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Tracks", fontSize = 11.sp) },
                            modifier = Modifier.testTag("tab_tracks_mode")
                        )
                    }
                }
            }
        }

        // Mode Content
        if (searchMode == SearchMode.WEB_BROWSER) {
            // Full In-App Web Browser (Google & YouTube)
            WebMusicBrowser(
                initialUrl = browserUrlToLoad,
                onPlayTrackFromWeb = { song ->
                    onSongSelected(song, listOf(song))
                },
                onSaveToPlaylist = { song ->
                    onAddToPlaylistClick(song)
                },
                onDownloadOffline = { song ->
                    viewModel.downloadSong(song)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Instant Catalog Search Mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Search Text Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search songs, artists, YouTube audio, Google...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        viewModel.performSearch()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Genre Suggestions (if idle)
                if (searchQuery.isBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        genreKeywords.forEach { genre ->
                            SuggestionChip(
                                onClick = { viewModel.onSearchQueryChanged(genre) },
                                label = { Text(genre, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Results or Trending Section
                when (val state = searchState) {
                    is SearchUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Searching across Google & YouTube...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is SearchUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Search Error", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(state.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { viewModel.performSearch() }) {
                                    Text("Retry Search")
                                }
                            }
                        }
                    }
                    is SearchUiState.Success -> {
                        val songs = state.songs
                        if (songs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No songs found for \"${state.query}\". Try another query or switch to Browser mode.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Results for \"${state.query}\" (${songs.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(songs, key = { it.id }) { song ->
                                    val isCurrent = playerState.currentSong?.id == song.id
                                    val isPlaying = isCurrent && playerState.isPlaying
                                    val progress = downloadProgressMap[song.id]

                                    SongListItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = isCurrent,
                                        downloadProgress = progress,
                                        onPlay = { onSongSelected(song, songs) },
                                        onToggleFavorite = { viewModel.toggleFavorite(song) },
                                        onDownload = { viewModel.downloadSong(song) },
                                        onAddToPlaylist = { onAddToPlaylistClick(song) }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                    }
                    SearchUiState.Idle -> {
                        // Trending / Featured Music Section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trending Global Hits",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Live Streams",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(trendingSongs, key = { it.id }) { song ->
                                val isCurrent = playerState.currentSong?.id == song.id
                                val isPlaying = isCurrent && playerState.isPlaying
                                val progress = downloadProgressMap[song.id]

                                SongListItem(
                                    song = song,
                                    isPlaying = isPlaying,
                                    isCurrent = isCurrent,
                                    downloadProgress = progress,
                                    onPlay = { onSongSelected(song, trendingSongs) },
                                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                                    onDownload = { viewModel.downloadSong(song) },
                                    onAddToPlaylist = { onAddToPlaylistClick(song) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }

        // Open Link / URL Dialog
        if (showOpenLinkDialog) {
            com.example.ui.components.OpenLinkDialog(
                onPlaySong = { song ->
                    onSongSelected(song, listOf(song))
                },
                onSaveToPlaylist = { song ->
                    onAddToPlaylistClick(song)
                },
                onDownloadOffline = { song ->
                    viewModel.downloadSong(song)
                },
                onOpenInBrowser = { url ->
                    browserUrlToLoad = url
                    searchMode = SearchMode.WEB_BROWSER
                },
                onDismiss = { showOpenLinkDialog = false }
            )
        }
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.viewmodel.MusicViewModel

enum class MainNavTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    SEARCH("Search", Icons.Default.Search, Icons.Outlined.Search),
    LIBRARY("Library", Icons.Default.LibraryMusic, Icons.Outlined.LibraryMusic),
    CLOUD_SYNC("Cloud Sync", Icons.Default.CloudSync, Icons.Outlined.CloudSync)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(MainNavTab.SEARCH) }
    var activePlaylistDetail by remember { mutableStateOf<Playlist?>(null) }
    var isFullPlayerExpanded by remember { mutableStateOf(false) }
    var songForAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    val playerState by viewModel.playerState.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val pendingSharedLink by viewModel.pendingSharedLink.collectAsState()

    LaunchedEffect(pendingSharedLink) {
        if (pendingSharedLink != null) {
            isFullPlayerExpanded = true
        }
    }

    // Handle Android back button
    BackHandler(enabled = isFullPlayerExpanded || activePlaylistDetail != null) {
        if (isFullPlayerExpanded) {
            isFullPlayerExpanded = false
        } else if (activePlaylistDetail != null) {
            activePlaylistDetail = null
        }
    }

    Scaffold(
        bottomBar = {
            if (!isFullPlayerExpanded) {
                Column {
                    // Mini Player Bar
                    MiniPlayerBar(
                        currentSong = playerState.currentSong,
                        isPlaying = playerState.isPlaying,
                        progressMs = playerState.currentPositionMs,
                        durationMs = playerState.durationMs,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.playNext() },
                        onOpenFullPlayer = { isFullPlayerExpanded = true }
                    )

                    // Bottom Navigation Bar
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        MainNavTab.entries.forEach { tab ->
                            val isSelected = currentTab == tab && activePlaylistDetail == null
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    activePlaylistDetail = null
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen Content
            if (activePlaylistDetail != null) {
                PlaylistDetailScreen(
                    playlist = activePlaylistDetail!!,
                    viewModel = viewModel,
                    onBack = { activePlaylistDetail = null },
                    onSongSelected = { song, queue ->
                        viewModel.playSong(song, queue)
                    },
                    onAddToPlaylistClick = { song ->
                        songForAddToPlaylist = song
                    }
                )
            } else {
                when (currentTab) {
                    MainNavTab.SEARCH -> {
                        SearchScreen(
                            viewModel = viewModel,
                            onSongSelected = { song, queue ->
                                viewModel.playSong(song, queue)
                            },
                            onAddToPlaylistClick = { song ->
                                songForAddToPlaylist = song
                            }
                        )
                    }
                    MainNavTab.LIBRARY -> {
                        LibraryScreen(
                            viewModel = viewModel,
                            onPlaylistClick = { playlist ->
                                activePlaylistDetail = playlist
                            },
                            onSongSelected = { song, queue ->
                                viewModel.playSong(song, queue)
                            },
                            onAddToPlaylistClick = { song ->
                                songForAddToPlaylist = song
                            }
                        )
                    }
                    MainNavTab.CLOUD_SYNC -> {
                        CloudSyncScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Fullscreen Now Playing Modal Sheet
            AnimatedVisibility(
                visible = isFullPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NowPlayingScreen(
                    viewModel = viewModel,
                    onClose = { isFullPlayerExpanded = false },
                    onAddToPlaylistClick = { song ->
                        songForAddToPlaylist = song
                    }
                )
            }

            // Add To Playlist Global Dialog
            songForAddToPlaylist?.let { song ->
                AddToPlaylistDialog(
                    song = song,
                    playlists = allPlaylists,
                    onAddToPlaylist = { playlistId ->
                        viewModel.addSongToPlaylist(playlistId, song)
                        songForAddToPlaylist = null
                    },
                    onCreatePlaylist = { name ->
                        viewModel.createPlaylist(name)
                    },
                    onDismiss = { songForAddToPlaylist = null }
                )
            }
        }
    }
}

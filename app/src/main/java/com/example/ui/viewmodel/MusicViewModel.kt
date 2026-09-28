package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.TrackEntity
import com.example.data.model.*
import com.example.data.offline.OfflineDownloadManager
import com.example.data.remote.MusicSearchRepository
import com.example.data.sync.CloudSyncManager
import com.example.player.MusicPlayerEngine
import com.example.player.PlayerUiState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.UUID

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val songs: List<Song>, val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("MusicViewModel", "Handled background exception safely: ${throwable.message}", throwable)
    }

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val dao = db.musicDao()
    private val searchRepo = MusicSearchRepository()

    val downloadManager = OfflineDownloadManager(application, dao, viewModelScope)
    val syncManager = CloudSyncManager(application, dao, viewModelScope)
    val playerEngine = MusicPlayerEngine(application, viewModelScope)

    // Player State
    val playerState: StateFlow<PlayerUiState> = playerEngine.uiState

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSourceFilter = MutableStateFlow<SongSource?>(null)
    val searchSourceFilter: StateFlow<SongSource?> = _searchSourceFilter.asStateFlow()

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()

    // Library State
    val allPlaylists: StateFlow<List<Playlist>> = dao.getAllPlaylists()
        .map { list -> list.map { it.toPlaylist() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = dao.getFavoriteTracks()
        .map { list -> list.map { it.toSong() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSongs: StateFlow<List<Song>> = dao.getDownloadedTracks()
        .map { list -> list.map { it.toSong() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSongs: StateFlow<List<Song>> = dao.getRecentlyPlayedTracks()
        .map { list -> list.map { it.toSong() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Playlist detail
    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPlaylistTracks: StateFlow<List<Song>> = _selectedPlaylistId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else if (id == "playlist_favorites") dao.getFavoriteTracks().map { it.map { t -> t.toSong() } }
        else if (id == "playlist_offline_hits") dao.getDownloadedTracks().map { it.map { t -> t.toSong() } }
        else dao.getTracksForPlaylist(id).map { it.map { t -> t.toSong() } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Storage Used
    private val _storageUsedBytes = MutableStateFlow(0L)
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes.asStateFlow()

    // Offline Mode Filter in UI
    private val _isOfflineOnlyMode = MutableStateFlow(false)
    val isOfflineOnlyMode: StateFlow<Boolean> = _isOfflineOnlyMode.asStateFlow()

    // Incoming Shared Link State
    private val _pendingSharedLink = MutableStateFlow<String?>(null)
    val pendingSharedLink: StateFlow<String?> = _pendingSharedLink.asStateFlow()

    fun handleIncomingLink(rawUrlOrText: String) {
        val extracted = com.example.util.LinkParser.extractUrlFromText(rawUrlOrText) ?: rawUrlOrText.trim()
        if (extracted.isNotBlank()) {
            _pendingSharedLink.value = extracted
            // Automatically parse and start playing!
            val song = com.example.util.LinkParser.parseUrlToSong(extracted)
            playSong(song, listOf(song))
        }
    }

    fun clearPendingSharedLink() {
        _pendingSharedLink.value = null
    }

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(exceptionHandler) {
            _trendingSongs.value = searchRepo.getTrendingTracks()
            updateStorageMetrics()
        }
    }

    fun updateStorageMetrics() {
        viewModelScope.launch(exceptionHandler) {
            _storageUsedBytes.value = downloadManager.getDownloadedStorageBytes()
        }
    }

    fun setOfflineOnlyMode(enabled: Boolean) {
        _isOfflineOnlyMode.value = enabled
    }

    // SEARCH
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchUiState.value = SearchUiState.Idle
            return
        }
        performSearch(query, _searchSourceFilter.value)
    }

    fun setSearchSourceFilter(source: SongSource?) {
        _searchSourceFilter.value = source
        performSearch(_searchQuery.value, source)
    }

    fun performSearch(query: String = _searchQuery.value, filter: SongSource? = _searchSourceFilter.value) {
        if (query.isBlank()) {
            _searchUiState.value = SearchUiState.Idle
            return
        }

        viewModelScope.launch(exceptionHandler) {
            _searchUiState.value = SearchUiState.Loading
            try {
                val results = searchRepo.searchMusic(query, filter)
                // Mark if already favorite or downloaded in DB
                val favIds = dao.getFavoriteTracks().first().map { it.id }.toSet()
                val downloadedIds = dao.getDownloadedTracks().first().map { it.id }.toSet()

                val mappedResults = results.map { song ->
                    song.copy(
                        isFavorite = favIds.contains(song.id),
                        isDownloaded = downloadedIds.contains(song.id)
                    )
                }
                _searchUiState.value = SearchUiState.Success(mappedResults, query)
            } catch (e: Exception) {
                _searchUiState.value = SearchUiState.Error(e.localizedMessage ?: "Search failed")
            }
        }
    }

    // PLAYBACK
    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                // Save to DB track cache and record history
                dao.insertTrack(TrackEntity.fromSong(song))
                dao.insertHistory(com.example.data.local.PlaybackHistoryEntity(trackId = song.id, playedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.w("MusicViewModel", "Failed to cache track history: ${e.message}")
            }
        }
        playerEngine.playSong(song, queue)
    }

    fun togglePlayPause() = playerEngine.togglePlayPause()
    fun playNext() = playerEngine.next()
    fun playPrevious() = playerEngine.previous()
    fun seekTo(positionMs: Long) = playerEngine.seekTo(positionMs)
    fun skipForward10s() = playerEngine.skipForward10s()
    fun skipBackward10s() = playerEngine.skipBackward10s()
    fun toggleRepeat() = playerEngine.toggleRepeatMode()
    fun toggleShuffle() = playerEngine.toggleShuffle()
    fun setPlaybackSpeed(speed: Float) = playerEngine.setPlaybackSpeed(speed)
    fun setEqualizerPreset(preset: EqualizerPreset) = playerEngine.setEqualizerPreset(preset)
    fun setBassBoost(level: Float) = playerEngine.setBassBoost(level)
    fun startSleepTimer(minutes: Int) = playerEngine.startSleepTimer(minutes)
    fun cancelSleepTimer() = playerEngine.cancelSleepTimer()
    fun addToQueueNext(song: Song) = playerEngine.addToQueueNext(song)
    fun removeFromQueue(index: Int) = playerEngine.removeFromQueue(index)

    // FAVORITES
    fun toggleFavorite(song: Song) {
        val newFav = !song.isFavorite
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            runCatching {
                dao.insertTrack(TrackEntity.fromSong(song, newFav))
                dao.updateFavorite(song.id, newFav)
            }
        }
    }

    // DOWNLOADS & OFFLINE
    fun downloadSong(song: Song) {
        downloadManager.downloadSong(song)
    }

    fun deleteDownloadedSong(songId: String) {
        viewModelScope.launch(exceptionHandler) {
            downloadManager.deleteDownloadedSong(songId)
            updateStorageMetrics()
        }
    }

    // PLAYLISTS MANAGEMENT
    fun selectPlaylist(playlistId: String?) {
        _selectedPlaylistId.value = playlistId
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        val id = "pl_" + UUID.randomUUID().toString().take(8)
        val covers = listOf(
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
        )
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            runCatching {
                val entity = PlaylistEntity(
                    id = id,
                    name = name.trim(),
                    description = description.trim(),
                    coverUrl = covers.random(),
                    createdAt = System.currentTimeMillis(),
                    isSynced = true,
                    isSystem = false
                )
                dao.insertPlaylist(entity)
            }
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            runCatching {
                dao.insertTrack(TrackEntity.fromSong(song))
                dao.insertPlaylistTrackCrossRef(
                    PlaylistTrackCrossRef(
                        playlistId = playlistId,
                        trackId = song.id,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            runCatching {
                dao.removeTrackFromPlaylist(playlistId, songId)
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            runCatching {
                dao.deletePlaylist(playlistId)
                dao.clearPlaylistTracks(playlistId)
                if (_selectedPlaylistId.value == playlistId) {
                    _selectedPlaylistId.value = null
                }
            }
        }
    }

    // CLOUD SYNC
    fun triggerSync(onComplete: ((Boolean, String) -> Unit)? = null) {
        syncManager.triggerCloudSync(onComplete)
    }

    fun pairWithSyncCode(code: String, onComplete: (Boolean, String) -> Unit) {
        syncManager.linkWithDeviceSyncCode(code, onComplete)
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.release()
    }
}

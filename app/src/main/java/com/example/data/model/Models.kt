package com.example.data.model

enum class SongSource(val displayName: String, val badgeColor: Long) {
    YOUTUBE("YouTube", 0xFFFF0000),
    GOOGLE_SEARCH("Google", 0xFF4285F4),
    STREAM("Web Stream", 0xFF00E5FF),
    LOCAL("Local / Offline", 0xFF10B981)
}

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class PlayerState {
    IDLE, BUFFERING, PLAYING, PAUSED, ERROR
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Single",
    val durationMs: Long = 180000L,
    val coverUrl: String,
    val streamUrl: String,
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val source: SongSource = SongSource.STREAM,
    val youtubeId: String? = null,
    val fileSize: Long = 0L,
    val lyrics: String? = null,
    val genre: String = "Pop"
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val coverUrl: String = "",
    val trackCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val isSystem: Boolean = false
)

data class LyricsLine(
    val timestampMs: Long,
    val text: String
)

data class DownloadProgress(
    val songId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progressPercent: Int,
    val isComplete: Boolean = false,
    val isFailed: Boolean = false,
    val error: String? = null
)

data class EqualizerPreset(
    val name: String,
    val bass: Float = 0f, // -10 to +10
    val lowMid: Float = 0f,
    val mid: Float = 0f,
    val highMid: Float = 0f,
    val treble: Float = 0f
)

data class SyncedDevice(
    val id: String,
    val name: String,
    val type: String, // "Phone", "Tablet", "Browser"
    val lastActive: String,
    val isCurrent: Boolean = false
)

data class CloudSyncState(
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val syncCode: String = "SW-8392-CLOUD",
    val connectedDevices: List<SyncedDevice> = emptyList(),
    val syncedPlaylistsCount: Int = 0,
    val syncedFavoritesCount: Int = 0,
    val lastSyncMessage: String = "Cloud Ready"
)

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val coverUrl: String,
    val createdAt: Long,
    val isSynced: Boolean,
    val isSystem: Boolean
) {
    fun toPlaylist(trackCount: Int = 0, totalDuration: Long = 0L): Playlist {
        return Playlist(
            id = id,
            name = name,
            description = description,
            coverUrl = coverUrl,
            trackCount = trackCount,
            totalDurationMs = totalDuration,
            createdAt = createdAt,
            isSynced = isSynced,
            isSystem = isSystem
        )
    }

    companion object {
        fun fromPlaylist(playlist: Playlist): PlaylistEntity {
            return PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                description = playlist.description,
                coverUrl = playlist.coverUrl,
                createdAt = playlist.createdAt,
                isSynced = playlist.isSynced,
                isSystem = playlist.isSystem
            )
        }
    }
}

package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Song
import com.example.data.model.SongSource

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["isFavorite"]),
        Index(value = ["isDownloaded"]),
        Index(value = ["downloadedAt"])
    ]
)
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String,
    val streamUrl: String,
    val localFilePath: String?,
    val isDownloaded: Boolean,
    val downloadedAt: Long?,
    val isFavorite: Boolean,
    val sourceName: String,
    val youtubeId: String?,
    val fileSize: Long,
    val lyrics: String?,
    val genre: String
) {
    fun toSong(): Song {
        val src = try {
            SongSource.valueOf(sourceName)
        } catch (e: Exception) {
            SongSource.STREAM
        }
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            coverUrl = coverUrl,
            streamUrl = streamUrl,
            localFilePath = localFilePath,
            isDownloaded = isDownloaded,
            isFavorite = isFavorite,
            source = if (isDownloaded && localFilePath != null) SongSource.LOCAL else src,
            youtubeId = youtubeId,
            fileSize = fileSize,
            lyrics = lyrics,
            genre = genre
        )
    }

    companion object {
        fun fromSong(song: Song, isFav: Boolean = song.isFavorite): TrackEntity {
            return TrackEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.durationMs,
                coverUrl = song.coverUrl,
                streamUrl = song.streamUrl,
                localFilePath = song.localFilePath,
                isDownloaded = song.isDownloaded,
                downloadedAt = if (song.isDownloaded) System.currentTimeMillis() else null,
                isFavorite = isFav,
                sourceName = song.source.name,
                youtubeId = song.youtubeId,
                fileSize = song.fileSize,
                lyrics = song.lyrics,
                genre = song.genre
            )
        }
    }
}

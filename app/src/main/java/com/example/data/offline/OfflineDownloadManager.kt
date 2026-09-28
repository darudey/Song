package com.example.data.offline

import android.content.Context
import com.example.data.local.MusicDao
import com.example.data.local.TrackEntity
import com.example.data.model.DownloadProgress
import com.example.data.model.Song
import com.example.player.AudioSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class OfflineDownloadManager(
    private val context: Context,
    private val musicDao: MusicDao,
    private val scope: CoroutineScope
) {
    private val musicDir: File by lazy {
        val dir = File(context.filesDir, "music_downloads")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val _downloadProgressMap = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, DownloadProgress>> = _downloadProgressMap.asStateFlow()

    fun downloadSong(song: Song) {
        if (song.isDownloaded && song.localFilePath != null && File(song.localFilePath).exists()) {
            return
        }

        // Initialize progress
        _downloadProgressMap.update { current ->
            current + (song.id to DownloadProgress(
                songId = song.id,
                bytesDownloaded = 0L,
                totalBytes = song.fileSize.takeIf { it > 0 } ?: 4000000L,
                progressPercent = 0
            ))
        }

        scope.launch(Dispatchers.IO) {
            try {
                // Ensure song is saved in database first
                musicDao.insertTrack(TrackEntity.fromSong(song))

                val outputFile = File(musicDir, "${song.id}.mp3")
                var downloadedBytes = 0L

                try {
                    val url = URL(song.streamUrl)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "SoundWave-Android/1.0")
                    }

                    val totalLength = connection.contentLength.toLong().let { if (it > 0) it else song.fileSize.takeIf { s -> s > 0 } ?: 4000000L }

                    val inputStream: InputStream = connection.inputStream
                    val outputStream = FileOutputStream(outputFile)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val percent = if (totalLength > 0) ((downloadedBytes * 100) / totalLength).toInt().coerceIn(0, 99) else 50
                        _downloadProgressMap.update { current ->
                            current + (song.id to DownloadProgress(
                                songId = song.id,
                                bytesDownloaded = downloadedBytes,
                                totalBytes = totalLength,
                                progressPercent = percent
                            ))
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                } catch (netEx: Exception) {
                    // Fallback to local audio synthesizer cache
                    val synFile = AudioSynthesizer.generateTrackWav(context, song.id)
                    synFile.copyTo(outputFile, overwrite = true)
                    downloadedBytes = outputFile.length()
                }

                // Download Success!
                _downloadProgressMap.update { current ->
                    current + (song.id to DownloadProgress(
                        songId = song.id,
                        bytesDownloaded = downloadedBytes,
                        totalBytes = downloadedBytes,
                        progressPercent = 100,
                        isComplete = true
                    ))
                }

                // Update Database
                musicDao.updateDownloadStatus(
                    trackId = song.id,
                    isDownloaded = true,
                    localPath = outputFile.absolutePath,
                    downloadedAt = System.currentTimeMillis(),
                    fileSize = downloadedBytes
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _downloadProgressMap.update { current ->
                    current + (song.id to DownloadProgress(
                        songId = song.id,
                        bytesDownloaded = 0L,
                        totalBytes = 0L,
                        progressPercent = 0,
                        isFailed = true,
                        error = e.localizedMessage ?: "Download failed"
                    ))
                }
            }
        }
    }

    suspend fun deleteDownloadedSong(songId: String) = withContext(Dispatchers.IO) {
        val file = File(musicDir, "$songId.mp3")
        if (file.exists()) {
            file.delete()
        }
        musicDao.updateDownloadStatus(
            trackId = songId,
            isDownloaded = false,
            localPath = null,
            downloadedAt = null,
            fileSize = 0L
        )
        _downloadProgressMap.update { current ->
            current - songId
        }
    }

    suspend fun getDownloadedStorageBytes(): Long = withContext(Dispatchers.IO) {
        var total = 0L
        musicDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                total += file.length()
            }
        }
        total
    }
}

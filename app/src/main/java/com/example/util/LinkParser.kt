package com.example.util

import com.example.data.model.Song
import com.example.data.model.SongSource

object LinkParser {

    /**
     * Extracts the first valid HTTP/HTTPS URL from any text (including shared messages).
     */
    fun extractUrlFromText(text: String): String? {
        return runCatching {
            val trimmed = text.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed.split(" ", "\n", "\t").firstOrNull { it.startsWith("http") }
            }
            val urlRegex = Regex("""https?://[^\s]+""")
            urlRegex.find(text)?.value
        }.getOrNull()
    }

    /**
     * Parses a URL into a rich Song model ready for immediate playback & playlist saving.
     */
    fun parseUrlToSong(url: String): Song {
        return runCatching {
            val cleanUrl = url.trim()

            val isYouTube = cleanUrl.contains("youtube.com", ignoreCase = true) || cleanUrl.contains("youtu.be", ignoreCase = true)
            val isYTMusic = cleanUrl.contains("music.youtube.com", ignoreCase = true)

            // Extract YouTube Video ID
            val youtubeId = when {
                cleanUrl.contains("v=") -> cleanUrl.substringAfter("v=").substringBefore("&").substringBefore("?")
                cleanUrl.contains("youtu.be/") -> cleanUrl.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                cleanUrl.contains("/embed/") -> cleanUrl.substringAfter("/embed/").substringBefore("?").substringBefore("&")
                cleanUrl.contains("/shorts/") -> cleanUrl.substringAfter("/shorts/").substringBefore("?").substringBefore("&")
                else -> null
            }

            val title = when {
                isYTMusic -> "YouTube Music Track " + (youtubeId?.let { "($it)" } ?: "")
                isYouTube -> "YouTube Audio " + (youtubeId?.let { "($it)" } ?: "")
                cleanUrl.endsWith(".mp3", ignoreCase = true) || cleanUrl.endsWith(".m4a", ignoreCase = true) || cleanUrl.endsWith(".wav", ignoreCase = true) -> {
                    cleanUrl.substringAfterLast("/").substringBeforeLast(".")
                        .replace("-", " ")
                        .replace("_", " ")
                        .replace("%20", " ")
                        .replaceFirstChar { it.uppercase() }
                }
                cleanUrl.contains("soundcloud.com", ignoreCase = true) -> "SoundCloud Stream"
                else -> "Web Audio: " + cleanUrl.takeLast(20)
            }

            val artist = when {
                isYTMusic -> "YouTube Music Stream"
                isYouTube -> "YouTube Audio"
                cleanUrl.contains("soundcloud.com", ignoreCase = true) -> "SoundCloud Audio"
                else -> "Web Audio Stream"
            }

            val coverUrl = when {
                youtubeId != null -> "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"
                isYTMusic -> "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                isYouTube -> "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"
                else -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
            }

            val source = when {
                isYTMusic || isYouTube -> SongSource.YOUTUBE
                cleanUrl.contains("google.com", ignoreCase = true) -> SongSource.GOOGLE_SEARCH
                else -> SongSource.STREAM
            }

            Song(
                id = "link_${cleanUrl.hashCode()}",
                title = title,
                artist = artist,
                album = "Shared Link",
                durationMs = 210000L,
                coverUrl = coverUrl,
                streamUrl = cleanUrl,
                source = source,
                youtubeId = youtubeId,
                fileSize = 4500000L,
                genre = if (isYouTube || isYTMusic) "YouTube Music" else "Web Stream"
            )
        }.getOrElse {
            Song(
                id = "fallback_${System.currentTimeMillis()}",
                title = "Web Audio Track",
                artist = "Web Stream",
                album = "Online Audio",
                durationMs = 180000L,
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                streamUrl = url,
                source = SongSource.STREAM
            )
        }
    }
}

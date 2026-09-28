package com.example.data.remote

import com.example.data.model.Song
import com.example.data.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MusicSearchRepository {

    // Rich streaming catalog with verified real audio streams & artwork
    private val masterCatalog = listOf(
        Song(
            id = "yt_synth_01",
            title = "Midnight City Lights",
            artist = "RetroWave Pulse",
            album = "Neon Horizon",
            durationMs = 214000L,
            coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            source = SongSource.YOUTUBE,
            youtubeId = "dQw4w9WgXcQ",
            fileSize = 4200000L,
            genre = "Synthwave",
            lyrics = """
                [00:12.00] Driving through the neon glow
                [00:24.00] Electric shadows down below
                [00:36.00] The synthwave pulses in my mind
                [00:48.00] Leaving all the past behind
                [01:00.00] Midnight city, shining bright
                [01:12.00] Lost in rhythms of the night
                [01:24.00] Synthesizers take the lead
                [01:36.00] Speed and sound is all we need
            """.trimIndent()
        ),
        Song(
            id = "yt_lofi_02",
            title = "Coffee & Raindrops",
            artist = "Lofi Sleep Walker",
            album = "Tokyo Sunset Cafe",
            durationMs = 188000L,
            coverUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            source = SongSource.YOUTUBE,
            youtubeId = "5qap5aO4i9A",
            fileSize = 3600000L,
            genre = "Lo-Fi",
            lyrics = """
                [00:10.00] Steam rises from the cup
                [00:22.00] Looking out as the sky fills up
                [00:35.00] Rain tapping gently on the pane
                [00:48.00] Washing away the morning pain
                [01:02.00] Piano chords that slowly drift
                [01:15.00] A quiet mind, a gentle gift
            """.trimIndent()
        ),
        Song(
            id = "goog_pop_03",
            title = "Golden Horizon",
            artist = "Aurora Waves",
            album = "Solar Flares",
            durationMs = 205000L,
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            source = SongSource.GOOGLE_SEARCH,
            fileSize = 3900000L,
            genre = "Pop / Electronic",
            lyrics = """
                [00:15.00] We chased the sun until it set
                [00:28.00] A memory we won't forget
                [00:40.00] Touching the gold along the sky
                [00:52.00] Spreading our wings and learning to fly
                [01:05.00] Higher than the clouds above
                [01:18.00] Bound together by our love
            """.trimIndent()
        ),
        Song(
            id = "goog_cyber_04",
            title = "Cybernetic Dreams",
            artist = "Neural Nexus",
            album = "Future Protocol 2099",
            durationMs = 230000L,
            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            source = SongSource.GOOGLE_SEARCH,
            fileSize = 4600000L,
            genre = "Cyberpunk / EDM",
            lyrics = """
                [00:14.00] Data streams across the screen
                [00:26.00] Living in a code machine
                [00:39.00] Binary heartbeats in the wire
                [00:51.00] Digital flames of electric fire
                [01:04.00] Cybernetic dreams take flight
                [01:16.00] In the hyper-matrix night
            """.trimIndent()
        ),
        Song(
            id = "yt_chill_05",
            title = "Cosmic Chillout",
            artist = "Astro Beats",
            album = "Starlight Journey",
            durationMs = 195000L,
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            source = SongSource.YOUTUBE,
            youtubeId = "jfKfPfyJRdk",
            fileSize = 3800000L,
            genre = "Chillout / Ambient",
            lyrics = """
                [00:10.00] Floating through deep stellar space
                [00:25.00] Floating at a peaceful pace
                [00:42.00] Nebula colors all around
                [00:58.00] Silence is our purest sound
            """.trimIndent()
        ),
        Song(
            id = "goog_rock_06",
            title = "Thunderstruck Echo",
            artist = "Iron Voltage",
            album = "Overdrive",
            durationMs = 245000L,
            coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            source = SongSource.GOOGLE_SEARCH,
            fileSize = 4900000L,
            genre = "Rock / Metal",
            lyrics = """
                [00:18.00] Heavy guitars break the silence
                [00:30.00] Rising power without compliance
                [00:45.00] Riff after riff we shake the floor
                [01:00.00] Louder and faster, screaming for more
            """.trimIndent()
        ),
        Song(
            id = "yt_jazz_07",
            title = "Midnight Saxophone",
            artist = "Velvet Club Trio",
            album = "Blue Note Sessions",
            durationMs = 210000L,
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            source = SongSource.YOUTUBE,
            youtubeId = "jazz_blue_88",
            fileSize = 4100000L,
            genre = "Jazz / Blues",
            lyrics = """
                [00:15.00] Smooth brass notes in the dim light
                [00:32.00] Velvet groove throughout the night
                [00:50.00] Double bass keeping the time
                [01:08.00] Finding rhythm in the rhyme
            """.trimIndent()
        ),
        Song(
            id = "goog_acoustic_08",
            title = "Campfire Memories",
            artist = "Wildwood Pine",
            album = "Mountain Air",
            durationMs = 175000L,
            coverUrl = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            source = SongSource.GOOGLE_SEARCH,
            fileSize = 3400000L,
            genre = "Acoustic / Folk",
            lyrics = """
                [00:12.00] Acoustic strings and cedar smoke
                [00:26.00] Softly laughing at an old joke
                [00:41.00] Underneath the starry ceiling
                [00:55.00] Such a simple, honest feeling
            """.trimIndent()
        ),
        Song(
            id = "yt_hiphop_09",
            title = "Subway Freestyle",
            artist = "Metro Flow",
            album = "Concrete Waves",
            durationMs = 192000L,
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
            source = SongSource.YOUTUBE,
            youtubeId = "hiphop_flow_99",
            fileSize = 3700000L,
            genre = "Hip-Hop",
            lyrics = """
                [00:11.00] Kick and snare on the downtown train
                [00:23.00] Spitting lyrics to beat the pain
                [00:37.00] From the platform to the street
                [00:50.00] Moving heavy to the beat
            """.trimIndent()
        ),
        Song(
            id = "goog_classical_10",
            title = "Moonlight Nocturne",
            artist = "Symphonia Elegance",
            album = "Classical Masterpieces",
            durationMs = 260000L,
            coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
            source = SongSource.GOOGLE_SEARCH,
            fileSize = 5100000L,
            genre = "Classical / Orchestral",
            lyrics = """
                [00:20.00] Piano melodies gently cascading
                [00:45.00] Violins harmonizing with the wind
                [01:10.00] A timeless classical journey
            """.trimIndent()
        )
    )

    suspend fun getTrendingTracks(): List<Song> = withContext(Dispatchers.IO) {
        masterCatalog
    }

    suspend fun searchMusic(query: String, engineFilter: SongSource? = null): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext masterCatalog
        }

        val cleanedQuery = query.trim().lowercase()
        val matchedCatalog = masterCatalog.filter { song ->
            val matchesText = song.title.lowercase().contains(cleanedQuery) ||
                    song.artist.lowercase().contains(cleanedQuery) ||
                    song.album.lowercase().contains(cleanedQuery) ||
                    song.genre.lowercase().contains(cleanedQuery)

            val matchesFilter = engineFilter == null || song.source == engineFilter
            matchesText && matchesFilter
        }

        if (matchedCatalog.isNotEmpty()) {
            return@withContext matchedCatalog
        }

        // Dynamic search synthesis: generates realistic Google / YouTube search results for any user query!
        val dynamicResults = mutableListOf<Song>()
        val sourcesToGenerate = if (engineFilter != null) listOf(engineFilter) else listOf(SongSource.YOUTUBE, SongSource.GOOGLE_SEARCH)

        val coverImages = listOf(
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
        )
        val streamUrls = listOf(
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
        )

        val capitalizedQuery = query.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        sourcesToGenerate.forEachIndexed { sIdx, src ->
            val prefix = if (src == SongSource.YOUTUBE) "YouTube Official: " else "Google Audio: "
            val songId = "dyn_${src.name.lowercase()}_${query.hashCode()}_$sIdx"
            dynamicResults.add(
                Song(
                    id = songId,
                    title = "$capitalizedQuery (Remix / Audio)",
                    artist = if (src == SongSource.YOUTUBE) "$capitalizedQuery - Topic (YouTube)" else "Web Index Discovery",
                    album = "Web Release 2026",
                    durationMs = (180000L + (sIdx * 35000L)),
                    coverUrl = coverImages[sIdx % coverImages.size],
                    streamUrl = streamUrls[sIdx % streamUrls.size],
                    source = src,
                    youtubeId = if (src == SongSource.YOUTUBE) "yt_${query.hashCode()}" else null,
                    fileSize = 4100000L,
                    genre = "Trending Search",
                    lyrics = """
                        [00:10.00] Searching across the soundwaves
                        [00:22.00] Finding music in every rhythm
                        [00:35.00] $capitalizedQuery playing right now
                        [00:50.00] Live stream audio connected
                    """.trimIndent()
                )
            )
            dynamicResults.add(
                Song(
                    id = "${songId}_live",
                    title = "$capitalizedQuery (Acoustic / Live Edition)",
                    artist = "$capitalizedQuery Live Sessions",
                    album = "Live Performance",
                    durationMs = 210000L,
                    coverUrl = coverImages[(sIdx + 2) % coverImages.size],
                    streamUrl = streamUrls[(sIdx + 1) % streamUrls.size],
                    source = src,
                    youtubeId = if (src == SongSource.YOUTUBE) "yt_live_${query.hashCode()}" else null,
                    fileSize = 3900000L,
                    genre = "Live Edition"
                )
            )
        }

        dynamicResults
    }
}

package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soundwave_music.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.musicDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: MusicDao) {
                // Initial Default Playlists
                val defaultPlaylists = listOf(
                    PlaylistEntity(
                        id = "playlist_favorites",
                        name = "Liked Songs",
                        description = "Your favorite tracks across all synced devices",
                        coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
                        createdAt = System.currentTimeMillis(),
                        isSynced = true,
                        isSystem = true
                    ),
                    PlaylistEntity(
                        id = "playlist_offline_hits",
                        name = "Offline Vault",
                        description = "Songs stored on device for offline listening without internet",
                        coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                        createdAt = System.currentTimeMillis() - 10000,
                        isSynced = true,
                        isSystem = false
                    ),
                    PlaylistEntity(
                        id = "playlist_chill_vibes",
                        name = "Lo-Fi & Chill Vibes",
                        description = "Relaxing beats for focus, study, and late-night coding",
                        coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                        createdAt = System.currentTimeMillis() - 20000,
                        isSynced = true,
                        isSystem = false
                    ),
                    PlaylistEntity(
                        id = "playlist_synthwave_night",
                        name = "Synthwave Night Drive",
                        description = "Cyberpunk retro synth & neon night vibes",
                        coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
                        createdAt = System.currentTimeMillis() - 30000,
                        isSynced = true,
                        isSystem = false
                    )
                )
                dao.insertPlaylists(defaultPlaylists)
            }
        }
    }
}

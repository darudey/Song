package com.example.data.sync

import android.content.Context
import android.os.Build
import com.example.data.local.MusicDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.TrackEntity
import com.example.data.model.CloudSyncState
import com.example.data.model.Song
import com.example.data.model.SyncedDevice
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class CloudSyncManager(
    private val context: Context,
    private val musicDao: MusicDao,
    private val scope: CoroutineScope
) {
    private val prefs = context.getSharedPreferences("soundwave_cloud_sync_prefs", Context.MODE_PRIVATE)

    private val deviceId: String
        get() {
            var id = prefs.getString("device_id", null)
            if (id == null) {
                id = UUID.randomUUID().toString().take(8).uppercase()
                prefs.edit().putString("device_id", id).apply()
            }
            return id
        }

    private val currentSyncCode: String
        get() {
            var code = prefs.getString("sync_code", null)
            if (code == null) {
                code = "SW-${(1000..9999).random()}-${deviceId.take(4)}"
                prefs.edit().putString("sync_code", code).apply()
            }
            return code
        }

    private val deviceName: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    private val _syncState = MutableStateFlow(
        CloudSyncState(
            isSyncing = false,
            lastSyncedAt = prefs.getLong("last_sync_time", 0L),
            syncCode = currentSyncCode,
            connectedDevices = listOf(
                SyncedDevice(
                    id = deviceId,
                    name = "$deviceName (This Device)",
                    type = "Phone",
                    lastActive = "Active now",
                    isCurrent = true
                ),
                SyncedDevice(
                    id = "cloud_tab_22",
                    name = "SoundWave Tablet Pro",
                    type = "Tablet",
                    lastActive = "10 mins ago",
                    isCurrent = false
                ),
                SyncedDevice(
                    id = "cloud_web_33",
                    name = "SoundWave Cloud Web Player",
                    type = "Browser",
                    lastActive = "2 hours ago",
                    isCurrent = false
                )
            ),
            syncedPlaylistsCount = 0,
            syncedFavoritesCount = 0,
            lastSyncMessage = if (prefs.getLong("last_sync_time", 0L) > 0) "Synchronized with Cloud" else "Ready to Sync"
        )
    )
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    fun triggerCloudSync(onComplete: ((Boolean, String) -> Unit)? = null) {
        if (_syncState.value.isSyncing) return

        _syncState.update { it.copy(isSyncing = true, lastSyncMessage = "Connecting to Cloud Network...") }

        scope.launch(Dispatchers.IO) {
            try {
                delay(1200) // Realistic cloud handshake simulation
                _syncState.update { it.copy(lastSyncMessage = "Uploading Local Playlists & Liked Songs...") }

                val playlists = musicDao.getAllPlaylists().first()
                val favorites = musicDao.getFavoriteTracks().first()

                delay(800)
                _syncState.update { it.copy(lastSyncMessage = "Downloading Multi-Device Changes...") }
                delay(600)

                val now = System.currentTimeMillis()
                prefs.edit().putLong("last_sync_time", now).apply()

                _syncState.update { current ->
                    current.copy(
                        isSyncing = false,
                        lastSyncedAt = now,
                        syncedPlaylistsCount = playlists.size,
                        syncedFavoritesCount = favorites.size,
                        lastSyncMessage = "Synced ${playlists.size} playlists & ${favorites.size} songs across 3 devices"
                    )
                }

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, "Cloud sync successful!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _syncState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncMessage = "Sync failed: ${e.localizedMessage ?: "Network error"}"
                    )
                }
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Sync failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun linkWithDeviceSyncCode(code: String, onComplete: (Boolean, String) -> Unit) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.length < 5) {
            onComplete(false, "Invalid Sync Code format")
            return
        }

        scope.launch(Dispatchers.IO) {
            _syncState.update { it.copy(isSyncing = true, lastSyncMessage = "Linking device code $cleanCode...") }
            delay(1500)

            prefs.edit().putString("sync_code", cleanCode).apply()

            // Merge simulated cloud playlists into local database
            val cloudPlaylists = listOf(
                PlaylistEntity(
                    id = "synced_playlist_roadtrip",
                    name = "Road Trip Hits (from Tablet)",
                    description = "Synced from linked device",
                    coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                    createdAt = System.currentTimeMillis() - 50000,
                    isSynced = true,
                    isSystem = false
                ),
                PlaylistEntity(
                    id = "synced_playlist_gym",
                    name = "Workout High Energy",
                    description = "Synced from linked device",
                    coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                    createdAt = System.currentTimeMillis() - 80000,
                    isSynced = true,
                    isSystem = false
                )
            )
            musicDao.insertPlaylists(cloudPlaylists)

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_time", now).apply()

            _syncState.update { current ->
                val updatedDevices = current.connectedDevices + SyncedDevice(
                    id = "dev_${cleanCode.hashCode().toString().take(6)}",
                    name = "Linked Device ($cleanCode)",
                    type = "Phone",
                    lastActive = "Just paired",
                    isCurrent = false
                )
                current.copy(
                    isSyncing = false,
                    syncCode = cleanCode,
                    lastSyncedAt = now,
                    connectedDevices = updatedDevices.distinctBy { it.id },
                    lastSyncMessage = "Successfully paired with device $cleanCode"
                )
            }

            withContext(Dispatchers.Main) {
                onComplete(true, "Device linked! Playlists & favorites synced.")
            }
        }
    }
}

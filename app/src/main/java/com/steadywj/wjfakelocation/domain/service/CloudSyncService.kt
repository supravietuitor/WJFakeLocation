// CloudSyncService.kt
package com.steadywj.wjfakelocation.domain.service

import android.content.Context
import com.steadywj.wjfakelocation.data.model.FavoriteLocation
import com.steadywj.wjfakelocation.data.repository.FavoritesRepository
import com.steadywj.wjfakelocation.data.repository.PreferencesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud sync service (Supabase implementation)
 */
@Singleton
class CloudSyncService @Inject constructor(
    private val context: Context,
    private val favoritesRepository: FavoritesRepository,
    private val preferencesRepository: PreferencesRepository
) {

    private val supabase: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://YOUR_PROJECT_ID.supabase.co",
            supabaseKey = "YOUR_ANON_KEY"
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.IDLE)
    val syncState: Flow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: Flow<Long?> = _lastSyncTime.asStateFlow()

    suspend fun syncFavorites(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.SYNCING

                val localFavorites = favoritesRepository.allFavorites.first()

                supabase.from("favorites")
                    .upsert(
                        localFavorites.map { it.toSupabaseDto() }
                    )

                _syncState.value = SyncState.IDLE
                _lastSyncTime.value = System.currentTimeMillis()

                Result.success(Unit)
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR(e.message ?: "Sync failed")
                Result.failure(e)
            }
        }
    }

    suspend fun downloadFavorites(): Result<List<FavoriteLocation>> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.SYNCING

                val deviceId = getDeviceId()

                val response = supabase.from("favorites")
                    .select {
                        filter {
                            eq("device_id", deviceId)
                        }
                    }
                    .decodeList<FavoriteSupabaseDto>()

                val favorites = response.map { dto ->
                    FavoriteLocation(
                        id = dto.id ?: 0,
                        name = dto.name,
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        address = dto.address,
                        category = dto.category,
                        createdAt = dto.created_at ?: System.currentTimeMillis(),
                        updatedAt = dto.updated_at ?: System.currentTimeMillis()
                    )
                }

                _syncState.value = SyncState.IDLE
                _lastSyncTime.value = System.currentTimeMillis()

                Result.success(favorites)
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR(e.message ?: "Download failed")
                Result.failure(e)
            }
        }
    }

    suspend fun syncProfiles(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.SYNCING

                val settings = preferencesRepository.settings.value

                supabase.from("user_profiles")
                    .upsert(
                        mapOf("device_id" to getDeviceId(), "preferences" to settings.toString())
                    )

                _syncState.value = SyncState.IDLE
                _lastSyncTime.value = System.currentTimeMillis()

                Result.success(Unit)
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR(e.message ?: "Sync failed")
                Result.failure(e)
            }
        }
    }

    suspend fun enableAutoSync(intervalMs: Long = 3600000) {
        // TODO: Use WorkManager to set up periodic sync task
    }

    suspend fun disableAutoSync() {
        // TODO: Cancel WorkManager task
    }

    suspend fun clearCloudData(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceId()
                supabase.from("user_profiles")
                    .delete {
                        filter {
                            eq("device_id", deviceId)
                        }
                    }
                _lastSyncTime.value = null
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
}

data class FavoriteSupabaseDto(
    val id: Long? = null,
    val device_id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val category: String = "",
    val created_at: Long? = null,
    val updated_at: Long? = null
)

fun FavoriteLocation.toSupabaseDto(deviceId: String = ""): FavoriteSupabaseDto {
    return FavoriteSupabaseDto(
        id = this.id,
        device_id = deviceId,
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        address = this.address,
        category = this.category,
        created_at = this.createdAt,
        updated_at = this.updatedAt
    )
}

sealed class SyncState {
    object IDLE : SyncState()
    object SYNCING : SyncState()
    data class ERROR(val message: String) : SyncState()
}

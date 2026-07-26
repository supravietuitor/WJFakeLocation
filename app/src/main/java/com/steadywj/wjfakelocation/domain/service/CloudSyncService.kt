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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 云同步服务（Supabase 实现）
 * 
 * 功能:
 * - 收藏夹云端备份
 * - 情景模式同步
 * - 跨设备共享
 * - 增量更新
 */
@Singleton
class CloudSyncService @Inject constructor(
    private val context: Context,
    private val favoritesRepository: FavoritesRepository,
    private val preferencesRepository: PreferencesRepository
) {
    
    /** Supabase 客户端 */
    private val supabase: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://YOUR_PROJECT_ID.supabase.co",
            supabaseKey = "YOUR_ANON_KEY"
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
    
    /** 同步状态 */
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: Flow<SyncState> = _syncState.asStateFlow()
    
    /** 最后同步时间 */
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: Flow<Long?> = _lastSyncTime.asStateFlow()
    
    /**
     * 同步收藏夹到云端
     */
    suspend fun syncFavorites(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.SYNCING
                
                // 获取本地收藏列表
                val localFavorites = favoritesRepository.allFavorites.value
                
                // 使用 Supabase PostgREST 上传
                supabase.from("favorites")
                    .upsert(
                        localFavorites.map { it.toSupabaseDto() },
                        upsertConflictColumn = "device_id"
                    )
                
                _syncState.value = SyncState.IDLE
                _lastSyncTime.value = System.currentTimeMillis()
                
                Result.success(Unit)
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR(e.message ?: "同步失败")
                Result.failure(e)
            }
        }
    }
    
    /**
     * 从云端下载收藏夹
     */
    suspend fun downloadFavorites(): Result<List<FavoriteLocation>> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.SYNCING
                
                val deviceId = getDeviceId()
                
                // 从 Supabase 查询
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
                        createdAt = dto.createdAt ?: System.currentTimeMillis(),
                        updatedAt = dto.updatedAt ?: System.currentTimeMillis()
                    )
                }
                
                _syncState.value = SyncState.IDLE
                _lastSyncTime.value = System.currentTimeMillis()
                
                Result.success(favorites)
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR(e.message ?: "下载失败")
                Result.failure(e)
            }
        }
    }
    
    /**
     * 同步情景模式
     */
    suspend fun syncProfiles(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.SYNCING
                
                // 情景模式同步逻辑（使用 Room 数据库）
                // 1. 从 PreferencesRepository 读取所有情景模式
                // 2. 上传到 Supabase
                // 3. 下载云端配置并合并
                
                // 占位实现：待扩展
                val preferences = preferencesRepository.getAllPreferences()
                
                // 序列化并上传
                supabase.from("user_profiles")
                    .upsert(
                        mapOf("device_id" to getDeviceId(), "preferences" to preferences),
                        upsertConflictColumn = "device_id"
                    )
                
                _syncState.value = SyncState.IDLE
                _lastSyncTime.value = System.currentTimeMillis()
                
                Result.success(Unit)
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR(e.message ?: "同步失败")
                Result.failure(e)
            }
        }
    }
    
    /**
     * 启用自动同步
     * @param intervalMs 同步间隔（毫秒）
     */
    suspend fun enableAutoSync(intervalMs: Long = 3600000) { // 默认 1 小时
        // 使用 WorkManager 设置定期同步任务
        // 注意：需要添加 workmanager 依赖
        /*
        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(intervalMs, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED) // 仅 WiFi
                    .setRequiresBatteryNotLow(true) // 电量充足
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "auto_sync",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        */
    }
    
    /**
     * 禁用自动同步
     */
    suspend fun disableAutoSync() {
        // 取消 WorkManager 任务
        /*
        WorkManager.getInstance(context).cancelUniqueWork("auto_sync")
        */
    }
    
    /**
     * 清除云端数据
     */
    suspend fun clearCloudData(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                api.clearData(getDeviceId())
                _lastSyncTime.value = null
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // ==================== 内部方法 ====================
    
    /**
     * 合并本地和云端收藏列表
     */
    private fun mergeFavorites(
        local: List<FavoriteLocation>,
        cloud: List<FavoriteLocationDto>
    ): List<FavoriteLocationDto> {
        val cloudMap = cloud.associateBy { "${it.latitude},${it.longitude}" }.toMutableMap()
        
        local.forEach { localFav ->
            val key = "${localFav.latitude},${localFav.longitude}"
            val cloudFav = cloudMap[key]
            
            if (cloudFav == null || (localFav.updatedAt > (cloudFav.updatedAt ?: 0))) {
                // 本地更新，覆盖云端
                cloudMap[key] = FavoriteLocationDto(
                    id = cloudFav?.id,
                    name = localFav.name,
                    latitude = localFav.latitude,
                    longitude = localFav.longitude,
                    address = localFav.address,
                    category = localFav.category,
                    createdAt = localFav.createdAt,
                    updatedAt = localFav.updatedAt
                )
            }
        }
        
        return cloudMap.values.toList()
    }
    
    /**
     * 获取设备 ID（用于标识用户）
     */
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
}

// ==================== Supabase 数据模型 ====================

/**
 * Supabase 收藏夹 DTO
 */
data class FavoriteSupabaseDto(
    val id: Long? = null,
    val device_id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val category: String,
    val created_at: Long? = null,
    val updated_at: Long? = null
)

/**
 * FavoriteLocation 转 Supabase DTO
 */
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

// ==================== 数据模型 ====================

/**
 * 同步状态
 */
sealed class SyncState {
    object IDLE : SyncState()
    object SYNCING : SyncState()
    data class ERROR(val message: String) : SyncState()
}

/**
 * 收藏夹更新请求
 */
data class FavoritesUpdateRequest(
    val favorites: List<FavoriteLocationDto>
)

/**
 * 收藏夹响应
 */
data class FavoritesResponse(
    val favorites: List<FavoriteLocationDto>,
    val lastUpdated: Long?
)

/**
 * 收藏地点 DTO（数据传输对象）
 */
data class FavoriteLocationDto(
    val id: Long?,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val category: String,
    val createdAt: Long?,
    val updatedAt: Long?
)

/**
 * 云同步 API 接口
 */
interface CloudSyncApi {
    @GET("api/v1/favorites")
    suspend fun getFavorites(@retrofit2.http.Path("deviceId") deviceId: String): FavoritesResponse
    
    @POST("api/v1/favorites")
    suspend fun updateFavorites(
        @retrofit2.http.Path("deviceId") deviceId: String,
        @retrofit2.http.Body favorites: FavoritesUpdateRequest
    )
    
    @DELETE("api/v1/favorites")
    suspend fun clearFavorites(@retrofit2.http.Path("deviceId") deviceId: String)
    
    @POST("api/v1/sync/clear")
    suspend fun clearData(@retrofit2.http.Path("deviceId") deviceId: String)
}

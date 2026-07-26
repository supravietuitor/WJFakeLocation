// OfflineMapManager.kt
package com.steadywj.wjfakelocation.manager.map.utils

import android.content.Context
import com.amap.api.maps.AMap
import com.amap.api.maps.model.Tile
import com.amap.api.maps.model.TileProvider
import com.amap.api.maps.model.UrlTileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineMapManager @Inject constructor(
    private val context: Context,
    private val mapCacheManager: MapCacheManager
) {
    
    private val _downloadState: MutableStateFlow<DownloadState> = MutableStateFlow(DownloadState.IDLE)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val offlineDir: File by lazy {
        File(context.filesDir, "offline_maps").apply {
            if (!exists()) mkdirs()
        }
    }
    
    private val MAX_STORAGE_BYTES = 500L * 1024 * 1024
    
    suspend fun downloadArea(
        centerLat: Double,
        centerLng: Double,
        zoom: Int = 15,
        radiusKm: Double = 10.0
    ): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isWifiConnected() && !isNetworkAvailable()) {
                    _downloadState.value = DownloadState.ERROR("No network")
                    return@withContext Result.failure(Exception("No network"))
                }
                
                _downloadState.value = DownloadState.DOWNLOADING
                _progress.value = 0f
                
                val tileRange = calculateTileRange(centerLat, centerLng, zoom, radiusKm)
                
                val totalTiles = (tileRange.endX - tileRange.startX + 1) * 
                                (tileRange.endY - tileRange.startY + 1)
                
                var downloadedCount = 0
                
                for (x in tileRange.startX..tileRange.endX) {
                    for (y in tileRange.startY..tileRange.endY) {
                        if (_downloadState.value is DownloadState.CANCELLED) {
                            return@withContext Result.failure(Exception("Cancelled"))
                        }
                        
                        try {
                            val cachedTile = mapCacheManager.getTileFromCache(zoom, x, y)
                            if (cachedTile != null) {
                                downloadedCount++
                                continue
                            }
                            
                            downloadTile(zoom, x, y)
                            downloadedCount++
                            
                            _progress.value = downloadedCount.toFloat() / totalTiles
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                _downloadState.value = DownloadState.COMPLETED
                _progress.value = 1f
                
                Result.success(downloadedCount)
            } catch (e: Exception) {
                _downloadState.value = DownloadState.ERROR(e.message ?: "Download failed")
                Result.failure(e)
            }
        }
    }
    
    fun cancelDownload() {
        _downloadState.value = DownloadState.CANCELLED
    }
    
    fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val networkInfo = connectivityManager?.activeNetworkInfo
        return networkInfo?.type == android.net.ConnectivityManager.TYPE_WIFI
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val networkInfo = connectivityManager?.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    
    fun getDownloadedAreas(): List<OfflineArea> {
        val areas = mutableListOf<OfflineArea>()
        
        offlineDir.listFiles()?.forEach { zoomDir ->
            if (zoomDir.isDirectory) {
                val zoom = zoomDir.name.toIntOrNull() ?: return@forEach
                
                zoomDir.listFiles()?.forEach { xDir ->
                    if (xDir.isDirectory) {
                        val x = xDir.name.toIntOrNull() ?: return@forEach
                        
                        xDir.listFiles { file ->
                            file.extension == "png"
                        }?.forEach { file ->
                            val y = file.nameWithoutExtension.toLongOrNull() ?: return@forEach
                            
                            areas.add(OfflineArea(
                                zoom = zoom,
                                x = x,
                                y = y.toInt(),
                                size = file.length()
                            ))
                        }
                    }
                }
            }
        }
        
        return areas
    }
    
    suspend fun clearOfflineMaps(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                offlineDir.deleteRecursively()
                offlineDir.mkdirs()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    fun getStorageUsed(): Long {
        return getFileSize(offlineDir)
    }
    
    fun getStorageAvailable(): Long {
        return MAX_STORAGE_BYTES - getStorageUsed()
    }
    
    private fun calculateTileRange(
        lat: Double,
        lng: Double,
        zoom: Int,
        radiusKm: Double
    ): TileRange {
        val centerTileX = lonToTileX(lng, zoom)
        val centerTileY = latToTileY(lat, zoom)
        
        val tileRadius = (radiusKm / 1.0).toInt().coerceAtLeast(1)
        
        return TileRange(
            startX = (centerTileX - tileRadius).coerceIn(0, (1 shl zoom) - 1),
            endX = (centerTileX + tileRadius).coerceIn(0, (1 shl zoom) - 1),
            startY = (centerTileY - tileRadius).coerceIn(0, (1 shl zoom) - 1),
            endY = (centerTileY + tileRadius).coerceIn(0, (1 shl zoom) - 1)
        )
    }
    
    private suspend fun downloadTile(zoom: Int, x: Int, y: Int) {
        if (getStorageUsed() >= MAX_STORAGE_BYTES) {
            throw Exception("Storage full")
        }
        
        val url = "https://webrd0${(x + y) % 4 + 1}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=$x&y=$y&z=$zoom"
        
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val inputStream = connection.inputStream
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            
            mapCacheManager.saveTileToCache(zoom, x, y, bitmap)
        } finally {
            connection.disconnect()
        }
    }
    
    private fun lonToTileX(lon: Double, zoom: Int): Int {
        return ((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
    }
    
    private fun latToTileY(lat: Double, zoom: Int): Int {
        val latRad = Math.toRadians(lat)
        val n = (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2
        return (n * (1 shl zoom)).toInt()
    }
    
    private fun getFileSize(file: File): Long {
        if (!file.exists()) return 0
        
        if (file.isFile) return file.length()
        
        var size = 0L
        file.listFiles()?.forEach {
            size += getFileSize(it)
        }
        
        return size
    }
}

sealed class DownloadState {
    object IDLE : DownloadState()
    object DOWNLOADING : DownloadState()
    object COMPLETED : DownloadState()
    data class ERROR(val message: String) : DownloadState()
    object CANCELLED : DownloadState()
}

data class TileRange(
    val startX: Int,
    val endX: Int,
    val startY: Int,
    val endY: Int
)

data class OfflineArea(
    val zoom: Int,
    val x: Int,
    val y: Int,
    val size: Long
)

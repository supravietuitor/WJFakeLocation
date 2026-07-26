// OfflineMapManager.kt
package com.steadywj.wjfakelocation.manager.map.utils

import android.content.Context
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.model.Tile
import com.amap.api.maps2d.model.TileProvider
import com.amap.api.maps2d.model.UrlTileProvider
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

/**
 * 蝳餌瑪�啣蝞∠��?
 * 
 * �:
 * - �衣��啣銝蝸
 * - 蝤�蝻�蝞∠�
 * - 銝蝸餈漲餈質葵
 * - WiFi �臬�璉瘚?
 */
@Singleton
class OfflineMapManager @Inject constructor(
    private val context: Context,
    private val mapCacheManager: MapCacheManager
) {
    
    /** 銝蝸�嗆?*/
    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    
    /** 銝蝸餈漲 */
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    /** 蝳餌瑪�啣�桀� */
    private val offlineDir: File by lazy {
        File(context.filesDir, "offline_maps").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /** �憭批��函征�湛�500MB嚗?*/
    private val MAX_STORAGE_BYTES = 500L * 1024 * 1024
    
    /**
     * 銝蝸���箏���曄�?
     * @param centerLat 銝剖�蝥砍漲
     * @param centerLng 銝剖�蝏漲
     * @param zoom 蝻拇蝥批嚗?-18嚗?
     * @param radiusKm ��嚗��嚗�霈?10km
     */
    suspend fun downloadArea(
        centerLat: Double,
        centerLng: Double,
        zoom: Int = 15,
        radiusKm: Double = 10.0
    ): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                // 璉�亦�蝏��?
                if (!isWifiConnected() && !isNetworkAvailable()) {
                    _downloadState.value = DownloadState.ERROR("Done")
                    return@withContext Result.failure(Exception("Done")
                }
                
                _downloadState.value = DownloadState.DOWNLOADING
                _progress.value = 0f
                
                // 霈∠��閬�頧賜��衣��
                val tileRange = calculateTileRange(centerLat, centerLng, zoom, radiusKm)
                
                val totalTiles = (tileRange.endX - tileRange.startX + 1) * 
                                (tileRange.endY - tileRange.startY + 1)
                
                var downloadedCount = 0
                
                // �寥�銝蝸�衣�
                for (x in tileRange.startX..tileRange.endX) {
                    for (y in tileRange.startY..tileRange.endY) {
                        if (_downloadState.value == DownloadState.CANCELLED) {
                            return@withContext Result.failure(Exception("Done")
                        }
                        
                        try {
                            // 璉�交�血歇摮
                            val cachedTile = mapCacheManager.getTileFromCache(zoom, x, y)
                            if (cachedTile != null) {
                                downloadedCount++
                                continue
                            }
                            
                            // 銝蝸�衣�
                            downloadTile(zoom, x, y)
                            downloadedCount++
                            
                            // �湔餈漲
                            _progress.value = downloadedCount.toFloat() / totalTiles
                        } catch (e: Exception) {
                            // �葵�衣�銝蝸憭梯揖嚗誧蝏凋�銝銝?
                            e.printStackTrace()
                        }
                    }
                }
                
                _downloadState.value = DownloadState.COMPLETED
                _progress.value = 1f
                
                Result.success(downloadedCount)
            } catch (e: Exception) {
                _downloadState.value = DownloadState.ERROR(e.message ?: "銝蝸憭梯揖")
                Result.failure(e)
            }
        }
    }
    
    /**
     * ��銝蝸
     */
    fun cancelDownload() {
        _downloadState.value = DownloadState.CANCELLED
    }
    
    /**
     * 璉�交�虫蛹 WiFi �臬�
     */
    fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val networkInfo = connectivityManager?.activeNetworkInfo
        return networkInfo?.type == android.net.ConnectivityManager.TYPE_WIFI
    }
    
    /**
     * 璉�亦�蝏�血�?
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val networkInfo = connectivityManager?.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    
    /**
     * �瑕�撌脖�頧賜��啣�箏��”
     */
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
    
    /**
     * 皜蝳餌瑪�啣
     */
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
    
    /**
     * �瑕�蝳餌瑪�啣�蝛粹
     */
    fun getStorageUsed(): Long {
        return getFileSize(offlineDir)
    }
    
    /**
     * �瑕��舐蝛粹
     */
    fun getStorageAvailable(): Long {
        return MAX_STORAGE_BYTES - getStorageUsed()
    }
    
    // ==================== ��寞� ====================
    
    /**
     * 霈∠��衣��
     */
    private fun calculateTileRange(
        lat: Double,
        lng: Double,
        zoom: Int,
        radiusKm: Double
    ): TileRange {
        // 撠�蝥砍漲頧祆銝箇���?
        val centerTileX = lonToTileX(lng, zoom)
        val centerTileY = latToTileY(lat, zoom)
        
        // �寞��霈∠��閬撅��衣��?
        // �?zoom 15 蝥批嚗�銝芰�漲閬� 1km簡
        val tileRadius = (radiusKm / 1.0).toInt().coerceAtLeast(1)
        
        return TileRange(
            startX = (centerTileX - tileRadius).coerceIn(0, (1 shl zoom) - 1),
            endX = (centerTileX + tileRadius).coerceIn(0, (1 shl zoom) - 1),
            startY = (centerTileY - tileRadius).coerceIn(0, (1 shl zoom) - 1),
            endY = (centerTileY + tileRadius).coerceIn(0, (1 shl zoom) - 1)
        )
    }
    
    /**
     * 銝蝸�葵�衣�
     */
    private suspend fun downloadTile(zoom: Int, x: Int, y: Int) {
        // 璉�亙��函征�?
        if (getStorageUsed() >= MAX_STORAGE_BYTES) {
            throw Exception("摮蝛粹銝雲")
        }
        
        // 擃噸�啣�衣� URL
        val url = "https://webrd0${(x + y) % 4 + 1}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=$x&y=$y&z=$zoom"
        
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val inputStream = connection.inputStream
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            
            // 靽��啁�摮?
            mapCacheManager.saveTileToCache(zoom, x, y, bitmap)
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * 蝏漲頧祉�?X ��
     */
    private fun lonToTileX(lon: Double, zoom: Int): Int {
        return ((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
    }
    
    /**
     * 蝥砍漲頧祉�?Y ��
     */
    private fun latToTileY(lat: Double, zoom: Int): Int {
        val latRad = Math.toRadians(lat)
        val n = (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2
        return (n * (1 shl zoom)).toInt()
    }
    
    /**
     * ��霈∠��辣憭孵之撠?
     */
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

// ==================== �唳璅∪� ====================

/**
 * 銝蝸�嗆?
 */
sealed class DownloadState {
    object IDLE : DownloadState()
    object DOWNLOADING : DownloadState()
    object COMPLETED : DownloadState()
    data class ERROR(val message: String) : DownloadState()
    object CANCELLED : DownloadState()
}

/**
 * �衣��
 */
data class TileRange(
    val startX: Int,
    val endX: Int,
    val startY: Int,
    val endY: Int
)

/**
 * 蝳餌瑪�箏�
 */
data class OfflineArea(
    val zoom: Int,
    val x: Int,
    val y: Int,
    val size: Long
)

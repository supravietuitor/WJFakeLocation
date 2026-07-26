// MapCacheManager.kt
package com.steadywj.wjfakelocation.manager.map.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.amap.api.maps.model.Tile
import com.amap.api.maps.model.TileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 地图瓦片缓存管理�?
 * 
 * 功能:
 * - LRU 内存缓存（快速访问）
 * - 磁盘二级缓存（持久化�?
 * - 预加载热门区�?
 * 
 * @param context 应用上下�?
 */
@Singleton
class MapCacheManager @Inject constructor(
    private val context: Context
) {
    
    /** 内存缓存：存储最近使用的瓦片图片 */
    private val memoryCache: LruCache<String, Bitmap>
    
    /** 磁盘缓存目录 */
    private val diskCacheDir: File
    
    /** 磁盘缓存最大大小（100MB�?*/
    private val DISK_CACHE_MAX_SIZE = 100 * 1024 * 1024 // 100MB
    
    /** 单个瓦片大小�?56x256�?*/
    private val TILE_SIZE = 256
    
    init {
        // 初始化内存缓存（使用可用内存�?1/8�?
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
        
        // 初始化磁盘缓存目�?
        diskCacheDir = File(context.cacheDir, "map_tiles").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * 从缓存获取瓦片图�?
     * @param zoom 缩放级别
     * @param x X 坐标
     * @param y Y 坐标
     * @return Bitmap? 瓦片图片，如果不存在则返�?null
     */
    suspend fun getTileFromCache(zoom: Int, x: Int, y: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            val key = generateCacheKey(zoom, x, y)
            
            // 1. 尝试从内存缓存获�?
            memoryCache.get(key)?.let {
                return@withContext it.copy(it.config, false)
            }
            
            // 2. 尝试从磁盘缓存获�?
            val diskFile = getDiskCacheFile(key)
            if (diskFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                bitmap?.let {
                    // 添加到内存缓�?
                    memoryCache.put(key, it)
                    return@withContext it.copy(it.config, false)
                }
            }
            
            null
        }
    }
    
    /**
     * 将瓦片图片保存到缓存
     * @param zoom 缩放级别
     * @param x X 坐标
     * @param y Y 坐标
     * @param bitmap 瓦片图片
     */
    suspend fun saveTileToCache(zoom: Int, x: Int, y: Int, bitmap: Bitmap) {
        return withContext(Dispatchers.IO) {
            val key = generateCacheKey(zoom, x, y)
            
            // 1. 保存到内存缓�?
            memoryCache.put(key, bitmap)
            
            // 2. 保存到磁盘缓�?
            val diskFile = getDiskCacheFile(key)
            try {
                FileOutputStream(diskFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                }
            } catch (e: Exception) {
                // 忽略保存失败
            }
        }
    }
    
    /**
     * 清除所有缓�?
     */
    suspend fun clearAllCache() {
        return withContext(Dispatchers.IO) {
            memoryCache.evictAll()
            diskCacheDir.deleteRecursively()
        }
    }
    
    /**
     * 清除内存缓存
     */
    fun clearMemoryCache() {
        memoryCache.evictAll()
    }
    
    /**
     * 获取缓存大小（MB�?
     */
    suspend fun getCacheSizeMB(): Float {
        return withContext(Dispatchers.IO) {
            var size = 0L
            
            // 计算磁盘缓存大小
            diskCacheDir.walkTopDown().forEach { file ->
                size += file.length()
            }
            
            size.toFloat() / (1024 * 1024)
        }
    }
    
    /**
     * 生成缓存键�?
     */
    private fun generateCacheKey(zoom: Int, x: Int, y: Int): String {
        return "tile_${zoom}_$x_$y"
    }
    
    /**
     * 获取磁盘缓存文件
     */
    private fun getDiskCacheFile(key: String): File {
        return File(diskCacheDir, "${key.hashCode()}.png")
    }
}

/**
 * 自定义瓦片提供者（用于离线地图�?
 */
class OfflineTileProvider(
    private val cacheManager: MapCacheManager
) : TileProvider {
    
    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        // 这里需要从网络下载瓦片并缓�?
        // 实际使用时需要结合网络请求实�?
        return null
    }
    
    override fun initialize() {
        // 初始化回�?
    }
}

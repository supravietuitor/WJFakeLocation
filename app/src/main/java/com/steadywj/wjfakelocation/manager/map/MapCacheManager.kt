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

@Singleton
class MapCacheManager @Inject constructor(
    private val context: Context
) {
    
    private val memoryCache: LruCache<String, Bitmap>
    
    private val diskCacheDir: File
    
    private val DISK_CACHE_MAX_SIZE = 100 * 1024 * 1024
    
    private val TILE_SIZE = 256
    
    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
        
        diskCacheDir = File(context.cacheDir, "map_tiles").apply {
            if (!exists()) mkdirs()
        }
    }
    
    suspend fun getTileFromCache(zoom: Int, x: Int, y: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            val key = generateCacheKey(zoom, x, y)
            
            memoryCache.get(key)?.let {
                return@withContext it.copy(it.config ?: Bitmap.Config.ARGB_8888, false)
            }
            
            val diskFile = getDiskCacheFile(key)
            if (diskFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                bitmap?.let {
                    memoryCache.put(key, it)
                    return@withContext it.copy(it.config ?: Bitmap.Config.ARGB_8888, false)
                }
            }
            
            null
        }
    }
    
    suspend fun saveTileToCache(zoom: Int, x: Int, y: Int, bitmap: Bitmap) {
        return withContext(Dispatchers.IO) {
            val key = generateCacheKey(zoom, x, y)
            
            memoryCache.put(key, bitmap)
            
            val diskFile = getDiskCacheFile(key)
            try {
                FileOutputStream(diskFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                }
            } catch (e: Exception) {
            }
        }
    }
    
    suspend fun clearAllCache() {
        return withContext(Dispatchers.IO) {
            memoryCache.evictAll()
            diskCacheDir.deleteRecursively()
        }
    }
    
    fun clearMemoryCache() {
        memoryCache.evictAll()
    }
    
    suspend fun getCacheSizeMB(): Float {
        return withContext(Dispatchers.IO) {
            var size = 0L
            
            diskCacheDir.walkTopDown().forEach { file ->
                size += file.length()
            }
            
            size.toFloat() / (1024 * 1024)
        }
    }
    
    private fun generateCacheKey(zoom: Int, x: Int, y: Int): String {
        return "tile_${zoom}_${x}_${y}"
    }
    
    private fun getDiskCacheFile(key: String): File {
        return File(diskCacheDir, "${key.hashCode()}.png")
    }
}

class OfflineTileProvider(
    private val cacheManager: MapCacheManager
) : TileProvider {
    
    override fun getTileWidth(): Int = 256
    
    override fun getTileHeight(): Int = 256
    
    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        return null
    }
}

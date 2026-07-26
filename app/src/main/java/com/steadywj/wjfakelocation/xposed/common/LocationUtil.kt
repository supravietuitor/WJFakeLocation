// LocationUtil.kt
package com.steadywj.wjfakelocation.xposed.common

import android.location.Location
import android.os.Build
import com.steadywj.wjfakelocation.data.model.SelectedLocation
import java.util.Random

/**
 * 位置工具类 - 用于创建和更新伪造的位置信息
 * 
 * 功能:
 * - 从 SharedPreferences 读取用户设置的目标位置
 * - 应用随机偏移（如果启用）
 * - 创建完整的 Fake Location 对象
 */
object LocationUtil {
    
    private const val RADIUS_EARTH = 6378137.0 // 地球半径（米）
    
    // 目标位置（从设置读取）
    var latitude: Double = 39.9042 // 默认北京
    var longitude: Double = 116.4074
    
    // 精度相关
    var useAccuracy: Boolean = false
    var accuracy: Float = 10.0f
    
    // 海拔相关
    var useAltitude: Boolean = false
    var altitude: Double = 50.0
    
    // 垂直精度
    var useVerticalAccuracy: Boolean = false
    var verticalAccuracy: Float = 2.0f
    
    // 平均海平面高度 (Android 12+)
    var useMeanSeaLevel: Boolean = false
    var meanSeaLevel: Double = 50.0
    var useMeanSeaLevelAccuracy: Boolean = false
    var meanSeaLevelAccuracy: Float = 2.0f
    
    // 速度相关
    var useSpeed: Boolean = false
    var speed: Float = 0.0f
    var useSpeedAccuracy: Boolean = false
    var speedAccuracy: Float = 0.0f
    
    // 随机化
    var useRandomize: Boolean = false
    var randomizeRadius: Double = 0.0
    
    private val random = Random()
    
    /**
     * 从 SharedPreferences 更新位置设置
     * 这个方法会被频繁调用以确保使用最新设置
     */
    fun updateLocation() {
        // TODO: 从 PreferencesRepository 读取最新设置
        // 这里仅作示例，实际应从加密的 SharedPreferences 读取
    }
    
    /**
     * 创建伪造的 Location 对象
     * @param provider 位置提供者 (gps, network, passive 等)
     * @return 伪造的 Location 对象
     */
    fun createFakeLocation(provider: String = "gps"): Location {
        val location = Location(provider)
        
        // 应用随机偏移（如果启用）
        val (finalLat, finalLng) = applyRandomOffset(latitude, longitude)
        
        location.latitude = finalLat
        location.longitude = finalLng
        location.time = System.currentTimeMillis()
        
        // 设置精度（如果启用）
        if (useAccuracy) {
            location.accuracy = accuracy
        } else {
            location.accuracy = 10.0f // 默认精度
        }
        
        // 设置海拔（如果启用）
        if (useAltitude) {
            location.altitude = altitude
        }
        
        // Android 12+ 特性
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 垂直精度
            if (useVerticalAccuracy) {
                location.verticalAccuracyMeters = verticalAccuracy
            }
            
            // 平均海平面高度
            if (useMeanSeaLevel) {
                location.mslAltitudeMeters = meanSeaLevel
            }
            
            // 平均海平面精度
            if (useMeanSeaLevelAccuracy) {
                location.mslAltitudeAccuracyMeters = meanSeaLevelAccuracy
            }
        }
        
        // 设置速度（如果启用）
        if (useSpeed) {
            location.speed = speed
        }
        
        // Android 13+ 速度精度
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (useSpeedAccuracy) {
                location.speedAccuracyMetersPerSecond = speedAccuracy
            }
        }
        
        // 设置时间戳和模拟标记
        location.elapsedRealtimeNanos = System.nanoTime()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                location.javaClass.getMethod("setIsFromMockProvider", Boolean::class.javaPrimitiveType)
                    ?.invoke(location, true)
            } catch (_: Exception) {}
        }
        
        return location
    }
    
    /**
     * 应用随机偏移
     * @param lat 原始纬度
     * @param lng 原始经度
     * @return 偏移后的坐标对 (纬度，经度)
     */
    private fun applyRandomOffset(lat: Double, lng: Double): Pair<Double, Double> {
        if (!useRandomize || randomizeRadius <= 0) {
            return Pair(lat, lng)
        }
        
        val randomAngle = random.nextDouble() * 2 * Math.PI
        val randomDistance = random.nextDouble() * randomizeRadius
        
        val latOffset = (randomDistance * Math.cos(randomAngle)) / 111000.0
        val lngOffset = (randomDistance * Math.sin(randomAngle)) / (111000.0 * Math.cos(Math.toRadians(lat)))
        
        return Pair(lat + latOffset, lng + lngOffset)
    }
    
    /**
     * GCJ-02 转 WGS-84
     * 高德地图返回的是 GCJ-02 坐标系，需要转换为 WGS-84 才能用于定位伪造
     * 
     * @param gcjLat GCJ-02 纬度
     * @param gcjLng GCJ-02 经度
     * @return WGS-84 坐标 [纬度，经度]
     */
    fun gcj02ToWgs84(gcjLat: Double, gcjLng: Double): Pair<Double, Double> {
        val ee = 0.00669342162296594323
        val a = 6378245.0
        
        val dLat = transformLat(gcjLng - 105.0, gcjLat - 35.0)
        val dLng = transformLng(gcjLng - 105.0, gcjLat - 35.0)
        
        val radLat = gcjLat / 180.0 * Math.PI
        var magic = Math.sin(radLat)
        magic = 1 - ee * magic * magic
        
        val sqrtMagic = Math.sqrt(magic)
        
        var wgsLat = gcjLat - dLat
        var wgsLng = gcjLng - dLng
        
        wgsLat = wgsLat * 180.0 / Math.PI
        wgsLng = wgsLng * 180.0 / Math.PI
        
        wgsLat = (2 * wgsLat - gcjLat)
        wgsLng = (2 * wgsLng - gcjLng)
        
        wgsLat -= (dLat / sqrtMagic) * 180.0 / Math.PI * a * (1 - ee) / (magic * RADIUS_EARTH)
        wgsLng -= (dLng / sqrtMagic) * 180.0 / Math.PI * a * (1 - ee) / (magic * RADIUS_EARTH * Math.cos(radLat))
        
        return Pair(wgsLat, wgsLng)
    }
    
    private fun transformLat(lng: Double, lat: Double): Double {
        var ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
        ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0
        return ret
    }
    
    private fun transformLng(lng: Double, lat: Double): Double {
        var ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
        ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0
        return ret
    }
}

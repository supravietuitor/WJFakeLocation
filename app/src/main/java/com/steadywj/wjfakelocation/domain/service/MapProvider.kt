// MapProvider.kt
package com.steadywj.wjfakelocation.domain.service

import android.content.Context
import com.amap.api.maps.AMap
import com.steadywj.wjfakelocation.xposed.common.LocationUtil
import kotlinx.coroutines.flow.Flow

/**
 * 地图提供商接口
 * 
 * 策略模式：支持多种地图引擎切换
 */
interface MapProvider {
    /**
     * 初始化地图
     */
    fun initialize(context: Context): Any
    
    /**
     * 搜索位置
     */
    fun search(query: String): Flow<LocationSearchResult>
    
    /**
     * GCJ-02 转 WGS-84
     */
    fun gcjToWgs(lat: Double, lng: Double): Pair<Double, Double>
    
    /**
     * 获取地图名称
     */
    fun getMapName(): String
    
    /**
     * 是否支持中国地区
     */
    fun isChinaSupported(): Boolean = true
}

/**
 * 高德地图提供商（已实现）
 */
class AMapProvider : MapProvider {
    
    override fun initialize(context: Context): Any {
        // 高德地图已在 AMapView.kt 中实现
        return AMap::class.java
    }
    
    override fun search(query: String): Flow<LocationSearchResult> {
        // 使用AMapManager 进行地理编码搜索
        // 注意：需要在 ViewModel 中注入 AMapManager
        TODO("需要在 MapViewModel 中集成 AMapManager.geocodeAddress()")
    }
    
    override fun gcjToWgs(lat: Double, lng: Double): Pair<Double, Double> {
        return LocationUtil.gcj02ToWgs84(lat, lng)
    }
    
    override fun getMapName(): String = "高德地图"
}

/**
 * 百度地图提供商（已实现）
 */
class BaiduMapProvider : MapProvider {
    
    override fun initialize(context: Context): Any {
        // 初始化百度地图 SDK
        // 需要在 Application 中调用：SDKInitializer.initialize(context)
        return com.baidu.mapapi.map.BaiduMap::class.java
    }
    
    override fun search(query: String): Flow<LocationSearchResult> {
        // 使用 BaiduMapManager 进行地理编码搜索
        // 注意：需要在 ViewModel 中注入 BaiduMapManager
        TODO("需要在 MapViewModel 中集成 BaiduMapManager.geocodeAddress()")
    }
    
    override fun gcjToWgs(lat: Double, lng: Double): Pair<Double, Double> {
        // 百度坐标系 (BD-09) → WGS-84
        // BD-09 → GCJ-02 → WGS-84
        // 注意：需要依赖注入 BaiduMapManager，这里使用静态方法
        return bd09ToWgs84Static(lat, lng)
    }
    
    /**
     * 静态坐标转换方法（无需依赖注入）
     */
    private fun bd09ToWgs84Static(bdLat: Double, bdLng: Double): Pair<Double, Double> {
        // BD-09 → GCJ-02
        val xPi = Math.PI * 3000.0 / 180.0
        val x = bdLng - 0.0065
        val y = bdLat - 0.006
        val z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * xPi)
        val theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * xPi)
        val gcjLng = z * Math.cos(theta)
        val gcjLat = z * Math.sin(theta)
        
        // GCJ-02 → WGS-84
        return LocationUtil.gcj02ToWgs84(gcjLat, gcjLng)
    }
    
    override fun getMapName(): String = "百度地图"
}

/**
 * Google Maps 提供商（国际版）
 */
class GoogleMapProvider : MapProvider {
    
    override fun initialize(context: Context): Any {
        // TODO: 初始化 Google Play Services
        // MapsInitializer.initialize(context)
        TODO("需要 Google Play Services")
    }
    
    override fun search(query: String): Flow<LocationSearchResult> {
        // TODO: 使用 Geocoding API
        TODO("需要网络 API 密钥")
    }
    
    override fun gcjToWgs(lat: Double, lng: Double): Pair<Double, Double> {
        // Google Maps 使用 WGS-84，无需转换
        return lat to lng
    }
    
    override fun isChinaSupported(): Boolean = false // 中国大陆偏移问题
    
    override fun getMapName(): String = "Google Maps"
}

/**
 * 地图类型枚举
 */
enum class MapType(val displayName: String, val description: String) {
    AMAP("高德地图", "中国地区首选，坐标精确"),
    BAIDU("百度地图", "POI 数据丰富"),
    GOOGLE("Google Maps", "国际通用，国内有偏移")
}

/**
 * 位置搜索结果
 */
data class LocationSearchResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val provider: MapType
)

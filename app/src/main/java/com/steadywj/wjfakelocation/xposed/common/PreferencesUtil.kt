// PreferencesUtil.kt
package com.steadywj.wjfakelocation.xposed.utils

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XposedBridge

/**
 * Xposed 模块偏好设置工具类
 * 用于从 SharedPreferences 读取用户配置
 */
object PreferencesUtil {
    
    private const val TAG = "[PreferencesUtil]"
    private const val SHARED_PREFS_FILE = "wjfakelocation_shared_prefs"
    private var prefs: SharedPreferences? = null
    
    /**
     * 初始化 SharedPreferences
     * 需要在获取到 Context 后调用
     */
    fun init(context: Context) {
        try {
            // 使用 MODE_WORLD_READABLE 以便 UI 应用可以写入
            prefs = context.createPackageContext(
                "com.steadywj.wjfakelocation",
                Context.CONTEXT_IGNORE_SECURITY
            ).getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_WORLD_READABLE)
            
            XposedBridge.log("$TAG SharedPreferences initialized successfully")
        } catch (e: Exception) {
            XposedBridge.log("$TAG Failed to initialize SharedPreferences: ${e.message}")
            prefs = null
        }
    }
    
    /**
     * 检查是否正在运行（伪造中）
     */
    fun getIsPlaying(): Boolean? {
        return prefs?.getBoolean("is_playing", false)
    }
    
    /**
     * 获取选中的位置
     */
    fun getSelectedLocation(): Pair<Double, Double>? {
        val lat = prefs?.getFloat("selected_latitude", Double.MIN_VALUE.toFloat())?.toDouble() ?: Double.MIN_VALUE
        val lng = prefs?.getFloat("selected_longitude", Double.MIN_VALUE.toFloat())?.toDouble() ?: Double.MIN_VALUE
        
        return if (lat != Double.MIN_VALUE && lng != Double.MIN_VALUE) {
            Pair(lat, lng)
        } else {
            null
        }
    }
    
    /**
     * 获取位置地址信息
     */
    fun getSelectedAddress(): String? {
        return prefs?.getString("selected_address", null)
    }
    
    // ===== 精度设置 =====
    
    fun getUseAccuracy(): Boolean? {
        return prefs?.getBoolean("use_accuracy", false)
    }
    
    fun getAccuracy(): Float? {
        return prefs?.getFloat("accuracy", 10.0f)
    }
    
    // ===== 海拔设置 =====
    
    fun getUseAltitude(): Boolean? {
        return prefs?.getBoolean("use_altitude", false)
    }
    
    fun getAltitude(): Float? {
        return prefs?.getFloat("altitude", 50.0f)
    }
    
    // ===== 随机化设置 =====
    
    fun getUseRandomize(): Boolean? {
        return prefs?.getBoolean("use_randomize", false)
    }
    
    fun getRandomizeRadius(): Float? {
        return prefs?.getFloat("randomize_radius", 0.0f)
    }
    
    // ===== 垂直精度设置 =====
    
    fun getUseVerticalAccuracy(): Boolean? {
        return prefs?.getBoolean("use_vertical_accuracy", false)
    }
    
    fun getVerticalAccuracy(): Float? {
        return prefs?.getFloat("vertical_accuracy", 2.0f)
    }
    
    // ===== 平均海平面设置 (Android 12+) =====
    
    fun getUseMeanSeaLevel(): Boolean? {
        return prefs?.getBoolean("use_mean_sea_level", false)
    }
    
    fun getMeanSeaLevel(): Float? {
        return prefs?.getFloat("mean_sea_level", 50.0f)
    }
    
    fun getUseMeanSeaLevelAccuracy(): Boolean? {
        return prefs?.getBoolean("use_mean_sea_level_accuracy", false)
    }
    
    fun getMeanSeaLevelAccuracy(): Float? {
        return prefs?.getFloat("mean_sea_level_accuracy", 2.0f)
    }
    
    // ===== 速度设置 =====
    
    fun getUseSpeed(): Boolean? {
        return prefs?.getBoolean("use_speed", false)
    }
    
    fun getSpeed(): Float? {
        return prefs?.getFloat("speed", 0.0f)
    }
    
    fun getUseSpeedAccuracy(): Boolean? {
        return prefs?.getBoolean("use_speed_accuracy", false)
    }
    
    fun getSpeedAccuracy(): Float? {
        return prefs?.getFloat("speed_accuracy", 0.0f)
    }
    
    // ===== 目标模式设置 =====
    
    fun getTargetMode(): String? {
        return prefs?.getString("target_mode", "GLOBAL")
    }
    
    fun getTargetPackages(): Set<String>? {
        return prefs?.getStringSet("target_packages", emptySet())
    }
}

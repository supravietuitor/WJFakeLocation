// WJFakeLocationApplication.kt
package com.steadywj.wjfakelocation

import android.app.Application
import com.amap.api.services.core.ServiceSettings
import com.baidu.mapapi.SDKInitializer
import com.steadywj.wjfakelocation.common.WJLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用全局上下文
 * 负责初始化第三方 SDK 和全局配置
 */
@HiltAndroidApp
class WJFakeLocationApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化日志系统
        WJLogger.init()
        WJLogger.d("WJFakeLocation 应用启动")
        
        // 初始化高德地图 SDK（预加载以提升性能）
        initAMapSDK()
        
        // 初始化百度地图 SDK（多地图引擎支持）
        initBaiduMapSDK()
        
        WJLogger.i("应用初始化完成")
    }
    
    /**
     * 初始化高德地图 SDK
     * 提前初始化可避免首次使用时的冷启动延迟
     */
    private fun initAMapSDK() {
        try {
            // 初始化地图服务设置
            ServiceSettings.getInstance()
            
            WJLogger.d("高德地图 SDK 初始化成功")
        } catch (e: Exception) {
            WJLogger.e(e, "高德地图 SDK 初始化失败")
        }
    }
    
    /**
     * 初始化百度地图 SDK
     * 支持多地图引擎切换
     */
    private fun initBaiduMapSDK() {
        try {
            // 初始化百度地图 SDK
            SDKInitializer.initialize(this)
            
            WJLogger.d("百度地图 SDK 初始化成功")
        } catch (e: Exception) {
            WJLogger.e(e, "百度地图 SDK 初始化失败")
        }
    }
}

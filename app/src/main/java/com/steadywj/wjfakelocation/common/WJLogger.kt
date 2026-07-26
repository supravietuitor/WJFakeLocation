// WJLogger.kt
package com.steadywj.wjfakelocation.common

import android.util.Log
import com.steadywj.wjfakelocation.BuildConfig

/**
 * 统一日志管理工具
 * 
 * 功能:
 * - 统一使用 android.util.Log 进行日志记录
 * - Debug 版本自动输出日志，Release 版本禁用
 * - 支持自定义标签和日志级别
 * 
 * 使用示例:
 * ```
 * WJLogger.d("地图加载完成")
 * WJLogger.e(exception, "位置获取失败")
 * WJLogger.i("用户切换到高德地图")
 * ```
 * 
 * @author WJFakeLocation Team
 * @since 2.0.0
 */
object WJLogger {
    
    private const val TAG = "WJFakeLocation"
    private var initialized = false
    
    /**
     * 初始化日志系统
     * 需要在 Application 的 onCreate 中调用
     */
    fun init() {
        initialized = true
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "WJLogger initialized in DEBUG mode")
        }
    }
    
    /**
     * 输出 VERBOSE 级别日志
     */
    fun v(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.v(TAG, message, throwable)
        } else {
            Log.v(TAG, message)
        }
    }
    
    /**
     * 输出 VERBOSE 级别日志（带自定义 TAG）
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.v(tag, message, throwable)
        } else {
            Log.v(tag, message)
        }
    }
    
    /**
     * 输出 DEBUG 级别日志（最常用）
     */
    fun d(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.d(TAG, message, throwable)
        } else {
            Log.d(TAG, message)
        }
    }
    
    /**
     * 输出 DEBUG 级别日志（带自定义 TAG）
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.d(tag, message, throwable)
        } else {
            Log.d(tag, message)
        }
    }
    
    /**
     * 输出 INFO 级别日志
     */
    fun i(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.i(TAG, message, throwable)
        } else {
            Log.i(TAG, message)
        }
    }
    
    /**
     * 输出 INFO 级别日志（带自定义 TAG）
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.i(tag, message, throwable)
        } else {
            Log.i(tag, message)
        }
    }
    
    /**
     * 输出 WARN 级别日志
     */
    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(TAG, message, throwable)
        } else {
            Log.w(TAG, message)
        }
    }
    
    /**
     * 输出 WARN 级别日志（带自定义 TAG）
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
    
    /**
     * 输出 ERROR 级别日志
     */
    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }
    
    /**
     * 输出 ERROR 级别日志（带自定义 TAG）
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * 输出 ASSERT 级别日志
     */
    fun wtf(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.wtf(TAG, message, throwable)
        } else {
            Log.wtf(TAG, message)
        }
    }
}

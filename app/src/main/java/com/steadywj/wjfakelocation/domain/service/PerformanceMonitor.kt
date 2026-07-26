// PerformanceMonitor.kt
package com.steadywj.wjfakelocation.domain.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 性能监控器
 * 
 * 功能:
 * - 方法执行时间统计
 * - 内存使用监控
 * - FPS 帧率监控
 * - 卡顿检测
 */
@Singleton
class PerformanceMonitor @Inject constructor() {
    
    /** 性能指标数据 */
    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()
    
    /** 方法调用记录 */
    private val methodTimings = mutableMapOf<String, MutableList<Double>>()
    
    /** 是否启用监控 */
    var isEnabled = true
    
    /**
     * 开始计时
     */
    fun startTiming(methodName: String): Long {
        if (!isEnabled) return System.currentTimeMillis()
        
        Log.d(TAG, "⏱️ Start: $methodName")
        return System.nanoTime()
    }
    
    /**
     * 结束计时并记录
     */
    fun endTiming(methodName: String, startTime: Long) {
        if (!isEnabled) return
        
        val duration = (System.nanoTime() - startTime) / 1_000_000.0 // 转换为毫秒
        
        // 记录到列表
        val timings = methodTimings.getOrPut(methodName) { mutableListOf() }
        timings.add(duration)
        
        // 保留最近 100 次记录
        if (timings.size > 100) {
            timings.removeAt(0)
        }
        
        // 更新指标
        updateMetrics(methodName, duration)
        
        // 慢方法警告（>1000ms）
        if (duration > 1000) {
            Log.w(TAG, "⚠️ Slow method: $methodName took ${String.format("%.2f", duration)}ms")
        }
    }
    
    /**
     * 获取方法平均执行时间
     */
    fun getAverageTime(methodName: String): Double {
        val timings = methodTimings[methodName] ?: return 0.0
        return if (timings.isEmpty()) 0.0 else timings.average()
    }
    
    /**
     * 获取方法最大执行时间
     */
    fun getMaxTime(methodName: String): Double {
        val timings = methodTimings[methodName] ?: return 0.0
        return timings.maxOrNull() ?: 0.0
    }
    
    /**
     * 记录内存使用
     */
    suspend fun recordMemoryUsage() = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        val currentMetrics = _metrics.value
        _metrics.value = currentMetrics.copy(
            usedMemoryBytes = usedMemory,
            totalMemoryBytes = totalMemory,
            memoryUsagePercent = (usedMemory.toDouble() / totalMemory * 100).toFloat()
        )
    }
    
    /**
     * 记录帧率
     */
    fun recordFrameTime(frameTimeMs: Long) {
        if (!isEnabled) return
        
        val currentMetrics = _metrics.value
        val fps = if (frameTimeMs > 0) (1000.0 / frameTimeMs).toInt() else 60
        
        _metrics.value = currentMetrics.copy(
            currentFps = fps,
            averageFrameTime = frameTimeMs.toDouble()
        )
        
        // 卡顿检测（帧时间 > 16.67ms 即低于 60FPS）
        if (frameTimeMs > 16.67) {
            Log.w(TAG, "⚠️ Jank detected: ${String.format("%.2f", frameTimeMs)}ms (${fps} FPS)")
        }
    }
    
    /**
     * 记录网络请求耗时
     */
    fun recordNetworkRequest(endpoint: String, durationMs: Double, success: Boolean) {
        if (!isEnabled) return
        
        val currentMetrics = _metrics.value
        val networkStats = currentMetrics.networkStats
        
        val newStats = networkStats.copy(
            totalRequests = networkStats.totalRequests + 1,
            successfulRequests = if (success) networkStats.successfulRequests + 1 else networkStats.successfulRequests,
            averageLatency = ((networkStats.averageLatency * networkStats.totalRequests) + durationMs) / 
                            (networkStats.totalRequests + 1)
        )
        
        _metrics.value = currentMetrics.copy(networkStats = newStats)
    }
    
    /**
     * 重置统计数据
     */
    fun resetMetrics() {
        methodTimings.clear()
        _metrics.value = PerformanceMetrics()
    }
    
    /**
     * 导出性能报告
     */
    fun exportReport(): String {
        val report = buildString {
            appendLine("=== 性能监控报告 ===")
            appendLine()
            
            appendLine("📊 内存使用:")
            val metrics = _metrics.value
            appendLine("   已用：${formatBytes(metrics.usedMemoryBytes)}")
            appendLine("   总计：${formatBytes(metrics.totalMemoryBytes)}")
            appendLine("   占比：${String.format("%.1f", metrics.memoryUsagePercent)}%")
            appendLine()
            
            appendLine("🎬 帧率:")
            appendLine("   当前：${metrics.currentFps} FPS")
            appendLine("   平均帧时间：${String.format("%.2f", metrics.averageFrameTime)}ms")
            appendLine()
            
            appendLine("🌐 网络:")
            appendLine("   总请求：${metrics.networkStats.totalRequests}")
            appendLine("   成功：${metrics.networkStats.successfulRequests}")
            appendLine("   平均延迟：${String.format("%.2f", metrics.networkStats.averageLatency)}ms")
            appendLine()
            
            appendLine("⏱️ 方法执行时间 (Top 10):")
            methodTimings.entries
                .sortedByDescending { it.value.average() }
                .take(10)
                .forEach { (method, timings) ->
                    appendLine("   $method: avg=${String.format("%.2f", timings.average())}ms, max=${String.format("%.2f", timings.maxOrNull() ?: 0)}ms")
                }
        }
        
        return report
    }
    
    // ==================== 内部方法 ====================
    
    private fun updateMetrics(methodName: String, duration: Double) {
        val currentMetrics = _metrics.value
        
        // 更新慢方法计数
        val slowMethods = if (duration > 100) {
            currentMetrics.slowMethodCount + 1
        } else {
            currentMetrics.slowMethodCount
        }
        
        _metrics.value = currentMetrics.copy(slowMethodCount = slowMethods)
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))} MB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
    
    companion object {
        private const val TAG = "PerformanceMonitor"
    }
}

/**
 * 性能指标
 */
data class PerformanceMetrics(
    val usedMemoryBytes: Long = 0L,
    val totalMemoryBytes: Long = 0L,
    val memoryUsagePercent: Float = 0f,
    val currentFps: Int = 60,
    val averageFrameTime: Double = 16.67,
    val slowMethodCount: Int = 0,
    val networkStats: NetworkStats = NetworkStats()
)

/**
 * 网络统计
 */
data class NetworkStats(
    val totalRequests: Int = 0,
    val successfulRequests: Int = 0,
    val averageLatency: Double = 0.0
)

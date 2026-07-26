// WifiHook.kt
package com.steadywj.wjfakelocation.xposed.hooks

import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import com.steadywj.wjfakelocation.data.model.FakeWifiInfo
import com.steadywj.wjfakelocation.data.model.SecurityType
import com.steadywj.wjfakelocation.data.model.WifiBand
import java.net.Inet4Address
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * WiFi 信息 Hook
 * 
 * 功能:
 * - 伪造 WiFi 扫描列表
 * - 修改当前连接的 WiFi 信息
 * - 支持 WPA/WPA2/WPA3 加密类型
 */
class WifiHook : IXposedHookLoadPackage {
    
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook WifiManager.getScanResults()
        hookGetScanResults(lpparam)
        
        // Hook WifiManager.getConnectionInfo()
        hookGetConnectionInfo(lpparam)
        
        // Hook WifiManager.getConfiguredNetworks()
        hookGetConfiguredNetworks(lpparam)
    }
    
    /**
     * Hook getScanResults() - 获取周边 WiFi 列表
     */
    private fun hookGetScanResults(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClass(WifiManager::class.java.name, lpparam.classLoader)
            
            XposedBridge.hookAllMethods(clazz, "getScanResults", object : XC_MethodReplacement() {
                @Throws(Throwable::class.java)
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    if (!isFakeWifiEnabled()) {
                        return param.method.invoke(param.thisObject, *param.args)
                    }
                    
                    try {
                        val fakeWifiList = createFakeScanResults()
                        XposedBridge.log("[WifiHook] 返回伪造 WiFi 列表：${fakeWifiList.size}个")
                        return fakeWifiList
                    } catch (e: Exception) {
                        XposedBridge.log("[WifiHook] 创建假 WiFi 列表失败：${e.message}")
                        return param.method.invoke(param.thisObject, *param.args)
                    }
                }
            })
        } catch (e: Throwable) {
            XposedBridge.log("[WifiHook] Hook getScanResults 失败：${e.message}")
        }
    }
    
    /**
     * Hook getConnectionInfo() - 获取当前连接的 WiFi 信息
     */
    private fun hookGetConnectionInfo(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClass(WifiManager::class.java.name, lpparam.classLoader)
            
            XposedBridge.hookAllMethods(clazz, "getConnectionInfo", object : XC_MethodReplacement() {
                @Throws(Throwable::class.java)
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    if (!isFakeWifiEnabled()) {
                        return param.method.invoke(param.thisObject, *param.args)
                    }
                    
                    try {
                        val fakeInfo = createFakeConnectionInfo()
                        XposedBridge.log("[WifiHook] 返回伪造连接信息")
                        return fakeInfo
                    } catch (e: Exception) {
                        XposedBridge.log("[WifiHook] 创建假连接信息失败：${e.message}")
                        return param.method.invoke(param.thisObject, *param.args)
                    }
                }
            })
        } catch (e: Throwable) {
            XposedBridge.log("[WifiHook] Hook getConnectionInfo 失败：${e.message}")
        }
    }
    
    /**
     * Hook getConfiguredNetworks() - 获取已保存的 WiFi 网络
     */
    private fun hookGetConfiguredNetworks(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClass(WifiManager::class.java.name, lpparam.classLoader)
            
            XposedBridge.hookAllMethods(clazz, "getConfiguredNetworks", object : XC_MethodReplacement() {
                @Throws(Throwable::class.java)
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    if (!isFakeWifiEnabled()) {
                        return param.method.invoke(param.thisObject, *param.args)
                    }
                    
                    try {
                        val fakeConfigs = createFakeConfiguredNetworks()
                        XposedBridge.log("[WifiHook] 返回伪造配置网络列表：${fakeConfigs.size}个")
                        return fakeConfigs
                    } catch (e: Exception) {
                        XposedBridge.log("[WifiHook] 创建假配置网络失败：${e.message}")
                        return param.method.invoke(param.thisObject, *param.args)
                    }
                }
            })
        } catch (e: Throwable) {
            XposedBridge.log("[WifiHook] Hook getConfiguredNetworks 失败：${e.message}")
        }
    }
    
    // ==================== 创建伪造数据 ====================
    
    /**
     * 创建假 WiFi 扫描列表
     */
    private fun createFakeScanResults(): List<ScanResult> {
        val fakeWifis = mutableListOf<ScanResult>()
        
        FakeWifiInfo.getFakeWifiList().forEachIndexed { index, wifiInfo ->
            try {
                val scanResult = ScanResult()
                
                // SSID（需要添加引号）
                scanResult.SSID = "\"${wifiInfo.ssid}\""
                
                // BSSID（MAC 地址格式）
                scanResult.BSSID = wifiInfo.bssid ?: generateRandomMac()
                
                // 频率（2.4GHz vs 5GHz）
                scanResult.frequency = when (wifiInfo.band) {
                    WifiBand.GHZ_2 -> 2437 // Channel 6
                    WifiBand.GHZ_5 -> 5180 // Channel 36
                    WifiBand.GHZ_6 -> 5955 // WiFi 6E
                }
                
                // 信号强度（-30 到 -100 dBm）
                scanResult.level = wifiInfo.signalStrength ?: -50
                
                // 加密方式
                scanResult.capabilities = buildCapabilities(wifiInfo.securityType)
                
                // 时间戳
                XposedHelpers.setLongField(scanResult, "timestampNanos", System.nanoTime())
                
                // Channel width (80MHz, 160MHz)
                if (wifiInfo.band == WifiBand.GHZ_5 || wifiInfo.band == WifiBand.GHZ_6) {
                    scanResult.channelWidth = ScanResult.CHANNEL_WIDTH_80MHZ
                }
                
                fakeWifis.add(scanResult)
            } catch (e: Exception) {
                XposedBridge.log("[WifiHook] 创建 WiFi 失败：${e.message}")
            }
        }
        
        return fakeWifis
    }
    
    /**
     * 创建假连接信息
     */
    private fun createFakeConnectionInfo(): WifiInfo {
        val primaryWifi = FakeWifiInfo.getPrimaryWifi()
        
        val wifiInfo = WifiInfo()
        
        // SSID
        XposedHelpers.callMethod(wifiInfo, "setSSID", "\"${primaryWifi.ssid}\"")
        
        // BSSID
        XposedHelpers.callMethod(wifiInfo, "setBSSID", primaryWifi.bssid ?: generateRandomMac())
        
        // MAC 地址（设备自身）
        XposedHelpers.callMethod(wifiInfo, "setMacAddress", "02:00:00:00:00:00")
        
        // Network ID
        XposedHelpers.callMethod(wifiInfo, "setNetworkId", 1)
        
        // 信号强度
        XposedHelpers.callMethod(wifiInfo, "setRssi", primaryWifi.signalStrength ?: -50)
        
        // 传输速度
        XposedHelpers.callMethod(wifiInfo, "setLinkSpeed", 300) // Mbps
        
        // 频率
        XposedHelpers.callMethod(wifiInfo, "setFrequency", 
            when (primaryWifi.band) {
                WifiBand.GHZ_2 -> 2437
                WifiBand.GHZ_5 -> 5180
                WifiBand.GHZ_6 -> 5955
            }
        )
        
        // IP 地址
        XposedHelpers.callMethod(wifiInfo, "setIpAddress", 
            Inet4Address.getByName("192.168.1.100").address
        )
        
        return wifiInfo
    }
    
    /**
     * 创建假配置网络列表
     */
    private fun createFakeConfiguredNetworks(): List<Any> {
        val fakeConfigs = mutableListOf<Any>()
        
        // TODO: 使用 WifiConfiguration 类创建配置网络
        // 由于该类隐藏，需要使用反射或 XposedHelpers
        
        FakeWifiInfo.getFakeWifiList().take(3).forEach { wifiInfo ->
            try {
                val configClass = XposedHelpers.findClass(
                    "android.net.wifi.WifiConfiguration",
                    null
                )
                
                val config = XposedHelpers.newInstance(configClass)
                
                // SSID
                XposedHelpers.setIntField(config, "networkId", 1)
                XposedHelpers.callMethod(config, "setSSID", "\"${wifiInfo.ssid}\"")
                
                // 加密协议
                when (wifiInfo.securityType) {
                    SecurityType.WPA2_PSK -> {
                        XposedHelpers.setObjectField(config, "protoTypes", booleanArrayOf(true, false, false))
                    }
                    SecurityType.WPA3_SAE -> {
                        XposedHelpers.setObjectField(config, "protoTypes", booleanArrayOf(false, true, false))
                    }
                    else -> {}
                }
                
                fakeConfigs.add(config)
            } catch (e: Exception) {
                XposedBridge.log("[WifiHook] 创建配置网络失败：${e.message}")
            }
        }
        
        return fakeConfigs
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 构建加密能力字符串
     */
    private fun buildCapabilities(securityType: SecurityType): String {
        return when (securityType) {
            SecurityType.OPEN -> "[ESS]"
            SecurityType.WEP -> "[WEP][ESS]"
            SecurityType.WPA_PSK -> "[WPA-PSK-CCMP][ESS]"
            SecurityType.WPA2_PSK -> "[WPA2-PSK-CCMP][ESS]"
            SecurityType.WPA3_SAE -> "[WPA3-SAE][ESS]"
            SecurityType.WPA2_WPA3 -> "[WPA2-PSK-CCMP][WPA3-SAE][ESS]"
        }
    }
    
    /**
     * 生成随机 MAC 地址
     */
    private fun generateRandomMac(): String {
        return buildString {
            for (i in 0 until 6) {
                if (i > 0) append(":")
                append(String.format("%02X", (Math.random() * 256).toInt()))
            }
        }
    }
    
    private fun isFakeWifiEnabled(): Boolean {
        // 从 Preferences 读取开关状态
        // TODO: 注入 PreferencesRepository 或使用静态方法读取
        return true // 默认启用
    }
}

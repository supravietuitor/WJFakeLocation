// TelephonyHook.kt
package com.steadywj.wjfakelocation.xposed.hooks

import android.telephony.CellIdentity
import android.telephony.CellSignalStrength
import android.telephony.TelephonyManager
import com.steadywj.wjfakelocation.data.model.CellType
import com.steadywj.wjfakelocation.data.model.FakeCellInfo
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 基站信息 Hook
 * 
 * 功能:
 * - 伪造基站列表
 * - 修改服务小区信息
 * - 支持 2G/3G/4G/5G 全制式
 */
class TelephonyHook : IXposedHookLoadPackage {
    
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook TelephonyManager.getAllCellInfo()
        hookGetAllCellInfo(lpparam)
        
        // Hook TelephonyManager.getCellLocation() (API 33+)
        hookGetCellLocation(lpparam)
        
        // Hook SignalStrength 相关方法
        hookSignalStrength(lpparam)
    }
    
    /**
     * Hook getAllCellInfo() - 获取所有基站信息
     */
    private fun hookGetAllCellInfo(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClass(TelephonyManager::class.java.name, lpparam.classLoader)
            
            XposedBridge.hookAllMethods(clazz, "getAllCellInfo", object : XC_MethodReplacement() {
                @Throws(Throwable::class)
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    // 检查是否启用基站伪造
                    if (!isFakeCellEnabled()) {
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                    }
                    
                    try {
                        val fakeCells = createFakeCellInfoList()
                        XposedBridge.log("[TelephonyHook] 返回伪造基站列表：${fakeCells.size}个")
                        return fakeCells
                    } catch (e: Exception) {
                        XposedBridge.log("[TelephonyHook] 创建假基站失败：${e.message}")
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                    }
                }
            })
        } catch (e: Throwable) {
            XposedBridge.log("[TelephonyHook] Hook getAllCellInfo 失败：${e.message}")
        }
    }
    
    /**
     * Hook getCellLocation() - 获取服务小区位置 (API 33+)
     */
    private fun hookGetCellLocation(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClass(TelephonyManager::class.java.name, lpparam.classLoader)
            
            XposedBridge.hookAllMethods(clazz, "getCellLocation", object : XC_MethodReplacement() {
                @Throws(Throwable::class)
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    if (!isFakeCellEnabled()) {
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                    }
                    
                    try {
                        val fakeLocation = createFakeCellLocation()
                        XposedBridge.log("[TelephonyHook] 返回伪造服务小区位置")
                        return fakeLocation
                    } catch (e: Exception) {
                        XposedBridge.log("[TelephonyHook] 创建假服务小区失败：${e.message}")
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                    }
                }
            })
        } catch (e: Throwable) {
            XposedBridge.log("[TelephonyHook] Hook getCellLocation 失败：${e.message}")
        }
    }
    
    /**
     * Hook SignalStrength - 信号强度
     */
    private fun hookSignalStrength(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val signalStrengthClass = XposedHelpers.findClass(
                "android.telephony.SignalStrength",
                lpparam.classLoader
            )
            
            // Hook getLevel() - 信号级别
            XposedBridge.hookAllMethods(signalStrengthClass, "getLevel", object : XC_MethodReplacement() {
                @Throws(Throwable::class)
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    if (!isFakeCellEnabled()) {
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                    }
                    
                    // 返回满格信号（4 格）
                    return 4
                }
            })
            
            // Hook getCdmaDbm, cdmaEcio 等具体方法
            hookSignalStrengthMethods(signalStrengthClass, lpparam)
            
        } catch (e: Throwable) {
            XposedBridge.log("[TelephonyHook] Hook SignalStrength 失败：${e.message}")
        }
    }
    
    /**
     * Hook 信号强度具体方法
     */
    private fun hookSignalStrengthMethods(clazz: Class<*>, lpparam: XC_LoadPackage.LoadPackageParam) {
        val methodsToHook = listOf(
            "getCdmaDbm",
            "getCdmaEcio",
            "getEvdoDbm",
            "getEvdoEcio",
            "getGsmSignalStrength",
            "getGsmBitErrorRate",
            "getLteSignalStrength",
            "getLteRsrp",
            "getLteRsrq",
            "getLteRssnr",
            "getLteCqi"
        )
        
        methodsToHook.forEach { methodName ->
            try {
                XposedBridge.hookAllMethods(clazz, methodName, object : XC_MethodReplacement() {
                    @Throws(Throwable::class)
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        if (!isFakeCellEnabled()) {
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                        
                        // 返回良好的信号值
                        return when (methodName) {
                            "getCdmaDbm", "getEvdoDbm", "getLteRsrp" -> -75 // dBm，值越大越好
                            "getCdmaEcio", "getEvdoEcio" -> -5 // dB，值越大越好
                            "getGsmSignalStrength", "getLteSignalStrength" -> 31 // 最大值
                            "getGsmBitErrorRate" -> 0 // 无误码
                            "getLteRsrq" -> -5 // dB
                            "getLteRssnr" -> 20 // dB
                            "getLteCqi" -> 15 // 最大值
                            else -> XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                    }
                })
            } catch (e: Throwable) {
                // 忽略不存在的方法
            }
        }
    }
    
    /**
     * 创建假基站列表
     */
    private fun createFakeCellInfoList(): List<Any> {
        val fakeCells = mutableListOf<Any>()
        
        // 从配置读取假基站信息
        val fakeCellInfos = FakeCellInfo.getFakeCells()
        
        fakeCellInfos.forEachIndexed { index, fakeInfo ->
            try {
                val cellInfo = when (fakeInfo.type) {
                    CellType.GSM -> createGsmCellInfo(fakeInfo, index == 0)
                    CellType.CDMA -> createCdmaCellInfo(fakeInfo, index == 0)
                    CellType.LTE -> createLteCellInfo(fakeInfo, index == 0)
                    CellType.WCDMA -> createWcdmaCellInfo(fakeInfo, index == 0)
                    CellType.TDSCDMA -> createTdscdmaCellInfo(fakeInfo, index == 0)
                    CellType.NR -> createNrCellInfo(fakeInfo, index == 0)
                }
                fakeCells.add(cellInfo)
            } catch (e: Exception) {
                XposedBridge.log("[TelephonyHook] 创建${fakeInfo.type}基站失败：${e.message}")
            }
        }
        
        return fakeCells
    }
    
    /**
     * 创建假的服务小区位置
     */
    private fun createFakeCellLocation(): Any {
        val fakeInfo = FakeCellInfo.getPrimaryCell()
        
        return when (fakeInfo.type) {
            CellType.GSM -> createGsmCellLocation(fakeInfo)
            CellType.CDMA -> createGsmCellLocation(fakeInfo)
            CellType.LTE -> createLteCellLocation(fakeInfo)
            CellType.WCDMA -> createGsmCellLocation(fakeInfo)
            CellType.TDSCDMA -> createGsmCellLocation(fakeInfo)
            CellType.NR -> createGsmCellLocation(fakeInfo)
        }
    }
    
    // ==================== 创建各类型基站信息 ====================
    
    private fun createGsmCellInfo(fakeInfo: FakeCellInfo, isRegistered: Boolean): Any {
        val cellInfoGsm = XposedHelpers.newInstance(Class.forName("android.telephony.CellInfoGsm"))
        XposedHelpers.callMethod(cellInfoGsm, "setRegistered", isRegistered)
        
        val cellIdentity = XposedHelpers.newInstance(Class.forName("android.telephony.CellIdentityGsm"),
            fakeInfo.mcc?.toIntOrNull() ?: 460,
            fakeInfo.mnc?.toIntOrNull() ?: 0,
            fakeInfo.cid ?: 0,
            fakeInfo.lac ?: 0,
            null as java.util.Set<String>?
        )
        
        XposedHelpers.callMethod(cellInfoGsm, "setCellIdentity", cellIdentity)
        
        return cellInfoGsm
    }
    
    private fun createCdmaCellInfo(fakeInfo: FakeCellInfo, isRegistered: Boolean): Any {
        val cellInfoCdma = XposedHelpers.newInstance(Class.forName("android.telephony.CellInfoCdma"))
        XposedHelpers.callMethod(cellInfoCdma, "setRegistered", isRegistered)
        
        val cellIdentity = XposedHelpers.newInstance(Class.forName("android.telephony.CellIdentityCdma"),
            fakeInfo.basestationId ?: 0,
            fakeInfo.longitude ?: 0.0,
            fakeInfo.latitude ?: 0.0,
            null as String?
        )
        
        XposedHelpers.callMethod(cellInfoCdma, "setCellIdentity", cellIdentity)
        
        return cellInfoCdma
    }
    
    private fun createLteCellInfo(fakeInfo: FakeCellInfo, isRegistered: Boolean): Any {
        val cellInfoLte = XposedHelpers.newInstance(Class.forName("android.telephony.CellInfoLte"))
        XposedHelpers.callMethod(cellInfoLte, "setRegistered", isRegistered)
        
        val cellIdentity = XposedHelpers.newInstance(Class.forName("android.telephony.CellIdentityLte"),
            fakeInfo.mcc?.toIntOrNull() ?: 460,
            fakeInfo.mnc?.toIntOrNull() ?: 0,
            fakeInfo.ci ?: fakeInfo.cid ?: 0,
            fakeInfo.tac ?: fakeInfo.lac ?: 0,
            0,
            null as java.util.Set<String>?
        )
        
        XposedHelpers.callMethod(cellInfoLte, "setCellIdentity", cellIdentity)
        
        return cellInfoLte
    }
    
    private fun createWcdmaCellInfo(fakeInfo: FakeCellInfo, isRegistered: Boolean): Any {
        val cellInfoWcdma = XposedHelpers.newInstance(Class.forName("android.telephony.CellInfoWcdma"))
        XposedHelpers.callMethod(cellInfoWcdma, "setRegistered", isRegistered)
        
        val cellIdentity = XposedHelpers.newInstance(Class.forName("android.telephony.CellIdentityWcdma"),
            fakeInfo.mcc?.toIntOrNull() ?: 460,
            fakeInfo.mnc?.toIntOrNull() ?: 0,
            fakeInfo.cid ?: 0,
            fakeInfo.lac ?: 0,
            fakeInfo.psc ?: 0,
            null as java.util.Set<String>?
        )
        
        XposedHelpers.callMethod(cellInfoWcdma, "setCellIdentity", cellIdentity)
        
        return cellInfoWcdma
    }
    
    private fun createTdscdmaCellInfo(fakeInfo: FakeCellInfo, isRegistered: Boolean): Any {
        val cellInfoTdscdma = XposedHelpers.newInstance(Class.forName("android.telephony.CellInfoTdscdma"))
        XposedHelpers.callMethod(cellInfoTdscdma, "setRegistered", isRegistered)
        
        val cellIdentity = XposedHelpers.newInstance(Class.forName("android.telephony.CellIdentityTdscdma"),
            fakeInfo.mcc?.toIntOrNull() ?: 460,
            fakeInfo.mnc?.toIntOrNull() ?: 0,
            fakeInfo.cid ?: 0,
            fakeInfo.lac ?: 0,
            fakeInfo.cpid ?: 0,
            null as java.util.Set<String>?
        )
        
        XposedHelpers.callMethod(cellInfoTdscdma, "setCellIdentity", cellIdentity)
        
        return cellInfoTdscdma
    }
    
    private fun createNrCellInfo(fakeInfo: FakeCellInfo, isRegistered: Boolean): Any {
        val cellInfoNr = XposedHelpers.newInstance(Class.forName("android.telephony.CellInfoNr"))
        XposedHelpers.callMethod(cellInfoNr, "setRegistered", isRegistered)
        
        val cellIdentity = XposedHelpers.newInstance(Class.forName("android.telephony.CellIdentityNr"),
            fakeInfo.mcc?.toIntOrNull() ?: 460,
            fakeInfo.mnc?.toIntOrNull() ?: 0,
            fakeInfo.nci ?: fakeInfo.cid ?: 0,
            fakeInfo.tac ?: fakeInfo.lac ?: 0,
            null as java.util.Set<String>?
        )
        
        XposedHelpers.callMethod(cellInfoNr, "setCellIdentity", cellIdentity)
        
        return cellInfoNr
    }
    
    // ==================== CellLocation 创建 ====================
    
    private fun createGsmCellLocation(fakeInfo: FakeCellInfo): Any {
        return XposedHelpers.newInstance(
            XposedHelpers.findClass("android.telephony.gsm.GsmCellLocation", null),
            fakeInfo.lac ?: 0,
            fakeInfo.cid ?: 0
        )
    }
    
    private fun createLteCellLocation(fakeInfo: FakeCellInfo): Any {
        return createGsmCellLocation(fakeInfo)
    }
    
    // ... 其他类型类似
    
    // ==================== 工具方法 ====================
    
    private fun isFakeCellEnabled(): Boolean {
        // 从 Preferences 读取开关状态
        // TODO: 注入 PreferencesRepository 或使用静态方法读取
        return true // 默认启用
    }
}

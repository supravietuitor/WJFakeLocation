// MainHook.kt
package com.steadywj.wjfakelocation.xposed

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.steadywj.wjfakelocation.xposed.hooks.LocationApiHooks
import com.steadywj.wjfakelocation.xposed.hooks.SystemServicesHooks
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {
    companion object {
        const val MANAGER_APP_PACKAGE_NAME = "com.steadywj.wjfakelocation"
    }
    private val tag = "[WJFakeLocation-Hook]"

    lateinit var context: Context

    private var locationApiHooks: LocationApiHooks? = null
    private var systemServicesHooks: SystemServicesHooks? = null

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // 避免 hook 自身应用导致递归
        if (lpparam.packageName == MANAGER_APP_PACKAGE_NAME) return

        // 如果未启用则不执行 hook
        if (!isPlaying()) return

        // 在 android 进程中 hook 系统服务
        if (lpparam.packageName == "android") {
            systemServicesHooks = SystemServicesHooks(lpparam).also { it.initHooks() }
            XposedBridge.log("$tag System services hook initialized")
        }

        initHookingLogic(lpparam)
    }

    private fun isPlaying(): Boolean {
        // 从 SharedPreferences 读取状态
        // TODO: 注入 PreferencesRepository 或使用静态方法读取
        return true // 默认启用
    }

    private fun initHookingLogic(lpparam: LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Instrumentation",
                lpparam.classLoader,
                "callApplicationOnCreate",
                Application::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        context = (param.args[0] as Application).applicationContext.also {
                            XposedBridge.log("$tag Target app context acquired successfully")
                            Toast.makeText(it, "WJFakeLocation 已激活!", Toast.LENGTH_SHORT).show()
                        }
                        
                        locationApiHooks = LocationApiHooks(lpparam).also { 
                            it.initHooks() 
                        }
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("$tag Error initializing hook logic: ${e.message}")
        }
    }
}

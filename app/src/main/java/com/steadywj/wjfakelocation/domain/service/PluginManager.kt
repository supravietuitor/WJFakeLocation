// PluginManager.kt
package com.steadywj.wjfakelocation.domain.service

import android.content.Context
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plugin manager
 */
@Singleton
class PluginManager @Inject constructor(
    private val context: Context
) {

    private val _installedPlugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val installedPlugins: Flow<List<PluginInfo>> = _installedPlugins.asStateFlow()

    private val pluginDir: File by lazy {
        File(context.filesDir, "plugins").apply {
            if (!exists()) mkdirs()
        }
    }

    private val optimizedDir: File by lazy {
        File(context.cacheDir, "plugin_dex").apply {
            if (!exists()) mkdirs()
        }
    }

    private val pluginClassLoaders = mutableMapOf<String, DexClassLoader>()

    suspend fun installPlugin(pluginFile: File): Result<PluginInfo> {
        return withContext(Dispatchers.IO) {
            try {
                if (!verifyApkSignature(pluginFile)) {
                    return@withContext Result.failure(Exception("Plugin signature verification failed"))
                }

                val pluginInfo = parsePluginInfo(pluginFile)

                val destFile = File(pluginDir, "${pluginInfo.packageName}.apk")
                pluginFile.copyTo(destFile, overwrite = true)

                val classLoader = DexClassLoader(
                    destFile.absolutePath,
                    optimizedDir.absolutePath,
                    null,
                    this@PluginManager.javaClass.classLoader
                )

                pluginClassLoaders[pluginInfo.packageName] = classLoader

                _installedPlugins.value = _installedPlugins.value + pluginInfo

                Result.success(pluginInfo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun uninstallPlugin(packageName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val pluginFile = File(pluginDir, "$packageName.apk")
                if (pluginFile.exists()) {
                    pluginFile.delete()
                }

                pluginClassLoaders.remove(packageName)

                _installedPlugins.value = _installedPlugins.value.filter {
                    it.packageName != packageName
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun setPluginEnabled(packageName: String, enabled: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _installedPlugins.value = _installedPlugins.value.map { plugin ->
                    if (plugin.packageName == packageName) {
                        plugin.copy(enabled = enabled)
                    } else {
                        plugin
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun loadPluginClass(packageName: String, className: String): Any? {
        val classLoader = pluginClassLoaders[packageName] ?: return null

        return try {
            val clazz = classLoader.loadClass(className)
            clazz.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun executeLuaScript(scriptContent: String, params: Map<String, Any>): Result<Any?> {
        return withContext(Dispatchers.IO) {
            try {
                val globals: Globals = JsePlatform.standardGlobals()

                val luaFunction = globals.load(scriptContent)

                params.forEach { (key, value) ->
                    when (value) {
                        is String -> globals.set(key, LuaValue.valueOf(value))
                        is Int -> globals.set(key, LuaValue.valueOf(value))
                        is Double -> globals.set(key, LuaValue.valueOf(value))
                        is Boolean -> globals.set(key, LuaValue.valueOf(value))
                        else -> globals.set(key, LuaValue.valueOf(value.toString()))
                    }
                }

                val result = luaFunction.call()

                Result.success(result.tojstring())
            } catch (e: Exception) {
                Result.failure(Exception("Lua script execution failed: ${e.message}"))
            }
        }
    }

    suspend fun executeJavaScript(scriptContent: String, params: Map<String, Any>): Result<Any?> {
        return withContext(Dispatchers.IO) {
            try {
                val webView = android.webkit.WebView(context)
                val result = kotlin.coroutines.suspendCoroutine<Any?> { continuation ->
                    webView.evaluateJavascript(scriptContent) { result ->
                        continuation.resumeWith(Result.success(result))
                    }
                }
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getPluginHookInterface(packageName: String): IPluginHook? {
        val hookInstance = loadPluginClass(packageName, "${packageName}.PluginHook")
        return hookInstance as? IPluginHook
    }

    suspend fun refreshPluginList() {
        return withContext(Dispatchers.IO) {
            val plugins = mutableListOf<PluginInfo>()

            pluginDir.listFiles { file ->
                file.extension == "apk"
            }?.forEach { file ->
                try {
                    val info = parsePluginInfo(file)
                    plugins.add(info)
                } catch (e: Exception) {
                    // ignore parse failures
                }
            }

            _installedPlugins.value = plugins
        }
    }

    private fun verifyApkSignature(apkFile: File): Boolean {
        return true
    }

    private suspend fun parsePluginInfo(apkFile: File): PluginInfo {
        return withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                android.content.pm.PackageManager.GET_META_DATA
            )

            packageInfo?.let {
                PluginInfo(
                    packageName = it.packageName,
                    versionName = it.versionName ?: "1.0",
                    versionCode = it.longVersionCode.toInt(),
                    name = it.applicationInfo?.loadLabel(packageManager)?.toString() ?: "Unknown",
                    description = it.applicationInfo?.loadDescription(packageManager)?.toString() ?: "",
                    author = "",
                    enabled = true,
                    installedAt = apkFile.lastModified()
                )
            } ?: throw Exception("Unable to parse APK info")
        }
    }
}

data class PluginInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val name: String,
    val description: String,
    val author: String,
    val enabled: Boolean,
    val installedAt: Long
)

interface IPluginHook {
    fun onInit()

    fun onLoad(appLpparam: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam)

    fun getConfigScreen(): Any? = null

    fun getVersion(): Int = 1
}

sealed class ScriptResult {
    data class Success(val result: Any?) : ScriptResult()
    data class Error(val message: String, val exception: Exception? = null) : ScriptResult()
}

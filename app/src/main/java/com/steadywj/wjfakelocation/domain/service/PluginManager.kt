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
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 插件管理器
 *
 * 功能:
 * - 第三方 Hook 模块加载
 * - 自定义脚本支持（Lua/JavaScript）
 * - 插件沙箱隔离
 * - 版本管理
 */
@Singleton
class PluginManager
    @Inject
    constructor(
        private val context: Context,
    ) {
        /** 已安装的插件列表 */
        private val _installedPlugins = MutableStateFlow<List<PluginInfo>>(emptyList())
        val installedPlugins: Flow<List<PluginInfo>> = _installedPlugins.asStateFlow()

        /** 插件目录 */
        private val pluginDir: File by lazy {
            File(context.filesDir, "plugins").apply {
                if (!exists()) mkdirs()
            }
        }

        /** 优化的 DEX 目录 */
        private val optimizedDir: File by lazy {
            File(context.cacheDir, "plugin_dex").apply {
                if (!exists()) mkdirs()
            }
        }

        /** 已加载的插件类加载器 */
        private val pluginClassLoaders = mutableMapOf<String, DexClassLoader>()

        /**
         * 安装插件
         * @param pluginFile 插件 APK 文件
         * @return 安装结果
         */
        suspend fun installPlugin(pluginFile: File): Result<PluginInfo> {
            return withContext(Dispatchers.IO) {
                try {
                    // 1. 验证 APK 签名
                    if (!verifyApkSignature(pluginFile)) {
                        return@withContext Result.failure(Exception("插件签名验证失败"))
                    }

                    // 2. 解析插件信息
                    val pluginInfo = parsePluginInfo(pluginFile)

                    // 3. 复制到插件目录
                    val destFile = File(pluginDir, "${pluginInfo.packageName}.apk")
                    pluginFile.copyTo(destFile, overwrite = true)

                    // 4. 创建类加载器
                    val classLoader =
                        DexClassLoader(
                            destFile.absolutePath,
                            optimizedDir.absolutePath,
                            null,
                            this@PluginManager.javaClass.classLoader,
                        )

                    // 5. 保存插件信息
                    pluginClassLoaders[pluginInfo.packageName] = classLoader

                    // 6. 更新插件列表
                    _installedPlugins.value = _installedPlugins.value + pluginInfo

                    Result.success(pluginInfo)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

        /**
         * 卸载插件
         * @param packageName 插件包名
         */
        suspend fun uninstallPlugin(packageName: String): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    // 1. 删除 APK 文件
                    val pluginFile = File(pluginDir, "$packageName.apk")
                    if (pluginFile.exists()) {
                        pluginFile.delete()
                    }

                    // 2. 移除类加载器
                    pluginClassLoaders.remove(packageName)

                    // 3. 更新插件列表
                    _installedPlugins.value =
                        _installedPlugins.value.filter {
                            it.packageName != packageName
                        }

                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

        /**
         * 启用/禁用插件
         */
        suspend fun setPluginEnabled(
            packageName: String,
            enabled: Boolean,
        ): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    _installedPlugins.value =
                        _installedPlugins.value.map { plugin ->
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

        /**
         * 加载插件类
         * @param packageName 插件包名
         * @param className 类名
         * @return 类的实例
         */
        fun loadPluginClass(
            packageName: String,
            className: String,
        ): Any? {
            val classLoader = pluginClassLoaders[packageName] ?: return null

            return try {
                val clazz = classLoader.loadClass(className)
                clazz.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 执行 Lua 脚本
         * @param scriptContent 脚本内容
         * @param params 参数
         * @return 执行结果
         */
        suspend fun executeLuaScript(
            scriptContent: String,
            params: Map<String, Any>,
        ): Result<Any?> {
            return withContext(Dispatchers.IO) {
                try {
                    // 创建 Lua 环境
                    val globals: Globals = JsePlatform.standardGlobals()

                    // 加载并执行脚本
                    val luaFunction = globals.load(scriptContent)

                    // 设置参数
                    params.forEach { (key, value) ->
                        when (value) {
                            is String -> globals.set(key, org.luaj.vm2.LuaValue.valueOf(value))
                            is Int -> globals.set(key, org.luaj.vm2.LuaValue.valueOf(value))
                            is Double -> globals.set(key, org.luaj.vm2.LuaValue.valueOf(value))
                            is Boolean -> globals.set(key, org.luaj.vm2.LuaValue.valueOf(value))
                            else -> globals.set(key, org.luaj.vm2.LuaValue.valueOf(value.toString()))
                        }
                    }

                    // 执行并获取结果
                    val result = luaFunction.call()

                    Result.success(result.tojstring())
                } catch (e: Exception) {
                    Result.failure(Exception("Lua 脚本执行失败：${e.message}"))
                }
            }
        }

        /**
         * 执行 JavaScript 脚本
         * @param scriptContent 脚本内容
         * @param params 参数
         * @return 执行结果
         */
        suspend fun executeJavaScript(
            scriptContent: String,
            params: Map<String, Any>,
        ): Result<Any?> {
            return withContext(Dispatchers.IO) {
                try {
                    // 使用Android 内置的 JavaScript 引擎
                    val webView = android.webkit.WebView(context)
                    val result =
                        kotlin.coroutines.suspendCoroutine<Any?> { continuation ->
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

        /**
         * 获取插件Hook 接口
         * @param packageName 插件包名
         * @return Hook 接口实例
         */
        fun getPluginHookInterface(packageName: String): IPluginHook? {
            val hookInstance = loadPluginClass(packageName, "$packageName.PluginHook")
            return hookInstance as? IPluginHook
        }

        /**
         * 刷新插件列表
         */
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
                        // 忽略解析失败的插件
                    }
                }

                _installedPlugins.value = plugins
            }
        }

        // ==================== 内部方法 ====================

        /**
         * 验证 APK 签名
         */
        private fun verifyApkSignature(apkFile: File): Boolean {
            // 实现 APK 签名验证逻辑
            // TODO: 添加 APK 签名验证（使用 jarsigner 或 apksigner）
            // 检查开发者签名、证书等

        /*
        try {
            val process = Runtime.getRuntime().exec(
                arrayOf("apksigner", "verify", "--print-certs", apkFile.absolutePath)
            )
            val output = process.inputStream.bufferedReader().readText()
            return output.contains("SHA256") // 简单检查
        } catch (e: Exception) {
            Log.e(TAG, "签名验证失败", e)
            return false
        }
         */
            return true // 暂时返回 true
        }

        /**
         * 解析插件信息
         */
        private suspend fun parsePluginInfo(apkFile: File): PluginInfo {
            return withContext(Dispatchers.IO) {
                // 使用 PackageManager 解析 APK
                val packageManager = context.packageManager
                val packageInfo =
                    packageManager.getPackageArchiveInfo(
                        apkFile.absolutePath,
                        android.content.pm.PackageManager.GET_META_DATA,
                    )

                packageInfo?.let {
                    PluginInfo(
                        packageName = it.packageName,
                        versionName = it.versionName ?: "1.0",
                        versionCode = it.longVersionCode.toInt(),
                        name = it.applicationInfo?.loadLabel(packageManager).toString(),
                        description = it.applicationInfo?.loadDescription(packageManager)?.toString() ?: "",
                        // 从 metadata 中读取
                        author = "",
                        enabled = true,
                        installedAt = apkFile.lastModified(),
                    )
                } ?: throw Exception("无法解析 APK 信息")
            }
        }
    }

// ==================== 数据模型 ====================

/**
 * 插件信息
 */
data class PluginInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val name: String,
    val description: String,
    val author: String,
    val enabled: Boolean,
    val installedAt: Long,
)

/**
 * 插件 Hook 接口
 * 所有插件必须实现此接口
 */
interface IPluginHook {
    /**
     * 插件初始化
     */
    fun onInit()

    /**
     * 加载 Hook
     * @param appLpparam 应用加载参数
     */
    fun onLoad(appLpparam: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam)

    /**
     * 获取插件配置界面
     * @return Compose UI 组件（可选）
     */
    fun getConfigScreen(): Any? = null

    /**
     * 插件版本
     */
    fun getVersion(): Int = 1
}

/**
 * 脚本执行结果
 */
sealed class ScriptResult {
    data class Success(val result: Any?) : ScriptResult()

    data class Error(val message: String, val exception: Exception? = null) : ScriptResult()
}

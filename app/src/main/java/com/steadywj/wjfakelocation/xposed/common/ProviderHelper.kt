package com.steadywj.wjfakelocation.xposed.common

import android.content.Context
import android.net.Uri
import de.robv.android.xposed.XposedBridge

/**
 * 跨进程获取主应用设置的工具类
 */
object ProviderHelper {
    private const val AUTHORITY = "com.steadywj.wjfakelocation.provider"
    private val URI: Uri = Uri.parse("content://$AUTHORITY/settings")

    // 缓存设置，避免频繁查询 ContentProvider 造成性能问题
    private var lastFetchTime = 0L
    private const val CACHE_DURATION_MS = 2000L // 2秒缓存过期时间

    private val cache = mutableMapOf<String, Any>()

    private fun ensureCache(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastFetchTime < CACHE_DURATION_MS && cache.isNotEmpty()) {
            return
        }

        try {
            val cursor = context.contentResolver.query(URI, null, null, null, null)
            cursor?.use {
                cache.clear()
                val keyIndex = it.getColumnIndex("key")
                val typeIndex = it.getColumnIndex("type")
                val valueIndex = it.getColumnIndex("value")

                if (keyIndex != -1 && typeIndex != -1 && valueIndex != -1) {
                    while (it.moveToNext()) {
                        val key = it.getString(keyIndex)
                        val type = it.getString(typeIndex)
                        val valueStr = it.getString(valueIndex)

                        when (type) {
                            "boolean" -> cache[key] = valueStr.toBoolean()
                            "int" -> cache[key] = valueStr.toIntOrNull() ?: 0
                            "long" -> cache[key] = valueStr.toLongOrNull() ?: 0L
                            "float" -> cache[key] = valueStr.toFloatOrNull() ?: 0f
                            "string" -> cache[key] = valueStr
                            "string_set" -> cache[key] = valueStr.split(",").filter { it.isNotEmpty() }.toSet()
                        }
                    }
                }
            }
            lastFetchTime = System.currentTimeMillis()
        } catch (e: Exception) {
            XposedBridge.log("ProviderHelper query error: ${e.message}")
        }
    }

    fun isPlaying(context: Context): Boolean {
        ensureCache(context)
        return cache["is_playing"] as? Boolean ?: false
    }

    fun getLatitude(context: Context): Double {
        ensureCache(context)
        val latLong = cache["selected_latitude"] as? Long ?: java.lang.Double.doubleToRawLongBits(39.9042)
        return java.lang.Double.longBitsToDouble(latLong)
    }

    fun getLongitude(context: Context): Double {
        ensureCache(context)
        val lngLong = cache["selected_longitude"] as? Long ?: java.lang.Double.doubleToRawLongBits(116.4074)
        return java.lang.Double.longBitsToDouble(lngLong)
    }

    fun useAccuracy(context: Context): Boolean {
        ensureCache(context)
        return cache["use_accuracy"] as? Boolean ?: false
    }

    fun getAccuracy(context: Context): Float {
        ensureCache(context)
        return cache["accuracy"] as? Float ?: 0.0f
    }

    fun useAltitude(context: Context): Boolean {
        ensureCache(context)
        return cache["use_altitude"] as? Boolean ?: false
    }

    fun getAltitude(context: Context): Double {
        ensureCache(context)
        val altFloat = cache["altitude"] as? Float ?: 0.0f
        return altFloat.toDouble()
    }

    fun useRandomize(context: Context): Boolean {
        ensureCache(context)
        return cache["use_randomize"] as? Boolean ?: false
    }

    fun getRandomizeRadius(context: Context): Double {
        ensureCache(context)
        val radiusFloat = cache["randomize_radius"] as? Float ?: 0.0f
        return radiusFloat.toDouble()
    }
    
    fun getTargetMode(context: Context): String {
        ensureCache(context)
        return cache["target_mode"] as? String ?: "GLOBAL"
    }
    
    fun getTargetPackages(context: Context): Set<String> {
        ensureCache(context)
        return cache["target_packages"] as? Set<String> ?: emptySet()
    }
    
    // 是否应该对当前包名生效
    fun shouldFakePackage(context: Context, packageName: String): Boolean {
        if (!isPlaying(context)) return false
        
        val mode = getTargetMode(context)
        val packages = getTargetPackages(context)
        
        return when (mode) {
            "GLOBAL" -> true
            "WHITELIST" -> packages.contains(packageName)
            "BLACKLIST" -> !packages.contains(packageName)
            else -> true
        }
    }
}

package com.steadywj.wjfakelocation.xposed.common

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import de.robv.android.xposed.XposedBridge

/**
 * 跨进程获取主应用设置的工具类
 */
@Suppress("TooManyFunctions")
object ProviderHelper {
    private const val AUTHORITY = "com.steadywj.wjfakelocation.provider"
    private val URI: Uri = Uri.parse("content://$AUTHORITY/settings")

    // 默认坐标常量，避免魔法数
    private const val DEFAULT_LATITUDE = 39.9042
    private const val DEFAULT_LONGITUDE = 116.4074

    // 缓存设置，避免频繁查询 ContentProvider 造成性能问题
    private var lastFetchTime = 0L
    private const val CACHE_DURATION_MS = 2000L // 2秒缓存过期时间

    private val cache = mutableMapOf<String, Any>()

    private val KEYS_TO_QUERY =
        arrayOf(
            "is_playing",
            "selected_latitude",
            "selected_longitude",
            "use_accuracy",
            "accuracy",
            "use_altitude",
            "altitude",
            "use_randomize",
            "randomize_radius",
            "target_mode",
            "target_packages"
        )

    private var isObserverRegistered = false

    @Suppress("TooGenericExceptionCaught")
    private fun registerObserver(context: Context) {
        if (isObserverRegistered) return
        try {
            context.contentResolver.registerContentObserver(
                URI,
                true,
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        synchronized(cache) {
                            cache.clear()
                            lastFetchTime = 0L
                        }
                    }
                }
            )
            isObserverRegistered = true
        } catch (e: Exception) {
            XposedBridge.log("ProviderHelper registerObserver error: ${e.message}")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun ensureCache(context: Context) {
        registerObserver(context)
        val now = System.currentTimeMillis()
        synchronized(cache) {
            if (now - lastFetchTime < CACHE_DURATION_MS && cache.isNotEmpty()) {
                return
            }
        }

        try {
            readSettings(context)
            lastFetchTime = System.currentTimeMillis()
        } catch (e: Exception) {
            XposedBridge.log("ProviderHelper query error: ${e.message}")
        }
    }

    private fun readSettings(context: Context) {
        val cursor =
            context.contentResolver.query(
                URI,
                null,
                "key IN (?)",
                KEYS_TO_QUERY,
                null
            )
        cursor?.use {
            synchronized(cache) {
                cache.clear()
                val keyIndex = it.getColumnIndex("key")
                val typeIndex = it.getColumnIndex("type")
                val valueIndex = it.getColumnIndex("value")

                if (keyIndex != -1 && typeIndex != -1 && valueIndex != -1) {
                    while (it.moveToNext()) {
                        val key = it.getString(keyIndex)
                        val type = it.getString(typeIndex)
                        val valueStr = it.getString(valueIndex)
                        parseAndCache(key, type, valueStr)
                    }
                }
            }
        }
    }

    private fun parseAndCache(
        key: String,
        type: String,
        valueStr: String
    ) {
        when (type) {
            "boolean" -> cache[key] = valueStr.toBoolean()
            "int" -> cache[key] = valueStr.toIntOrNull() ?: 0
            "long" -> cache[key] = valueStr.toLongOrNull() ?: 0L
            "float" -> cache[key] = valueStr.toFloatOrNull() ?: 0f
            "string" -> cache[key] = valueStr
            "string_set" -> cache[key] = valueStr.split(",").filter { it.isNotEmpty() }.toSet()
        }
    }

    fun isPlaying(context: Context): Boolean {
        ensureCache(context)
        synchronized(cache) {
            return cache["is_playing"] as? Boolean ?: false
        }
    }

    fun getLatitude(context: Context): Double {
        ensureCache(context)
        val raw =
            synchronized(cache) {
                cache["selected_latitude"] as? Long
            } ?: return DEFAULT_LATITUDE
        val d = java.lang.Double.longBitsToDouble(raw)
        return if (d == Double.MIN_VALUE || d.isNaN() || d == 0.0) DEFAULT_LATITUDE else d
    }

    fun getLongitude(context: Context): Double {
        ensureCache(context)
        val raw =
            synchronized(cache) {
                cache["selected_longitude"] as? Long
            } ?: return DEFAULT_LONGITUDE
        val d = java.lang.Double.longBitsToDouble(raw)
        return if (d == Double.MIN_VALUE || d.isNaN() || d == 0.0) DEFAULT_LONGITUDE else d
    }

    fun useAccuracy(context: Context): Boolean {
        ensureCache(context)
        synchronized(cache) {
            return cache["use_accuracy"] as? Boolean ?: false
        }
    }

    fun getAccuracy(context: Context): Float {
        ensureCache(context)
        synchronized(cache) {
            return cache["accuracy"] as? Float ?: 0.0f
        }
    }

    fun useAltitude(context: Context): Boolean {
        ensureCache(context)
        synchronized(cache) {
            return cache["use_altitude"] as? Boolean ?: false
        }
    }

    fun getAltitude(context: Context): Double {
        ensureCache(context)
        val altFloat =
            synchronized(cache) {
                cache["altitude"] as? Float ?: 0.0f
            }
        return altFloat.toDouble()
    }

    fun useRandomize(context: Context): Boolean {
        ensureCache(context)
        synchronized(cache) {
            return cache["use_randomize"] as? Boolean ?: false
        }
    }

    fun getRandomizeRadius(context: Context): Double {
        ensureCache(context)
        val radiusFloat =
            synchronized(cache) {
                cache["randomize_radius"] as? Float ?: 0.0f
            }
        return radiusFloat.toDouble()
    }

    fun getTargetMode(context: Context): String {
        ensureCache(context)
        synchronized(cache) {
            return cache["target_mode"] as? String ?: "GLOBAL"
        }
    }

    fun getTargetPackages(context: Context): Set<String> {
        ensureCache(context)
        synchronized(cache) {
            return cache["target_packages"] as? Set<String> ?: emptySet()
        }
    }

    // 是否应该对当前包名生效
    fun shouldFakePackage(
        context: Context,
        packageName: String
    ): Boolean {
        if (!isPlaying(context)) return false

        val mode = getTargetMode(context)
        val packages = getTargetPackages(context)

        return when (mode) {
            "GLOBAL" -> true
            "APP_SPECIFIC" -> packages.contains(packageName)
            "WHITELIST" -> packages.contains(packageName)
            "BLACKLIST" -> !packages.contains(packageName)
            else -> true
        }
    }
}

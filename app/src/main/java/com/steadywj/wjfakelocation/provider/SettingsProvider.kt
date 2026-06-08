package com.steadywj.wjfakelocation.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class SettingsProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.steadywj.wjfakelocation.provider"
        val URI: Uri = Uri.parse("content://$AUTHORITY/settings")

        // 白名单：仅允许通过 ContentProvider 读取这 11 个不包含用户敏感地址与配置的 key，防止隐私泄露
        private val SAFE_KEYS =
            setOf(
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
                "target_packages",
                "use_dingtalk_location_hook",
                "use_dingtalk_anti_detect",
                "use_dingtalk_update_hook"
            )
    }

    private lateinit var prefs: SharedPreferences

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            context?.contentResolver?.notifyChange(URI, null)
        }

    override fun onCreate(): Boolean {
        context?.let {
            prefs = it.getSharedPreferences("wjfakelocation_prefs", Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(prefListener)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(arrayOf("key", "type", "value"))

        // 如果指定了选择器，则仅查询交集中的 key；否则返回所有安全 key
        val keysToQuery =
            if (selection == "key IN (?)" && selectionArgs != null) {
                selectionArgs.filter { it in SAFE_KEYS }
            } else {
                SAFE_KEYS.toList()
            }

        for (key in keysToQuery) {
            val value = prefs.all[key] ?: continue

            val type =
                when (value) {
                    is Boolean -> "boolean"
                    is Int -> "int"
                    is Long -> "long"
                    is Float -> "float"
                    is String -> "string"
                    is Set<*> -> "string_set"
                    else -> "string"
                }

            val stringValue =
                if (value is Set<*>) {
                    value.joinToString(",")
                } else {
                    value.toString()
                }

            cursor.addRow(arrayOf(key, type, stringValue))
        }

        return cursor
    }

    override fun getType(uri: Uri): String? {
        return "vnd.android.cursor.dir/vnd.com.steadywj.wjfakelocation.settings"
    }

    override fun insert(
        uri: Uri,
        values: ContentValues?
    ): Uri? {
        throw UnsupportedOperationException("Not supported")
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("Not supported")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("Not supported")
    }
}

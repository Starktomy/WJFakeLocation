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
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(): Boolean {
        context?.let {
            prefs = it.getSharedPreferences("wjfakelocation_prefs", Context.MODE_PRIVATE)
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
        
        for ((key, value) in prefs.all) {
            if (value == null) continue
            
            val type = when (value) {
                is Boolean -> "boolean"
                is Int -> "int"
                is Long -> "long"
                is Float -> "float"
                is String -> "string"
                is Set<*> -> "string_set"
                else -> "string"
            }
            
            val stringValue = if (value is Set<*>) {
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

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
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

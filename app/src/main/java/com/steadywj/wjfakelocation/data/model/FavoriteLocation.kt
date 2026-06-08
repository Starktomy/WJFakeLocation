// FavoriteLocation.kt
package com.steadywj.wjfakelocation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_locations")
data class FavoriteLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val category: String = "default",
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

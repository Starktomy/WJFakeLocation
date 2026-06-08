// AppDatabase.kt
package com.steadywj.wjfakelocation.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.steadywj.wjfakelocation.data.model.FavoriteLocation

@Database(
    entities = [FavoriteLocation::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteLocationDao(): FavoriteLocationDao
}

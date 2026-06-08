// FavoriteLocationDao.kt
package com.steadywj.wjfakelocation.data.local

import androidx.room.*
import com.steadywj.wjfakelocation.data.model.FavoriteLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteLocationDao {
    @Query("SELECT * FROM favorite_locations ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteLocation>>

    @Query("SELECT * FROM favorite_locations ORDER BY updatedAt DESC")
    suspend fun getAllFavoritesSync(): List<FavoriteLocation>

    @Query("SELECT * FROM favorite_locations WHERE id = :id")
    suspend fun getFavoriteById(id: Long): FavoriteLocation?

    @Query("SELECT * FROM favorite_locations WHERE category = :category ORDER BY name ASC")
    fun getFavoritesByCategory(category: String): Flow<List<FavoriteLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(location: FavoriteLocation): Long

    @Update
    suspend fun updateFavorite(location: FavoriteLocation)

    @Delete
    suspend fun deleteFavorite(location: FavoriteLocation)

    @Query("DELETE FROM favorite_locations WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)

    @Query("SELECT DISTINCT category FROM favorite_locations")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM favorite_locations WHERE name LIKE :query OR address LIKE :query ORDER BY createdAt DESC")
    fun searchFavorites(query: String): Flow<List<FavoriteLocation>>
}

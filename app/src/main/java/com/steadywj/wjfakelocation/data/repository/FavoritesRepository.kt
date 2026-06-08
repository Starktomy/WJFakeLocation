// FavoritesRepository.kt
package com.steadywj.wjfakelocation.data.repository

import com.steadywj.wjfakelocation.data.local.FavoriteLocationDao
import com.steadywj.wjfakelocation.data.model.FavoriteLocation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository
    @Inject
    constructor(
        private val favoriteLocationDao: FavoriteLocationDao,
    ) {
        val allFavorites: Flow<List<FavoriteLocation>> = favoriteLocationDao.getAllFavorites()

        suspend fun getAllFavoritesSync(): List<FavoriteLocation> = favoriteLocationDao.getAllFavoritesSync()

        fun getFavoritesByCategory(category: String): Flow<List<FavoriteLocation>> {
            return favoriteLocationDao.getFavoritesByCategory(category)
        }

        fun searchFavorites(query: String): Flow<List<FavoriteLocation>> {
            return favoriteLocationDao.searchFavorites("%$query%")
        }

        suspend fun getFavoriteById(id: Long): FavoriteLocation? {
            return favoriteLocationDao.getFavoriteById(id)
        }

        suspend fun insertFavorite(location: FavoriteLocation): Long {
            return favoriteLocationDao.insertFavorite(location)
        }

        suspend fun updateFavorite(location: FavoriteLocation) {
            favoriteLocationDao.updateFavorite(location)
        }

        suspend fun deleteFavorite(location: FavoriteLocation) {
            favoriteLocationDao.deleteFavorite(location)
        }

        suspend fun deleteFavoriteById(id: Long) {
            favoriteLocationDao.deleteFavoriteById(id)
        }

        val allCategories: Flow<List<String>> = favoriteLocationDao.getAllCategories()
    }

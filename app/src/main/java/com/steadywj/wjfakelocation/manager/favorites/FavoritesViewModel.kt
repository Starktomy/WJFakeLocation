// FavoritesViewModel.kt
package com.steadywj.wjfakelocation.manager.favorites.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steadywj.wjfakelocation.data.model.FavoriteLocation
import com.steadywj.wjfakelocation.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 收藏?ViewModel
 * 管理收藏夹界面的状态和业务逻辑
 */
@HiltViewModel
class FavoritesViewModel
    @Inject
    constructor(
        private val favoritesRepository: FavoritesRepository,
    ) : ViewModel() {
        /** 所有收藏项（响应式数据流） */
        val allFavorites: StateFlow<List<FavoriteLocation>> =
            favoritesRepository.allFavorites
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        /** UI 状?*/
        private val _uiState = MutableStateFlow(FavoritesUiState())
        val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

        /**
         * 搜索收藏?
         * @param query 搜索关键?
         */
        fun searchFavorites(query: String) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                if (query.isBlank()) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            searchQuery = "",
                        )
                } else {
                    favoritesRepository.searchFavorites(query).collect { favorites ->
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                searchQuery = query,
                            )
                    }
                }
            }
        }

        /**
         * 添加收藏?
         * @param name 名称
         * @param latitude 纬度
         * @param longitude 经度
         * @param address 地址
         * @param category 分类
         */
        fun addFavorite(
            name: String,
            latitude: Double,
            longitude: Double,
            address: String?,
            category: String,
        ) {
            viewModelScope.launch {
                val favorite =
                    FavoriteLocation(
                        name = name,
                        latitude = latitude,
                        longitude = longitude,
                        address = address,
                        category = category,
                    )
                favoritesRepository.insertFavorite(favorite)
                _uiState.value = _uiState.value.copy(showSuccessMessage = "已添加到收藏")
            }
        }

        /**
         * 更新收藏?
         * @param favorite 收藏?
         */
        fun updateFavorite(favorite: FavoriteLocation) {
            viewModelScope.launch {
                val updated = favorite.copy(updatedAt = System.currentTimeMillis())
                favoritesRepository.updateFavorite(updated)
                _uiState.value = _uiState.value.copy(showSuccessMessage = "已更新收藏")
            }
        }

        /**
         * 删除收藏?
         * @param favorite 收藏?
         */
        fun deleteFavorite(favorite: FavoriteLocation) {
            viewModelScope.launch {
                favoritesRepository.deleteFavorite(favorite)
                _uiState.value = _uiState.value.copy(showSuccessMessage = "已删除收藏")
            }
        }

        /**
         * 清除消息提示
         */
        fun clearMessage() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(showSuccessMessage = null)
            }
        }
    }

/**
 * 收藏?UI 状?
 * @property isLoading 加载状?
 * @property searchQuery 搜索关键?
 * @property showSuccessMessage 成功消息
 * @property showEditDialog 显示编辑对话?
 * @property editingFavorite 正在编辑的收藏项
 */
data class FavoritesUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val showSuccessMessage: String? = null,
    val showEditDialog: Boolean = false,
    val editingFavorite: FavoriteLocation? = null,
)

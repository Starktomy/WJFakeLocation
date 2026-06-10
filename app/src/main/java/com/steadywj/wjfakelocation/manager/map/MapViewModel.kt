// MapViewModel.kt
package com.steadywj.wjfakelocation.manager.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steadywj.wjfakelocation.data.model.SelectedLocation
import com.steadywj.wjfakelocation.data.repository.FavoritesRepository
import com.steadywj.wjfakelocation.data.repository.PreferencesRepository
import com.steadywj.wjfakelocation.manager.map.utils.AMapManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 地图 ViewModel
 * 管理地图界面的状态和业务逻辑
 */
@HiltViewModel
class MapViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
        private val favoritesRepository: FavoritesRepository,
        private val aMapManager: AMapManager,
    ) : ViewModel() {
        /** UI 状?*/
        private val _uiState = MutableStateFlow(MapUiState())
        val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

        /** 选中的位?*/
        private val _selectedLocation = MutableStateFlow<SelectedLocation?>(null)
        val selectedLocation: StateFlow<SelectedLocation?> = _selectedLocation.asStateFlow()

        /** 是否正在运行（伪造中?*/
        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        /**
         * 选择位置
         * @param latitude 纬度
         * @param longitude 经度
         * @param address 地址（可选）
         */
        fun selectLocation(
            latitude: Double,
            longitude: Double,
            address: String? = null,
        ) {
            viewModelScope.launch {
                val location = SelectedLocation(latitude, longitude, address)
                _selectedLocation.value = location
                preferencesRepository.updateSelectedLocation(location)
            }
        }

        /**
         * 启动虚拟定位
         */
        fun startFakeLocation() {
            viewModelScope.launch {
                preferencesRepository.updateIsPlaying(true)
                _isPlaying.value = true
            }
        }

        /**
         * 停止虚拟定位
         */
        fun stopFakeLocation() {
            viewModelScope.launch {
                preferencesRepository.updateIsPlaying(false)
                _isPlaying.value = false
            }
        }

        /**
         * 切换虚拟定位状?
         */
        fun toggleFakeLocation() {
            if (_isPlaying.value) {
                stopFakeLocation()
            } else {
                startFakeLocation()
            }
        }

        /**
         * 搜索位置（高德地图地理编码）
         * @param query 搜索关键?
         */
        fun searchLocation(query: String) {
            if (query.isBlank()) {
                clearSearch()
                return
            }

            viewModelScope.launch {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = true,
                        searchQuery = query,
                        errorMessage = null,
                    )

                try {
                    aMapManager.geocodeAddress(query).collect { result ->
                        result.onSuccess { latLng ->
                            selectLocation(latLng.latitude, latLng.longitude, latLng.address)
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }.onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "搜索失败"
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "搜索失败",
                        )
                }
            }
        }

        /**
         * 清除搜索状?
         */
        fun clearSearch() {
            viewModelScope.launch {
                _uiState.value =
                    _uiState.value.copy(
                        searchQuery = "",
                        isLoading = false,
                        errorMessage = null,
                    )
            }
        }
    }

/**
 * 地图 UI 状?
 * @property isLoading 加载状?
 * @property searchQuery 搜索关键?
 * @property errorMessage 错误消息
 * @property showAddFavoriteDialog 显示添加收藏对话?
 */
data class MapUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val showAddFavoriteDialog: Boolean = false,
)

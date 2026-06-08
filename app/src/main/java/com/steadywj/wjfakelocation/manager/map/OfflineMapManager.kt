// OfflineMapManager.kt
package com.steadywj.wjfakelocation.manager.map.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线地图管理?
 *
 * 功能:
 * - 瓦片地图下载
 * - 磁盘缓存管理
 * - 下载进度追踪
 * - WiFi 环境检?
 */
@Singleton
class OfflineMapManager
    @Inject
    constructor(
        private val context: Context,
        private val mapCacheManager: MapCacheManager,
    ) {
        /** 下载状?*/
        private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.IDLE)
        val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

        /** 下载进度 */
        private val _progress = MutableStateFlow(0f)
        val progress: StateFlow<Float> = _progress.asStateFlow()

        /** 离线地图目录 */
        private val offlineDir: File by lazy {
            File(context.filesDir, "offline_maps").apply {
                if (!exists()) mkdirs()
            }
        }

        /** 最大存储空间（500MB?*/
        private val MAX_STORAGE_BYTES = 500L * 1024 * 1024

        /**
         * 下载指定区域的地图瓦?
         * @param centerLat 中心纬度
         * @param centerLng 中心经度
         * @param zoom 缩放级别?-18?
         * @param radiusKm 半径（公里），默?10km
         */
        suspend fun downloadArea(
            centerLat: Double,
            centerLng: Double,
            zoom: Int = 15,
            radiusKm: Double = 10.0,
        ): Result<Int> {
            return withContext(Dispatchers.IO) {
                try {
                    // 检查网络连?
                    if (!isWifiConnected() && !isNetworkAvailable()) {
                        _downloadState.value = DownloadState.ERROR("无网络连接")
                        return@withContext Result.failure(Exception("无网络连接"))
                    }

                    _downloadState.value = DownloadState.DOWNLOADING
                    _progress.value = 0f

                    // 计算需要下载的瓦片范围
                    val tileRange = calculateTileRange(centerLat, centerLng, zoom, radiusKm)

                    val totalTiles =
                        (tileRange.endX - tileRange.startX + 1) *
                            (tileRange.endY - tileRange.startY + 1)

                    var downloadedCount = 0

                    // 批量下载瓦片
                    for (x in tileRange.startX..tileRange.endX) {
                        for (y in tileRange.startY..tileRange.endY) {
                            if (_downloadState.value == DownloadState.CANCELLED) {
                                return@withContext Result.failure(Exception("下载已取消"))
                            }

                            try {
                                // 检查是否已存在
                                val cachedTile = mapCacheManager.getTileFromCache(zoom, x, y)
                                if (cachedTile != null) {
                                    downloadedCount++
                                    continue
                                }

                                // 下载瓦片
                                downloadTile(zoom, x, y)
                                downloadedCount++

                                // 更新进度
                                _progress.value = downloadedCount.toFloat() / totalTiles
                            } catch (e: Exception) {
                                // 单个瓦片下载失败，继续下一?
                                e.printStackTrace()
                            }
                        }
                    }

                    _downloadState.value = DownloadState.COMPLETED
                    _progress.value = 1f

                    Result.success(downloadedCount)
                } catch (e: Exception) {
                    _downloadState.value = DownloadState.ERROR(e.message ?: "下载失败")
                    Result.failure(e)
                }
            }
        }

        /**
         * 取消下载
         */
        fun cancelDownload() {
            _downloadState.value = DownloadState.CANCELLED
        }

        /**
         * 检查是否为 WiFi 环境
         */
        fun isWifiConnected(): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val networkInfo = connectivityManager?.activeNetworkInfo
            return networkInfo?.type == android.net.ConnectivityManager.TYPE_WIFI
        }

        /**
         * 检查网络是否可?
         */
        private fun isNetworkAvailable(): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val networkInfo = connectivityManager?.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }

        /**
         * 获取已下载的地图区域列表
         */
        fun getDownloadedAreas(): List<OfflineArea> {
            val areas = mutableListOf<OfflineArea>()

            offlineDir.listFiles()?.forEach { zoomDir ->
                if (zoomDir.isDirectory) {
                    val zoom = zoomDir.name.toIntOrNull() ?: return@forEach

                    zoomDir.listFiles()?.forEach { xDir ->
                        if (xDir.isDirectory) {
                            val x = xDir.name.toIntOrNull() ?: return@forEach

                            xDir.listFiles { file ->
                                file.extension == "png"
                            }?.forEach { file ->
                                val y = file.nameWithoutExtension.toLongOrNull() ?: return@forEach

                                areas.add(
                                    OfflineArea(
                                        zoom = zoom,
                                        x = x,
                                        y = y.toInt(),
                                        size = file.length(),
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            return areas
        }

        /**
         * 清除离线地图
         */
        suspend fun clearOfflineMaps(): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    offlineDir.deleteRecursively()
                    offlineDir.mkdirs()
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

        /**
         * 获取离线地图占用空间
         */
        fun getStorageUsed(): Long {
            return getFileSize(offlineDir)
        }

        /**
         * 获取可用空间
         */
        fun getStorageAvailable(): Long {
            return MAX_STORAGE_BYTES - getStorageUsed()
        }

        // ==================== 内部方法 ====================

        /**
         * 计算瓦片范围
         */
        private fun calculateTileRange(
            lat: Double,
            lng: Double,
            zoom: Int,
            radiusKm: Double,
        ): TileRange {
            // 将经纬度转换为瓦片坐?
            val centerTileX = lonToTileX(lng, zoom)
            val centerTileY = latToTileY(lat, zoom)

            // 根据半径计算需要扩展的瓦片?
            // ?zoom 15 级别，每个瓦片约覆盖 1km²
            val tileRadius = (radiusKm / 1.0).toInt().coerceAtLeast(1)

            return TileRange(
                startX = (centerTileX - tileRadius).coerceIn(0, (1 shl zoom) - 1),
                endX = (centerTileX + tileRadius).coerceIn(0, (1 shl zoom) - 1),
                startY = (centerTileY - tileRadius).coerceIn(0, (1 shl zoom) - 1),
                endY = (centerTileY + tileRadius).coerceIn(0, (1 shl zoom) - 1),
            )
        }

        /**
         * 下载单个瓦片
         */
        private suspend fun downloadTile(
            zoom: Int,
            x: Int,
            y: Int,
        ) {
            // 检查存储空?
            if (getStorageUsed() >= MAX_STORAGE_BYTES) {
                throw Exception("存储空间不足")
            }

            // 高德地图瓦片 URL
            val url = "https://webrd0${(x + y) % 4 + 1}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=$x&y=$y&z=$zoom"

            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val inputStream = connection.inputStream
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

                // 保存到缓?
                mapCacheManager.saveTileToCache(zoom, x, y, bitmap)
            } finally {
                connection.disconnect()
            }
        }

        /**
         * 经度转瓦?X 坐标
         */
        private fun lonToTileX(
            lon: Double,
            zoom: Int,
        ): Int {
            return ((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
        }

        /**
         * 纬度转瓦?Y 坐标
         */
        private fun latToTileY(
            lat: Double,
            zoom: Int,
        ): Int {
            val latRad = Math.toRadians(lat)
            val n = (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2
            return (n * (1 shl zoom)).toInt()
        }

        /**
         * 递归计算文件夹大?
         */
        private fun getFileSize(file: File): Long {
            if (!file.exists()) return 0

            if (file.isFile) return file.length()

            var size = 0L
            file.listFiles()?.forEach {
                size += getFileSize(it)
            }

            return size
        }
    }

// ==================== 数据模型 ====================

/**
 * 下载状?
 */
sealed class DownloadState {
    object IDLE : DownloadState()

    object DOWNLOADING : DownloadState()

    object COMPLETED : DownloadState()

    data class ERROR(val message: String) : DownloadState()

    object CANCELLED : DownloadState()
}

/**
 * 瓦片范围
 */
data class TileRange(
    val startX: Int,
    val endX: Int,
    val startY: Int,
    val endY: Int,
)

/**
 * 离线区域
 */
data class OfflineArea(
    val zoom: Int,
    val x: Int,
    val y: Int,
    val size: Long,
)

// PathPlayer.kt
package com.steadywj.wjfakelocation.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 路径播放器
 *
 * 功能:
 * - 播放 GPX 轨迹
 * - 控制播放速度
 * - 暂停/继续/停止
 * - 循环播放
 */
@Singleton
class PathPlayer
    @Inject
    constructor() {
        /** 播放状态 */
        private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
        val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

        /** 当前播放位置索引 */
        private var currentIndex = 0

        /** 当前轨迹段 */
        private var currentSegment: TrackSegment? = null

        /** 播放速度（米/秒） */
        private var speedMps: Float = 1.0f

        /** 是否循环播放 */
        private var isLooping = false

        /** 播放协程 Job */
        private var playbackJob: Job? = null

        /** 位置更新回调 */
        var onPositionUpdate: ((TrackPoint) -> Unit)? = null

        /** 播放完成回调 */
        var onPlaybackComplete: (() -> Unit)? = null

        /**
         * 加载轨迹
         * @param segment 轨迹段
         * @param speedMps 播放速度（米/秒），默认步行速度 1.4m/s
         */
        suspend fun load(
            segment: TrackSegment,
            speedMps: Float = 1.4f,
        ) {
            stop() // 停止当前播放

            currentSegment = segment
            currentIndex = 0
            this.speedMps = speedMps.coerceIn(0.5f, 20.0f) // 限制速度范围

            _playbackState.value = PlaybackState.READY

            // 定位到起点
            if (segment.points.isNotEmpty()) {
                onPositionUpdate?.invoke(segment.points[0])
            }
        }

        /**
         * 开始播放
         */
        fun play() {
            if (_playbackState.value == PlaybackState.PLAYING) return
            if (currentSegment == null || currentSegment!!.points.isEmpty()) return

            _playbackState.value = PlaybackState.PLAYING

            playbackJob =
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                    playPath()
                }
        }

        /**
         * 暂停播放
         */
        fun pause() {
            if (_playbackState.value != PlaybackState.PLAYING) return

            _playbackState.value = PlaybackState.PAUSED
            playbackJob?.cancel()
        }

        /**
         * 继续播放
         */
        fun resume() {
            if (_playbackState.value != PlaybackState.PAUSED) return

            _playbackState.value = PlaybackState.PLAYING
            playbackJob =
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                    playPath()
                }
        }

        /**
         * 停止播放
         */
        fun stop() {
            _playbackState.value = PlaybackState.STOPPED
            playbackJob?.cancel()
            playbackJob = null
            currentIndex = 0
        }

        /**
         * 跳转到指定位置
         * @param index 轨迹点索引
         */
        fun seekTo(index: Int) {
            if (currentSegment == null) return

            currentIndex = index.coerceIn(0, currentSegment!!.points.size - 1)

            onPositionUpdate?.invoke(currentSegment!!.points[currentIndex])
        }

        /**
         * 设置循环播放
         */
        fun setLooping(looping: Boolean) {
            isLooping = looping
        }

        /**
         * 获取进度（0.0-1.0）
         */
        fun getProgress(): Float {
            if (currentSegment == null || currentSegment!!.points.isEmpty()) return 0f
            return currentIndex.toFloat() / currentSegment!!.points.size
        }

        /**
         * 获取当前轨迹点
         */
        fun getCurrentPoint(): TrackPoint? {
            return currentSegment?.points?.get(currentIndex)
        }

        /**
         * 获取剩余时间（秒）
         */
        fun getRemainingTimeSeconds(): Long {
            if (currentSegment == null) return 0

            val remainingPoints = currentSegment!!.points.size - currentIndex
            if (remainingPoints <= 0) return 0

            // 估算剩余距离
            var remainingDistance = 0.0
            for (i in currentIndex until currentSegment!!.points.size - 1) {
                remainingDistance +=
                    calculateDistance(
                        currentSegment!!.points[i].latitude,
                        currentSegment!!.points[i].longitude,
                        currentSegment!!.points[i + 1].latitude,
                        currentSegment!!.points[i + 1].longitude,
                    )
            }

            return (remainingDistance / speedMps).toLong()
        }

        // ==================== 内部方法 ====================

        /**
         * 播放路径
         */
        private suspend fun playPath() =
            withContext(Dispatchers.Default) {
                val points = currentSegment?.points ?: return@withContext

                while (currentIndex < points.size && _playbackState.value == PlaybackState.PLAYING) {
                    val currentPoint = points[currentIndex]

                    // 通知位置更新
                    withContext(Dispatchers.Main) {
                        onPositionUpdate?.invoke(currentPoint)
                    }

                    // 计算到下一个点的延迟
                    if (currentIndex < points.size - 1) {
                        val nextPoint = points[currentIndex + 1]
                        val distance =
                            calculateDistance(
                                currentPoint.latitude,
                                currentPoint.longitude,
                                nextPoint.latitude,
                                nextPoint.longitude,
                            )

                        val delayMs = (distance / speedMps * 1000).toLong()
                        delay(delayMs.coerceIn(100, 5000)) // 限制延迟范围
                    }

                    currentIndex++
                }

                // 播放完成
                if (currentIndex >= points.size) {
                    if (isLooping) {
                        // 循环播放
                        currentIndex = 0
                        withContext(Dispatchers.Main) {
                            play()
                        }
                    } else {
                        // 结束播放
                        _playbackState.value = PlaybackState.COMPLETED
                        withContext(Dispatchers.Main) {
                            onPlaybackComplete?.invoke()
                        }
                    }
                }
            }

        /**
         * 计算两点间距离（Haversine 公式）
         */
        private fun calculateDistance(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double,
        ): Double {
            val earthRadius = 6371000.0 // 米
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)

            val a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)

            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

            return earthRadius * c
        }
    }

/**
 * 播放状态
 */
enum class PlaybackState {
    READY, // 准备就绪
    PLAYING, // 播放中
    PAUSED, // 已暂停
    STOPPED, // 已停止
    COMPLETED, // 已完成
}

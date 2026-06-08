// MapInteractionManager.kt
package com.steadywj.wjfakelocation.manager.map.utils

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.amap.api.maps.AMap
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 地图交互管理?
 *
 * 功能:
 * - 长按添加标记?
 * - 拖拽微调位置
 * - 双击放大/缩小
 * - 手势识别
 */
@Singleton
class MapInteractionManager
    @Inject
    constructor() {
        /** 当前标记?*/
        private val _currentMarker = MutableStateFlow<Marker?>(null)
        val currentMarker: StateFlow<Marker?> = _currentMarker.asStateFlow()

        /** 最后点击位?*/
        private var lastTapPosition: LatLng? = null

        /** 手势检测器 */
        private var gestureDetector: GestureDetector? = null

        /** 是否正在拖拽 */
        private var isDragging = false

        /**
         * 初始化手势检测器
         */
        fun initialize(
            context: Context,
            aMap: AMap,
        ) {
            gestureDetector =
                GestureDetector(
                    context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onLongPress(e: MotionEvent) {
                            // 长按添加标记?
                            val screenX = e.x
                            val screenY = e.y

                            aMap.projection.fromScreenLocation(
                                android.graphics.Point(screenX.toInt(), screenY.toInt()),
                            )?.let { latLng ->
                                addMarkerAtLocation(aMap, latLng, "长按添加的位置")
                            }
                        }

                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            // 双击放大
                            val screenX = e.x
                            val screenY = e.y

                            aMap.projection.fromScreenLocation(
                                android.graphics.Point(screenX.toInt(), screenY.toInt()),
                            )?.let { latLng ->
                                // 以点击位置为中心放大
                                aMap.animateCamera(
                                    com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                        latLng,
                                        (aMap.cameraPosition.zoom + 2).coerceIn(3f, 18f),
                                    ),
                                )
                            }
                            return true
                        }

                        override fun onSingleTapUp(e: MotionEvent): Boolean {
                            // 单击选择标记?
                            val screenX = e.x
                            val screenY = e.y

                            aMap.projection.fromScreenLocation(
                                android.graphics.Point(screenX.toInt(), screenY.toInt()),
                            )?.let { latLng ->
                                lastTapPosition = latLng

                                // 检查是否点击到现有标记?
                                _currentMarker.value?.let { marker ->
                                    val distance = calculateDistance(latLng, marker.position)
                                    if (distance < 50) { // 50 米范围内
                                        selectMarker(marker)
                                    }
                                }
                            }
                            return true
                        }
                    },
                )

            // 设置地图触摸监听
            aMap.setOnMapTouchListener { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        isDragging = true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // 短距离移动视为点?
                            gestureDetector?.onTouchEvent(event)
                        }
                        isDragging = false
                    }
                }
            }

            // 设置标记点拖动监?
            aMap.setOnMarkerDragListener(
                object : AMap.OnMarkerDragListener {
                    override fun onMarkerDragStart(marker: Marker) {
                        // 开始拖?
                        selectMarker(marker)
                    }

                    override fun onMarkerDrag(marker: Marker) {
                        // 拖拽?- 实时更新位置
                        updateMarkerPosition(marker)
                    }

                    override fun onMarkerDragEnd(marker: Marker) {
                        // 拖拽结束 - 保存最终位?
                        finalizeMarkerPosition(marker)
                    }
                },
            )
        }

        /**
         * 在指定位置添加标记点
         */
        fun addMarkerAtLocation(
            aMap: AMap,
            latLng: LatLng,
            title: String = "目标位置",
        ): Marker? {
            // 移除旧标记点
            _currentMarker.value?.remove()

            try {
                val markerOptions =
                    MarkerOptions()
                        .position(latLng)
                        .title(title)
                        .snippet("${latLng.latitude}, ${latLng.longitude}")
                        .draggable(true) // 可拖?
                        .icon(com.amap.api.maps.model.BitmapDescriptorFactory.defaultMarker())

                val marker = aMap.addMarker(markerOptions)
                _currentMarker.value = marker

                // 移动到标记点位置
                aMap.animateCamera(
                    com.amap.api.maps.CameraUpdateFactory.newLatLng(latLng),
                )

                return marker
            } catch (e: Exception) {
                return null
            }
        }

        /**
         * 选择标记?
         */
        fun selectMarker(marker: Marker) {
            _currentMarker.value = marker
            marker.showInfoWindow()
        }

        /**
         * 更新标记点位置（拖拽中）
         */
        fun updateMarkerPosition(marker: Marker) {
            // 可以在这里实时更?UI 或显示坐标信?
        }

        /**
         * 确认标记点最终位置（拖拽结束?
         */
        fun finalizeMarkerPosition(marker: Marker) {
            val position = marker.position
            marker.snippet = "${position.latitude}, ${position.longitude}"

            // 通知 ViewModel 更新选中的位?
            // 通过回调?Flow 实现
        }

        /**
         * 清除所有标记点
         */
        fun clearMarkers() {
            _currentMarker.value?.remove()
            _currentMarker.value = null
        }

        /**
         * 计算两点间距离（米）
         */
        private fun calculateDistance(
            from: LatLng,
            to: LatLng,
        ): Double {
            val earthRadius = 6371000.0 // ?
            val dLat = Math.toRadians(to.latitude - from.latitude)
            val dLon = Math.toRadians(to.longitude - from.longitude)

            val a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(from.latitude)) * Math.cos(Math.toRadians(to.latitude)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)

            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

            return earthRadius * c
        }

        /**
         * 释放资源
         */
        fun destroy() {
            clearMarkers()
            gestureDetector = null
        }
    }

/**
 * Compose 扩展：地图交互修饰符
 */
@Composable
fun rememberMapInteraction(
    context: Context,
    aMap: AMap?,
    onMarkerMoved: ((LatLng) -> Unit)? = null,
): MapInteractionManager {
    val manager = remember { MapInteractionManager() }

    DisposableEffect(context, aMap) {
        aMap?.let { map ->
            manager.initialize(context, map)
        }

        onDispose {
            manager.destroy()
        }
    }

    return manager
}

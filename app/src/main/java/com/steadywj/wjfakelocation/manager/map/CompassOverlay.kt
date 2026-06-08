// CompassOverlay.kt
package com.steadywj.wjfakelocation.manager.map

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 罗盘方向指示?
 *
 * 功能:
 * - 实时获取设备朝向
 * - 在地图上显示方向箭头
 * - 自动旋转更新
 */
@Singleton
class CompassOverlay
    @Inject
    constructor() : SensorEventListener {
        /** 当前朝向角度?-360 度，正北?0?*/
        private val _heading = MutableStateFlow(0f)
        val heading: StateFlow<Float> = _heading.asStateFlow()

        /** 传感器管理器 */
        private var sensorManager: SensorManager? = null

        /** 方向标记?*/
        private var compassMarker: Marker? = null

        /** 地图实例 */
        private var aMap: AMap? = null

        /** 是否启用罗盘 */
        private var isEnabled = false

        /**
         * 初始化罗?
         * @param context 上下?
         * @param map 地图实例
         */
        fun initialize(
            context: Context,
            map: AMap,
        ) {
            aMap = map

            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

            // 添加方向箭头标记点（默认指向正北?
            addCompassMarker()

            enable()
        }

        /**
         * 添加方向箭头标记?
         */
        private fun addCompassMarker() {
            if (aMap == null) return

            try {
                // 使用高德地图内置的方向图?
                val markerOptions =
                    MarkerOptions()
                        .position(aMap!!.cameraPosition.target)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .anchor(0.5f, 0.5f)
                        .draggable(false)
                        .visible(true)

                compassMarker = aMap!!.addMarker(markerOptions)
            } catch (e: Exception) {
                // 忽略添加失败
            }
        }

        /**
         * 启用罗盘
         */
        fun enable() {
            if (isEnabled) return

            isEnabled = true

            // 注册方向传感?
            sensorManager?.registerListener(
                this,
                sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI,
            )
        }

        /**
         * 禁用罗盘
         */
        fun disable() {
            if (!isEnabled) return

            isEnabled = false
            sensorManager?.unregisterListener(this)
        }

        /**
         * 更新箭头方向
         */
        private fun updateCompassRotation(heading: Float) {
            compassMarker?.let { marker ->
                // 平滑旋转到目标角?
                marker.setRotateAngle(heading)
            }
        }

        /**
         * 释放资源
         */
        fun destroy() {
            disable()
            compassMarker?.remove()
            compassMarker = null
            aMap = null
            sensorManager = null
        }

        // ==================== SensorEventListener ====================

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type != Sensor.TYPE_ORIENTATION) return

            val azimuth = event.values[0]

            // 过滤抖动
            if (kotlin.math.abs(azimuth - _heading.value) > 2.0f) {
                _heading.value = azimuth
                updateCompassRotation(azimuth)
            }
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int,
        ) {
            // 精度变化时的回调（可选处理）
        }
    }

/**
 * Compose 扩展：记住罗盘组?
 */
@Composable
fun rememberCompassOverlay(map: AMap?): CompassOverlay {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val compassOverlay = remember { CompassOverlay() }

    DisposableEffect(context, map) {
        map?.let { aMap ->
            compassOverlay.initialize(context, aMap)
        }

        onDispose {
            compassOverlay.destroy()
        }
    }

    // 监听生命周期
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> compassOverlay.enable()
                    Lifecycle.Event.ON_PAUSE -> compassOverlay.disable()
                    else -> {}
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return compassOverlay
}

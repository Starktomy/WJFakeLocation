// BaiduMapView.kt
package com.steadywj.wjfakelocation.manager.map.components

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.model.LatLng

/**
 * 百度地图 MapView 包装?
 *
 * 功能:
 * - MapView 生命周期管理
 * - Compose 互操?
 * - 加载进度反馈
 */
@Composable
fun BaiduMapView(
    modifier: Modifier = Modifier,
    onMapReady: ((BaiduMap) -> Unit)? = null,
    initialLatitude: Double = 39.908823,
    initialLongitude: Double = 116.397470,
    zoomLevel: Float = 15f,
) {
    val context = LocalContext.current
    var isMapLoaded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    // 初始化地?
                    val baiduMap = map

                    // 设置初始位置
                    val currentLatLng = LatLng(initialLatitude, initialLongitude)
                    val update = MapStatusUpdateFactory.newLatLngZoom(currentLatLng, zoomLevel)
                    baiduMap.setMapStatus(update)

                    // 启用定位图层
                    baiduMap.isMyLocationEnabled = true

                    // 标记加载完成
                    isMapLoaded = true

                    // 回调
                    onMapReady?.invoke(baiduMap)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 显示加载进度?
        if (!isMapLoaded) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * 百度地图标记?
 */
@Composable
fun BaiduMapMarker(
    latitude: Double,
    longitude: Double,
    title: String? = null,
    snippet: String? = null,
    draggable: Boolean = true,
    onClick: (() -> Unit)? = null,
    baiduMap: BaiduMap?,
) {
    DisposableEffect(latitude, longitude, title, snippet, baiduMap) {
        var marker: Marker? = null
        if (baiduMap != null) {
            val latLng = LatLng(latitude, longitude)

            val markerOptions =
                MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .draggable(draggable)

            marker = baiduMap.addOverlay(markerOptions) as? Marker

            // 设置点击监听
            if (onClick != null) {
                baiduMap.setOnMarkerClickListener { clickedMarker ->
                    if (clickedMarker == marker) {
                        onClick()
                        true
                    } else {
                        false
                    }
                }
            }
        }

        onDispose {
            marker?.remove()
        }
    }
}

/**
 * 百度地图生命周期管理
 */
@Composable
fun rememberBaiduMapLifecycle(mapView: MapView?) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = androidx.compose.ui.platform.LocalContext.current

    DisposableEffect(lifecycleOwner, mapView) {
        if (mapView != null) {
            val map = mapView
            // onCreate
            map.onCreate(context, android.os.Bundle())

            val observer =
                androidx.lifecycle.LifecycleEventObserver { _, event ->
                    when (event) {
                        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> map.onResume()
                        androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> map.onPause()
                        androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> map.onDestroy()
                        else -> {}
                    }
                }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        } else {
            onDispose {}
        }
    }
}

// MapScreen.kt
package com.steadywj.wjfakelocation.manager.map

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.steadywj.wjfakelocation.R
import com.steadywj.wjfakelocation.manager.map.components.AMapView
import com.steadywj.wjfakelocation.manager.map.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
) {
    var showDrawer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.nav_map)) },
                navigationIcon = {
                    IconButton(onClick = { showDrawer = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 搜索功能 */ }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* 定位到当前位?*/ },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "当前位置")
            }
        },
    ) { paddingValues ->
        val viewModel: MapViewModel = hiltViewModel()
        val currentLat by remember { mutableDoubleStateOf(39.9042) }
        val currentLng by remember { mutableDoubleStateOf(116.4074) }
        val zoomLevel by remember { mutableFloatStateOf(14f) }

        // 集成高德地图 MapView
        AMapView(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            initialLatitude = currentLat,
            initialLongitude = currentLng,
            zoomLevel = zoomLevel,
            onMapReady = { aMap ->
                Log.d("MapScreen", "高德地图加载完成")
            },
        )
    }
}

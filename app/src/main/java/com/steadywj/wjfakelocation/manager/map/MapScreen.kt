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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.steadywj.wjfakelocation.R
import com.steadywj.wjfakelocation.manager.map.components.AMapView
import com.steadywj.wjfakelocation.manager.map.viewmodel.MapViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
) {
    val viewModel: MapViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Menu, contentDescription = null) },
                    label = { Text("设置 (API Key)") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.nav_map)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO 搜索功能 */ }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    },
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    if (selectedLocation != null) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.toggleFakeLocation() },
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(if (isPlaying) "停止模拟" else "开始模拟")
                        }
                    }
                    FloatingActionButton(
                        onClick = { /* TODO 定位到当前位?*/ },
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "当前位置")
                    }
                }
            },
        ) { paddingValues ->
            val currentLat by remember { mutableDoubleStateOf(39.9042) }
            val currentLng by remember { mutableDoubleStateOf(116.4074) }
            val zoomLevel by remember { mutableFloatStateOf(14f) }
            var aMapInstance by remember { mutableStateOf<com.amap.api.maps.AMap?>(null) }

            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // 集成高德地图 MapView
                AMapView(
                    modifier = Modifier.fillMaxSize(),
                    initialLatitude = currentLat,
                    initialLongitude = currentLng,
                    zoomLevel = zoomLevel,
                    onMapReady = { aMap ->
                        Log.d("MapScreen", "高德地图加载完成")
                        aMapInstance = aMap
                        aMap.setOnMapClickListener { latLng ->
                            viewModel.selectLocation(latLng.latitude, latLng.longitude)
                        }
                    },
                )

                selectedLocation?.let { loc ->
                    com.steadywj.wjfakelocation.manager.map.components.MapMarker(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        title = "模拟位置",
                        map = aMapInstance
                    )
                }
            }
        }
    }
}

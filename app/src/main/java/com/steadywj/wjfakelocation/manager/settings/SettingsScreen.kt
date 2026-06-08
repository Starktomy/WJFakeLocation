// SettingsScreen.kt
package com.steadywj.wjfakelocation.manager.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.steadywj.wjfakelocation.R
import com.steadywj.wjfakelocation.manager.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToApiKeySettings: () -> Unit,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showAccuracyDialog by remember { mutableStateOf(false) }
    var showAltitudeDialog by remember { mutableStateOf(false) }
    var showRandomizeDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
        ) {
            // 定位设置分组
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_location),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 精度设置
                    SettingSwitchItem(
                        title = stringResource(id = R.string.settings_use_accuracy),
                        subtitle = if (settings.useAccuracy) "${settings.accuracy}米" else "关闭",
                        checked = settings.useAccuracy,
                        onCheckedChange = { viewModel.updateAccuracy(it, settings.accuracy) },
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // 海拔设置
                    SettingSwitchItem(
                        title = stringResource(id = R.string.settings_use_altitude),
                        subtitle = if (settings.useAltitude) "${settings.altitude}米" else "关闭",
                        checked = settings.useAltitude,
                        onCheckedChange = { viewModel.updateAltitude(it, settings.altitude) },
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // 随机偏移
                    SettingSwitchItem(
                        title = stringResource(id = R.string.settings_use_randomize),
                        subtitle = if (settings.useRandomize) "半径${settings.randomizeRadius}米" else "关闭",
                        checked = settings.useRandomize,
                        onCheckedChange = { viewModel.updateRandomize(it, settings.randomizeRadius) },
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // 速度设置
                    SettingSwitchItem(
                        title = stringResource(id = R.string.settings_use_speed),
                        subtitle = if (settings.useSpeed) "${settings.speed}米/秒" else "关闭",
                        checked = settings.useSpeed,
                        onCheckedChange = { viewModel.updateSpeed(it, settings.speed) },
                    )
                }
            }

            // 钉钉专项设置分组
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "钉钉专项防护",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = "高德地图反溯源",
                        subtitle = "拦截钉钉内部高德SDK底层经纬度，防安全组件逆向解析",
                        checked = settings.useDingTalkLocationHook,
                        onCheckedChange = { viewModel.updateDingTalkLocationHook(it) },
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingSwitchItem(
                        title = "环境污染与风控拦截",
                        subtitle = "剥离模拟器特征，阻断 LBSWUA/DDSEC 真实环境采集",
                        checked = settings.useDingTalkAntiDetect,
                        onCheckedChange = { viewModel.updateDingTalkAntiDetect(it) },
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingSwitchItem(
                        title = "屏蔽版本强更",
                        subtitle = "伪装为极高版本，拦截并阻断内部强制更新与升级弹窗",
                        checked = settings.useDingTalkUpdateHook,
                        onCheckedChange = { viewModel.updateDingTalkUpdateHook(it) },
                    )
                }
            }

            // 情景模式
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "情景模式",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("保存模式")
                        }

                        OutlinedButton(
                            onClick = {
                                // 实现加载模式逻辑，可显示模式选择对话?
                                // TODO: 添加模式选择对话?UI
                                showProfileDialog = true
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("加载模式")
                        }
                    }
                }
            }

            // API Key 配置
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.api_key_title),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(id = R.string.api_key_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onNavigateToApiKeySettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("配置 API Key")
                    }
                }
            }
        }

        // 显示成功提示
        uiState.showSuccessMessage?.let { message ->
            Snackbar(
                modifier =
                    Modifier
                        .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearMessage() }) {
                        Text("关闭")
                    }
                },
            ) {
                Text(message)
            }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearMessage()
            }
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

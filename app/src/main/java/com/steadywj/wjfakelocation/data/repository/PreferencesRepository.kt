// PreferencesRepository.kt
package com.steadywj.wjfakelocation.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.steadywj.wjfakelocation.data.model.LocationSettings
import com.steadywj.wjfakelocation.data.model.SelectedLocation
import com.steadywj.wjfakelocation.data.model.TargetMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val masterKey =
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        private val encryptedPrefs: SharedPreferences =
            EncryptedSharedPreferences.create(
                context,
                "encrypted_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

        private val prefs: SharedPreferences =
            context.getSharedPreferences("wjfakelocation_prefs", Context.MODE_PRIVATE)

        private val _settings = MutableStateFlow(LocationSettings())
        val settings: StateFlow<LocationSettings> = _settings.asStateFlow()

        init {
            loadSettings()
        }

        private fun loadSettings() {
            _settings.value =
                LocationSettings(
                    isPlaying = prefs.getBoolean("is_playing", false),
                    useAccuracy = prefs.getBoolean("use_accuracy", false),
                    accuracy = prefs.getFloat("accuracy", 0.0f).toDouble(),
                    useAltitude = prefs.getBoolean("use_altitude", false),
                    altitude = prefs.getFloat("altitude", 0.0f).toDouble(),
                    useRandomize = prefs.getBoolean("use_randomize", false),
                    randomizeRadius = prefs.getFloat("randomize_radius", 0.0f).toDouble(),
                    useVerticalAccuracy = prefs.getBoolean("use_vertical_accuracy", false),
                    verticalAccuracy = prefs.getFloat("vertical_accuracy", 0.0f),
                    useMeanSeaLevel = prefs.getBoolean("use_mean_sea_level", false),
                    meanSeaLevel = prefs.getFloat("mean_sea_level", 0.0f).toDouble(),
                    useMeanSeaLevelAccuracy = prefs.getBoolean("use_mean_sea_level_accuracy", false),
                    meanSeaLevelAccuracy = prefs.getFloat("mean_sea_level_accuracy", 0.0f),
                    useSpeed = prefs.getBoolean("use_speed", false),
                    speed = prefs.getFloat("speed", 0.0f),
                    useSpeedAccuracy = prefs.getBoolean("use_speed_accuracy", false),
                    speedAccuracy = prefs.getFloat("speed_accuracy", 0.0f),
                    selectedLocation = getSelectedLocation(),
                    targetMode = TargetMode.valueOf(prefs.getString("target_mode", TargetMode.GLOBAL.name) ?: TargetMode.GLOBAL.name),
                    targetPackages = prefs.getStringSet("target_packages", emptySet())?.toList() ?: emptyList(),
                    useDingTalkLocationHook = prefs.getBoolean("use_dingtalk_location_hook", false),
                    useDingTalkAntiDetect = prefs.getBoolean("use_dingtalk_anti_detect", false),
                    useDingTalkUpdateHook = prefs.getBoolean("use_dingtalk_update_hook", false),
                    useDingTalkCameraHook = prefs.getBoolean("use_dingtalk_camera_hook", false),
                    useDingTalkWifiHook = prefs.getBoolean("use_dingtalk_wifi_hook", false),
                    wifiSSID = prefs.getString("wifi_ssid", "Spoofed_WiFi") ?: "Spoofed_WiFi",
                    wifiBSSID = prefs.getString("wifi_bssid", "00:11:22:33:44:55") ?: "00:11:22:33:44:55",
                    wifiMac = prefs.getString("wifi_mac", "00:11:22:33:44:55") ?: "00:11:22:33:44:55",
                )
        }

        private fun getSelectedLocation(): SelectedLocation? {
            val lat =
                java.lang.Double.longBitsToDouble(
                    prefs.getLong("selected_latitude", java.lang.Double.doubleToRawLongBits(Double.MIN_VALUE))
                )
            val lng =
                java.lang.Double.longBitsToDouble(
                    prefs.getLong("selected_longitude", java.lang.Double.doubleToRawLongBits(Double.MIN_VALUE))
                )
            return if (lat != Double.MIN_VALUE && lng != Double.MIN_VALUE) {
                SelectedLocation(
                    latitude = lat,
                    longitude = lng,
                    address = prefs.getString("selected_address", null),
                )
            } else {
                null
            }
        }

        suspend fun updateIsPlaying(isPlaying: Boolean) {
            prefs.edit().putBoolean("is_playing", isPlaying).apply()
            _settings.value = _settings.value.copy(isPlaying = isPlaying)
        }

        suspend fun updateSelectedLocation(location: SelectedLocation?) {
            prefs.edit().apply {
                if (location != null) {
                    putLong("selected_latitude", java.lang.Double.doubleToRawLongBits(location.latitude))
                    putLong("selected_longitude", java.lang.Double.doubleToRawLongBits(location.longitude))
                    putString("selected_address", location.address)
                } else {
                    remove("selected_latitude")
                    remove("selected_longitude")
                    remove("selected_address")
                }
                apply()
            }
            _settings.value = _settings.value.copy(selectedLocation = location)
        }

        suspend fun updateSettings(settings: LocationSettings) {
            prefs.edit().apply {
                putBoolean("is_playing", settings.isPlaying)
                putBoolean("use_accuracy", settings.useAccuracy)
                putFloat("accuracy", settings.accuracy.toFloat())
                putBoolean("use_altitude", settings.useAltitude)
                putFloat("altitude", settings.altitude.toFloat())
                putBoolean("use_randomize", settings.useRandomize)
                putFloat("randomize_radius", settings.randomizeRadius.toFloat())
                putBoolean("use_vertical_accuracy", settings.useVerticalAccuracy)
                putFloat("vertical_accuracy", settings.verticalAccuracy)
                putBoolean("use_mean_sea_level", settings.useMeanSeaLevel)
                putFloat("mean_sea_level", settings.meanSeaLevel.toFloat())
                putBoolean("use_mean_sea_level_accuracy", settings.useMeanSeaLevelAccuracy)
                putFloat("mean_sea_level_accuracy", settings.meanSeaLevelAccuracy)
                putBoolean("use_speed", settings.useSpeed)
                putFloat("speed", settings.speed)
                putBoolean("use_speed_accuracy", settings.useSpeedAccuracy)
                putFloat("speed_accuracy", settings.speedAccuracy)
                putString("target_mode", settings.targetMode.name)
                putStringSet("target_packages", settings.targetPackages.toSet())
                putBoolean("use_dingtalk_location_hook", settings.useDingTalkLocationHook)
                putBoolean("use_dingtalk_anti_detect", settings.useDingTalkAntiDetect)
                putBoolean("use_dingtalk_update_hook", settings.useDingTalkUpdateHook)
                putBoolean("use_dingtalk_camera_hook", settings.useDingTalkCameraHook)
                putBoolean("use_dingtalk_wifi_hook", settings.useDingTalkWifiHook)
                putString("wifi_ssid", settings.wifiSSID)
                putString("wifi_bssid", settings.wifiBSSID)
                putString("wifi_mac", settings.wifiMac)
                apply()
            }
            _settings.value = settings
        }

        // API Key 管理（加密存储）
        fun getApiKey(): String? {
            return encryptedPrefs.getString("juhe_api_key", null)
        }

        suspend fun saveApiKey(apiKey: String) {
            encryptedPrefs.edit().putString("juhe_api_key", apiKey).apply()
        }

        fun clearApiKey() {
            encryptedPrefs.edit().remove("juhe_api_key").apply()
        }

        // 情景模式支持
        @Suppress("MaxLineLength")
        suspend fun saveProfile(
            name: String,
            settings: LocationSettings,
        ) {
            val profileKey = "profile_$name"
            prefs.edit().apply {
                // 使用 JSON 格式完整保存所有设置
                putString(
                    "${profileKey}_data",
                    "${settings.useAccuracy}|${settings.accuracy}|${settings.useAltitude}|${settings.altitude}|" +
                        "${settings.useRandomize}|${settings.randomizeRadius}|" +
                        "${settings.useVerticalAccuracy}|${settings.verticalAccuracy}|" +
                        "${settings.useMeanSeaLevel}|${settings.meanSeaLevel}|" +
                        "${settings.useMeanSeaLevelAccuracy}|${settings.meanSeaLevelAccuracy}|" +
                        "${settings.useSpeed}|${settings.speed}|${settings.useSpeedAccuracy}|${settings.speedAccuracy}|" +
                        "${settings.useDingTalkLocationHook}|${settings.useDingTalkAntiDetect}|" +
                        "${settings.useDingTalkUpdateHook}|${settings.useDingTalkCameraHook}|" +
                        "${settings.useDingTalkWifiHook}|${settings.wifiSSID}|${settings.wifiBSSID}|${settings.wifiMac}",
                )
                putLong("${profileKey}_timestamp", System.currentTimeMillis())
                apply()
            }
        }

        suspend fun loadProfile(name: String): LocationSettings? {
            val profileKey = "profile_$name"
            val data = prefs.getString("${profileKey}_data", null) ?: return null
            return deserializeSettings(data)
        }

        @Suppress("MaxLineLength", "UnusedPrivateMember")
        private fun serializeSettings(settings: LocationSettings): String {
            // 保留向后兼容的序列化方法
            return "${settings.useAccuracy}|${settings.accuracy}|${settings.useAltitude}|${settings.altitude}|" +
                "${settings.useRandomize}|${settings.randomizeRadius}|" +
                "${settings.useVerticalAccuracy}|${settings.verticalAccuracy}|" +
                "${settings.useMeanSeaLevel}|${settings.meanSeaLevel}|" +
                "${settings.useMeanSeaLevelAccuracy}|${settings.meanSeaLevelAccuracy}|" +
                "${settings.useSpeed}|${settings.speed}|${settings.useSpeedAccuracy}|${settings.speedAccuracy}|" +
                "${settings.useDingTalkLocationHook}|${settings.useDingTalkAntiDetect}|" +
                "${settings.useDingTalkUpdateHook}|${settings.useDingTalkCameraHook}|" +
                "${settings.useDingTalkWifiHook}|${settings.wifiSSID}|${settings.wifiBSSID}|${settings.wifiMac}"
        }

        @Suppress("TooGenericExceptionCaught", "SwallowedException", "CyclomaticComplexMethod", "ReturnCount")
        private fun deserializeSettings(data: String): LocationSettings? {
            try {
                val parts = data.split("|")
                if (parts.size < 16) return null

                return LocationSettings(
                    useAccuracy = parts[0].toBoolean(),
                    accuracy = parts[1].toDoubleOrNull() ?: 0.0,
                    useAltitude = parts[2].toBoolean(),
                    altitude = parts[3].toDoubleOrNull() ?: 0.0,
                    useRandomize = parts[4].toBoolean(),
                    randomizeRadius = parts[5].toDoubleOrNull() ?: 0.0,
                    useVerticalAccuracy = parts[6].toBoolean(),
                    verticalAccuracy = parts[7].toFloatOrNull() ?: 0.0f,
                    useMeanSeaLevel = parts[8].toBoolean(),
                    meanSeaLevel = parts[9].toDoubleOrNull() ?: 0.0,
                    useMeanSeaLevelAccuracy = parts[10].toBoolean(),
                    meanSeaLevelAccuracy = parts[11].toFloatOrNull() ?: 0.0f,
                    useSpeed = parts[12].toBoolean(),
                    speed = parts[13].toFloatOrNull() ?: 0.0f,
                    useSpeedAccuracy = parts[14].toBoolean(),
                    speedAccuracy = parts[15].toFloatOrNull() ?: 0.0f,
                    useDingTalkLocationHook = if (parts.size > 16) parts[16].toBoolean() else false,
                    useDingTalkAntiDetect = if (parts.size > 17) parts[17].toBoolean() else false,
                    useDingTalkUpdateHook = if (parts.size > 18) parts[18].toBoolean() else false,
                    useDingTalkCameraHook = if (parts.size > 19) parts[19].toBoolean() else false,
                    useDingTalkWifiHook = if (parts.size > 20) parts[20].toBoolean() else false,
                    wifiSSID = if (parts.size > 21) parts[21] else "Spoofed_WiFi",
                    wifiBSSID = if (parts.size > 22) parts[22] else "00:11:22:33:44:55",
                    wifiMac = if (parts.size > 23) parts[23] else "00:11:22:33:44:55",
                )
            } catch (e: Exception) {
                return null
            }
        }
    }

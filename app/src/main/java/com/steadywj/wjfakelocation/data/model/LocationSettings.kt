// LocationSettings.kt
package com.steadywj.wjfakelocation.data.model

data class LocationSettings(
    val isPlaying: Boolean = false,
    val useAccuracy: Boolean = false,
    val accuracy: Double = 0.0,
    val useAltitude: Boolean = false,
    val altitude: Double = 0.0,
    val useRandomize: Boolean = false,
    val randomizeRadius: Double = 0.0,
    val useVerticalAccuracy: Boolean = false,
    val verticalAccuracy: Float = 0.0f,
    val useMeanSeaLevel: Boolean = false,
    val meanSeaLevel: Double = 0.0,
    val useMeanSeaLevelAccuracy: Boolean = false,
    val meanSeaLevelAccuracy: Float = 0.0f,
    val useSpeed: Boolean = false,
    val speed: Float = 0.0f,
    val useSpeedAccuracy: Boolean = false,
    val speedAccuracy: Float = 0.0f,
    val selectedLocation: SelectedLocation? = null,
    val targetMode: TargetMode = TargetMode.GLOBAL,
    val targetPackages: List<String> = emptyList(),
    val useDingTalkLocationHook: Boolean = false,
    val useDingTalkAntiDetect: Boolean = false,
    val useDingTalkUpdateHook: Boolean = false,
    val useDingTalkCameraHook: Boolean = false,
)

data class SelectedLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
)

enum class TargetMode {
    GLOBAL,
    APP_SPECIFIC,
}

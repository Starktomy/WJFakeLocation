// AIService.kt
package com.steadywj.wjfakelocation.domain.service

import android.content.Context
import android.location.Location
import com.steadywj.wjfakelocation.data.model.FavoriteLocation
import com.steadywj.wjfakelocation.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 智能服务
 *
 * 功能:
 * - 常用地点学习
 * - 出行路线预测
 * - 自动化场景触发
 * - 智能推荐
 */
@Singleton
class AIService
    @Inject
    constructor(
        private val context: Context,
        private val favoritesRepository: FavoritesRepository,
    ) {
        /** 用户行为模式数据 */
        private val _userPatterns = MutableStateFlow(UserBehaviorPatterns())
        val userPatterns: Flow<UserBehaviorPatterns> = _userPatterns.asStateFlow()

        /** 预测结果 */
        private val _predictions = MutableStateFlow<List<LocationPrediction>>(emptyList())
        val predictions: Flow<List<LocationPrediction>> = _predictions.asStateFlow()

        /**
         * 记录位置访问
         * @param location 位置
         * @param timestamp 时间戳
         */
        suspend fun recordLocationVisit(
            location: FavoriteLocation,
            timestamp: Long,
        ) {
            val patterns =
                _userPatterns.value.copy(
                    visitedLocations =
                        _userPatterns.value.visitedLocations +
                            LocationVisit(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                name = location.name,
                                category = location.category,
                                timestamp = timestamp,
                                visitCount = 1,
                            ),
                )

            // 合并相同地点
            val mergedLocations = mergeDuplicateVisits(patterns.visitedLocations)
            _userPatterns.value = patterns.copy(visitedLocations = mergedLocations)

            // 更新预测
            // updatePredictions()
        }

        /**
         * 学习用户习惯
         * 分析访问频率、时间段、停留时长等
         */
        suspend fun learnUserHabits() {
            val patterns = _userPatterns.value

            // 分析时间规律
            val timePatterns = analyzeTimePatterns(patterns.visitedLocations)

            // 分析地点偏好
            val locationPreferences = analyzeLocationPreferences(patterns.visitedLocations)

            // 更新模式
            _userPatterns.value =
                patterns.copy(
                    timePatterns = timePatterns,
                    locationPreferences = locationPreferences,
                )
        }

        /**
         * 预测目的地
         * @param currentLocation 当前位置
         * @param currentTime 当前时间
         * @return 预测的目的地列表（按概率排序）
         */
        fun predictDestination(
            currentLocation: Location,
            currentTime: Long,
        ): List<LocationPrediction> {
            val patterns = _userPatterns.value

            return patterns.visitedLocations
                .map { visit ->
                    val probability = calculateProbability(visit, currentTime, currentLocation)
                    LocationPrediction(
                        name = visit.name,
                        latitude = visit.latitude,
                        longitude = visit.longitude,
                        probability = probability,
                        reason = generateReason(visit, currentTime),
                    )
                }
                .filter { it.probability > 0.3 }
                .sortedByDescending { it.probability }
        }

        /**
         * 自动化场景触发
         * 当到达特定地点时自动启用虚拟定位
         */
        suspend fun checkAutoTrigger(currentLocation: Location): AutoTriggerResult? {
            val patterns = _userPatterns.value

            // 查找附近的常用地点
            val nearbyLocation =
                patterns.visitedLocations.find { visit ->
                    val distance =
                        calculateDistance(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            visit.latitude,
                            visit.longitude,
                        )
                    distance < 100.0
                } ?: return null

            // 检查是否有自动化规则
            val autoRule =
                patterns.autoRules.find { rule ->
                    rule.locationName == nearbyLocation.name
                } ?: return null

            return AutoTriggerResult(
                action = autoRule.action,
                locationName = nearbyLocation.name,
                confidence = 0.9,
            )
        }

        /**
         * 添加自动化规则
         */
        suspend fun addAutoRule(rule: AutomationRule) {
            val patterns = _userPatterns.value
            _userPatterns.value =
                patterns.copy(
                    autoRules = patterns.autoRules + rule,
                )
        }

        /**
         * 清除学习数据
         */
        suspend fun clearLearningData() {
            _userPatterns.value = UserBehaviorPatterns()
            _predictions.value = emptyList()
        }

        // ==================== 内部方法 ====================

        /**
         * 合并重复访问记录
         */
        private fun mergeDuplicateVisits(visits: List<LocationVisit>): List<LocationVisit> {
            return visits.groupBy { "${it.latitude},${it.longitude}" }
                .map { (_, group) ->
                    val first = group.first()
                    LocationVisit(
                        latitude = first.latitude,
                        longitude = first.longitude,
                        name = first.name,
                        category = first.category,
                        timestamp = group.maxOf { it.timestamp },
                        visitCount = group.sumOf { it.visitCount },
                    )
                }
        }

        /**
         * 分析时间模式
         */
        private fun analyzeTimePatterns(visits: List<LocationVisit>): TimePatterns {
            val hourDistribution = IntArray(24)
            val dayDistribution = IntArray(7)

            visits.forEach { visit ->
                val hour =
                    java.util.Calendar.getInstance().apply {
                        timeInMillis = visit.timestamp
                    }.get(java.util.Calendar.HOUR_OF_DAY)

                val dayOfWeek =
                    java.util.Calendar.getInstance().apply {
                        timeInMillis = visit.timestamp
                    }.get(java.util.Calendar.DAY_OF_WEEK) - 1

                hourDistribution[hour]++
                dayDistribution[dayOfWeek]++
            }

            return TimePatterns(
                peakHours = hourDistribution.indices.maxByOrNull { hourDistribution[it] } ?: 9,
                weekendActivityRatio =
                    (dayDistribution[6] + dayDistribution[0]).toFloat() /
                        (dayDistribution.slice(1..5).sum() + 1),
            )
        }

        /**
         * 分析地点偏好
         */
        private fun analyzeLocationPreferences(visits: List<LocationVisit>): LocationPreferences {
            val categoryCount = visits.groupingBy { it.category }.eachCount()
            val topCategory = categoryCount.maxByOrNull { it.value }?.key ?: "default"

            return LocationPreferences(
                favoriteCategories = categoryCount.keys.sortedByDescending { categoryCount[it] },
                mostVisitedLocation = visits.maxByOrNull { it.visitCount }?.name ?: "",
            )
        }

        /**
         * 计算概率
         */
        private fun calculateProbability(
            visit: LocationVisit,
            currentTime: Long,
            currentLocation: Location,
        ): Double {
            val calendar =
                java.util.Calendar.getInstance().apply {
                    timeInMillis = currentTime
                }
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentDay = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1

            // 基础概率（访问次数）
            val baseProbability = kotlin.math.min(visit.visitCount.toDouble() / 10.0, 1.0)

            // 时间匹配度
            val timeMatch = if (kotlin.math.abs(currentHour - visit.timestamp % 86400000 / 3600000) < 2) 1.0 else 0.5

            // 星期匹配度
            val dayMatch = if (currentDay == 0 || currentDay == 6) 0.8 else 0.6

            // 距离衰减
            val distance =
                calculateDistance(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    visit.latitude,
                    visit.longitude,
                )
            val distanceDecay = kotlin.math.exp(-distance / 5000.0)

            return baseProbability * timeMatch * dayMatch * distanceDecay
        }

        /**
         * 生成推荐理由
         */
        private fun generateReason(
            visit: LocationVisit,
            currentTime: Long,
        ): String {
            val calendar =
                java.util.Calendar.getInstance().apply {
                    timeInMillis = currentTime
                }
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

            return when {
                visit.visitCount > 5 -> "您常去的地点（${visit.visitCount}次）"
                hour in 7..9 -> "上班时间可能前往"
                hour in 17..19 -> "下班时间可能前往"
                else -> "根据您的习惯推荐"
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
            val earthRadius = 6371000.0
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

// ==================== 数据模型 ====================

/**
 * 用户行为模式
 */
data class UserBehaviorPatterns(
    val visitedLocations: List<LocationVisit> = emptyList(),
    val timePatterns: TimePatterns = TimePatterns(),
    val locationPreferences: LocationPreferences = LocationPreferences(),
    val autoRules: List<AutomationRule> = emptyList(),
)

/**
 * 地点访问记录
 */
data class LocationVisit(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val category: String,
    val timestamp: Long,
    val visitCount: Int,
)

/**
 * 时间模式
 */
data class TimePatterns(
    val peakHours: Int = 9,
    val weekendActivityRatio: Float = 0.5f,
)

/**
 * 地点偏好
 */
data class LocationPreferences(
    val favoriteCategories: List<String> = emptyList(),
    val mostVisitedLocation: String = "",
)

/**
 * 位置预测
 */
data class LocationPrediction(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val probability: Double,
    val reason: String,
)

/**
 * 自动化规则
 */
data class AutomationRule(
    val locationName: String,
    val action: AutomationAction,
    val enabled: Boolean = true,
)

/**
 * 自动化动作
 */
enum class AutomationAction {
    ENABLE_FAKE_LOCATION,
    DISABLE_FAKE_LOCATION,
    SWITCH_PROFILE,
    NOTIFY_USER,
}

/**
 * 自动触发结果
 */
data class AutoTriggerResult(
    val action: AutomationAction,
    val locationName: String,
    val confidence: Double,
)

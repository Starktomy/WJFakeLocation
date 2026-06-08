// PoiSearchManager.kt
package com.steadywj.wjfakelocation.manager.search.utils

import android.content.Context
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.amap.api.services.poisearch.PoiResult as AMapPoiResult

/**
 * POI 搜索管理?
 *
 * 功能:
 * - 周边搜索（美食、酒店等?
 * - POI 分类筛?
 * - 关键词搜?
 */
@Singleton
class PoiSearchManager
    @Inject
    constructor(
        private val context: Context,
    ) {
        /**
         * 周边搜索
         * @param latitude 纬度
         * @param longitude 经度
         * @param radius 半径（米），默认 3000 ?
         * @param type POI 类型（餐饮、酒店等?
         * @return POI 列表
         */
        fun searchNearby(
            latitude: Double,
            longitude: Double,
            radius: Int = 3000,
            type: PoiType = PoiType.FOOD,
        ): Flow<List<PoiResult>> =
            callbackFlow {
                withContext(Dispatchers.IO) {
                    try {
                        val query = PoiSearch.Query("", type.code, "北京")
                        val poiSearch = PoiSearch(context, query)

                        // 设置范围
                        query.setPageSize(20)
                        query.setPageNum(1)

                        poiSearch.setQuery(query)
                        poiSearch.bound = PoiSearch.SearchBound(LatLonPoint(latitude, longitude), radius)

                        poiSearch.setOnPoiSearchListener(
                            object : PoiSearch.OnPoiSearchListener {
                                override fun onPoiSearched(
                                    result: AMapPoiResult?,
                                    errorCode: Int,
                                ) {
                                    if (errorCode == 0 && result != null) {
                                        val pois =
                                            result.pois.mapNotNull { poiItem ->
                                                poiItem.toPoiResult()
                                            }
                                        trySend(pois)
                                    } else {
                                        trySend(emptyList())
                                    }
                                }

                                override fun onPoiItemSearched(
                                    poiItem: PoiItem?,
                                    errorCode: Int,
                                ) {
                                    // 单个 POI 搜索结果（不使用?
                                }
                            },
                        )
                        poiSearch.searchPOIAsyn()
                    } catch (e: Exception) {
                        trySend(emptyList())
                    }
                }

                awaitClose {}
            }

        /**
         * 关键词搜?
         * @param keyword 关键?
         * @param city 城市
         * @return POI 列表
         */
        fun searchByKeyword(
            keyword: String,
            city: String = "全国",
        ): Flow<List<PoiResult>> =
            callbackFlow {
                withContext(Dispatchers.IO) {
                    try {
                        val query = PoiSearch.Query(keyword, "", city)
                        val poiSearch = PoiSearch(context, query)

                        query.setPageSize(20)
                        query.setPageNum(1)

                        poiSearch.setQuery(query)

                        poiSearch.setOnPoiSearchListener(
                            object : PoiSearch.OnPoiSearchListener {
                                override fun onPoiSearched(
                                    result: AMapPoiResult?,
                                    errorCode: Int,
                                ) {
                                    if (errorCode == 0 && result != null) {
                                        val pois =
                                            result.pois.mapNotNull { poiItem ->
                                                poiItem.toPoiResult()
                                            }
                                        trySend(pois)
                                    } else {
                                        trySend(emptyList())
                                    }
                                }

                                override fun onPoiItemSearched(
                                    poiItem: PoiItem?,
                                    errorCode: Int,
                                ) {
                                    // 单个 POI 搜索结果
                                }
                            },
                        )

                        poiSearch.searchPOIAsyn()
                    } catch (e: Exception) {
                        trySend(emptyList())
                    }
                }

                awaitClose {}
            }

        /**
         * 获取热门推荐
         * @return 热门 POI 类型列表
         */
        fun getPopularTypes(): List<PoiType> {
            return listOf(
                PoiType.FOOD,
                PoiType.HOTEL,
                PoiType.SHOPPING,
                PoiType.TRANSPORT,
                PoiType.ENTERTAINMENT,
            )
        }
    }

/**
 * POI 结果
 */
data class PoiResult(
    val id: String,
    val name: String,
    val type: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Float?,
    val tel: String?,
    val rating: Float?,
)

/**
 * POI 类型枚举（高德地?POI 编码?
 */
enum class PoiType(val code: String, val displayName: String) {
    FOOD("餐饮服务", "美食"),
    HOTEL("住宿服务", "酒店"),
    SHOPPING("购物服务", "购物"),
    TRANSPORT("交通设施", "交通"),
    ENTERTAINMENT("休闲娱乐", "娱乐"),
    EDUCATION("教育培训", "教育"),
    MEDICAL("医疗服务", "医疗"),
    FINANCE("金融保险", "金融"),
    GOVERNMENT("政府机构", "政府"),
    TOURIST("旅游景点", "旅游"),
    DEFAULT("", "全部"),
}

/**
 * PoiItem 扩展函数
 */
fun PoiItem.toPoiResult(): PoiResult {
    return PoiResult(
        id = this.poiId,
        name = this.title,
        type = this.typeDes ?: "",
        address = this.snippet ?: "",
        latitude = this.latLonPoint.latitude,
        longitude = this.latLonPoint.longitude,
        distance = this.distance.toFloat(),
        tel = this.tel,
        rating = null,
    )
}

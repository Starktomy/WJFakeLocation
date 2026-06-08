// MapProviderFactory.kt
package com.steadywj.wjfakelocation.domain.service

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 地图提供商工厂
 *
 * 工厂模式：根据配置创建不同的地图引擎实例
 */
@Singleton
class MapProviderFactory
    @Inject
    constructor() {
        /** 当前地图类型 */
        private var currentMapType: MapType = MapType.AMAP

        /** 缓存的地图实例 */
        private val mapProviders = mutableMapOf<MapType, MapProvider>()

        init {
            // 预注册支持的地图提供商
            registerProvider(MapType.AMAP, AMapProvider())
            registerProvider(MapType.BAIDU, BaiduMapProvider())
            registerProvider(MapType.GOOGLE, GoogleMapProvider())
        }

        /**
         * 注册地图提供商
         */
        fun registerProvider(
            type: MapType,
            provider: MapProvider,
        ) {
            mapProviders[type] = provider
        }

        /**
         * 获取地图提供商实例
         */
        fun getProvider(type: MapType): MapProvider? {
            return mapProviders[type]
        }

        /**
         * 设置当前地图类型
         */
        fun setCurrentMapType(type: MapType) {
            currentMapType = type
        }

        /**
         * 获取当前地图类型
         */
        fun getCurrentMapType(): MapType {
            return currentMapType
        }

        /**
         * 获取当前地图提供商
         */
        fun getCurrentProvider(): MapProvider? {
            return mapProviders[currentMapType]
        }

        /**
         * 列出所有可用的地图
         */
        fun getAvailableMaps(): List<MapInfo> {
            return mapProviders.keys.mapNotNull { type ->
                val provider = mapProviders[type]
                if (provider != null) {
                    MapInfo(
                        type = type,
                        name = provider.getMapName(),
                        description = type.description,
                        isChinaSupported = provider.isChinaSupported(),
                    )
                } else {
                    null
                }
            }
        }
    }

/**
 * 地图信息
 */
data class MapInfo(
    val type: MapType,
    val name: String,
    val description: String,
    val isChinaSupported: Boolean,
)

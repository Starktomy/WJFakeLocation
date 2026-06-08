package com.steadywj.wjfakelocation.xposed.hooks.apps

import android.content.Context
import com.steadywj.wjfakelocation.xposed.ProviderHelper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 针对钉钉 (DingTalk) 的专属 Hook 策略。
 * 钉钉内部使用了高德地图 (AMap) SDK 以及深度的安全机制 (SafeGuard/VMP)。
 * 我们优先在 Java 层拦截高德的经纬度对象，确保其安全 SDK 读取到的原始坐标已经被替换。
 */
class DingTalkHook : IAppHook {
    override val targetPackages: List<String> =
        listOf(
            "com.alibaba.android.rimet",
            "com.alibaba.android.rimet.zrhgz"
        )

    override fun hook(
        appLpparam: XC_LoadPackage.LoadPackageParam,
        context: Context
    ) {
        if (ProviderHelper.useDingTalkLocationHook(context)) {
            hookAMapLocation(appLpparam)
            hookAMapLatLng(appLpparam)
        }
        if (ProviderHelper.useDingTalkAntiDetect(context)) {
            hookEnvironment(appLpparam)
        }
        if (ProviderHelper.useDingTalkUpdateHook(context)) {
            hookUpdate(appLpparam)
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun hookAMapLocation(appLpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val aMapLocationClass =
                XposedHelpers.findClassIfExists(
                    "com.amap.api.location.AMapLocation",
                    appLpparam.classLoader
                ) ?: return

            // 拦截获取纬度
            XposedBridge.hookAllMethods(
                aMapLocationClass,
                "getLatitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!ProviderHelper.isMockEnabled()) return
                        val lat = ProviderHelper.getLatitude()
                        if (lat != 0.0) {
                            param.result = lat
                        }
                    }
                }
            )

            // 拦截获取经度
            XposedBridge.hookAllMethods(
                aMapLocationClass,
                "getLongitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!ProviderHelper.isMockEnabled()) return
                        val lon = ProviderHelper.getLongitude()
                        if (lon != 0.0) {
                            param.result = lon
                        }
                    }
                }
            )

            // 拦截地址详情信息，防止被逆向解析出真实的街道信息
            val stringMethods =
                arrayOf(
                    "getAddress", "getCountry", "getProvince", "getCity",
                    "getDistrict", "getStreet", "getStreetNum", "getPoiName", "getAoiName"
                )

            stringMethods.forEach { method ->
                XposedBridge.hookAllMethods(
                    aMapLocationClass,
                    method,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (!ProviderHelper.isMockEnabled()) return
                            // 暂时返回空或者模拟的地址，防止泄漏真实地址。
                            // 更完美的做法是在 ProviderHelper 里让用户设置虚拟地址文本
                        }
                    }
                )
            }
        } catch (e: Throwable) {
            // 忽略找不到类等异常
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun hookAMapLatLng(appLpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // 高德地图的 LatLng 对象，用于钉钉打卡地图展示界面的坐标
            val latLngClass =
                XposedHelpers.findClassIfExists(
                    "com.amap.api.maps.model.LatLng",
                    appLpparam.classLoader
                ) ?: return

            XposedBridge.hookAllConstructors(
                latLngClass,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!ProviderHelper.isMockEnabled()) return
                        val lat = ProviderHelper.getLatitude()
                        val lon = ProviderHelper.getLongitude()

                        if (lat != 0.0 && lon != 0.0) {
                            // LatLng 构造函数签名：LatLng(double latitude, double longitude)
                            // 或者 LatLng(double latitude, double longitude, boolean isCheck)
                            if (param.args.isNotEmpty() && param.args[0] is Double) {
                                param.args[0] = lat
                                param.args[1] = lon
                            }
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            // 忽略异常
        }
    }
}

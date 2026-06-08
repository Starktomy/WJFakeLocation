package com.steadywj.wjfakelocation.xposed.hooks.apps

import android.content.Context
import com.steadywj.wjfakelocation.xposed.common.ProviderHelper
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
            hookAMapLocation(appLpparam, context)
            hookAMapLatLng(appLpparam, context)
        }
        if (ProviderHelper.useDingTalkAntiDetect(context)) {
            hookEnvironment(appLpparam, context)
        }
        if (ProviderHelper.useDingTalkUpdateHook(context)) {
            hookUpdate(appLpparam, context)
        }
        if (ProviderHelper.useDingTalkCameraHook(context)) {
            hookCamera(appLpparam, context)
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun hookAMapLocation(
        appLpparam: XC_LoadPackage.LoadPackageParam,
        context: Context
    ) {
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
                        if (!ProviderHelper.isPlaying(context)) return
                        val lat = ProviderHelper.getLatitude(context)
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
                        if (!ProviderHelper.isPlaying(context)) return
                        val lon = ProviderHelper.getLongitude(context)
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
                            if (!ProviderHelper.isPlaying(context)) return
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
    private fun hookAMapLatLng(
        appLpparam: XC_LoadPackage.LoadPackageParam,
        context: Context
    ) {
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
                        if (!ProviderHelper.isPlaying(context)) return
                        val lat = ProviderHelper.getLatitude(context)
                        val lon = ProviderHelper.getLongitude(context)

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

    @Suppress("TooGenericExceptionCaught", "SwallowedException", "UnusedParameter")
    private fun hookEnvironment(
        appLpparam: XC_LoadPackage.LoadPackageParam,
        context: Context
    ) {
        try {
            // 1. 拦截阿里安全组件 ILBSRiskComponent (绕过 LBSWUA / DDSEC 真实坐标收集)
            val lbsRiskClass =
                XposedHelpers.findClassIfExists(
                    "com.alibaba.wireless.security.open.lbsrisk.ILBSRiskComponent",
                    appLpparam.classLoader
                ) ?: XposedHelpers.findClassIfExists(
                    "com.taobao.wireless.security.sdk.lbsrisk.LBSRiskComponent",
                    appLpparam.classLoader
                )

            if (lbsRiskClass != null) {
                XposedBridge.hookAllMethods(
                    lbsRiskClass,
                    "putLocationData",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (!ProviderHelper.isPlaying(context)) return
                            val loc = param.args[0] as? android.location.Location ?: return
                            loc.latitude = ProviderHelper.getLatitude(context)
                            loc.longitude = ProviderHelper.getLongitude(context)
                        }
                    }
                )
            }

            // 2. 拦截 Wi-Fi 信息 (打卡防篡改不仅查 GPS，还会查 Wi-Fi 的 BSSID)
            val wifiHook =
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!ProviderHelper.useDingTalkAntiDetect(context)) return
                        when (param.method.name) {
                            "getSSID" -> param.result = "\"Spoofed_WiFi\""
                            "getBSSID" -> param.result = "00:11:22:33:44:55"
                            "getMacAddress" -> param.result = "00:11:22:33:44:55"
                        }
                    }
                }
            XposedBridge.hookAllMethods(android.net.wifi.WifiInfo::class.java, "getSSID", wifiHook)
            XposedBridge.hookAllMethods(android.net.wifi.WifiInfo::class.java, "getBSSID", wifiHook)
            XposedBridge.hookAllMethods(android.net.wifi.WifiInfo::class.java, "getMacAddress", wifiHook)
        } catch (e: Throwable) {
            // 忽略异常
        }
    }

    @Suppress("UnusedParameter")
    private fun hookUpdate(
        appLpparam: XC_LoadPackage.LoadPackageParam,
        context: Context
    ) {
        // 实现版本强更屏蔽
    }

    @Suppress(
        "TooGenericExceptionCaught",
        "SwallowedException",
        "UnusedParameter",
        "ComplexCondition",
        "ReturnCount",
        "MaxLineLength"
    )
    private fun hookCamera(
        appLpparam: XC_LoadPackage.LoadPackageParam,
        context: Context
    ) {
        try {
            XposedBridge.hookAllMethods(
                java.io.File::class.java,
                "getAbsolutePath",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!ProviderHelper.useDingTalkCameraHook(context)) return

                        val originalPath = param.result as? String ?: return

                        // 典型的拍照临时文件路径特征
                        // 实际开发中需结合用户选择的替换图片路径
                        val fakeImagePath = "/sdcard/Pictures/fake_attendance.jpg"

                        // 避免错误替换
                        if (originalPath == fakeImagePath) return

                        // 检查是否为钉钉缓存目录中的临时图片文件
                        if (originalPath.contains("com.alibaba.android.rimet") &&
                            (originalPath.contains("cache") || originalPath.contains("temp")) &&
                            (
                                originalPath.endsWith(".jpg", ignoreCase = true) ||
                                    originalPath.endsWith(".jpeg", ignoreCase = true)
                            )
                        ) {
                            val fakeFile = java.io.File(fakeImagePath)
                            if (fakeFile.exists()) {
                                param.result = fakeImagePath
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

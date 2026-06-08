// MainHook.kt
package com.steadywj.wjfakelocation.xposed

import android.app.Application
import android.content.Context
import com.steadywj.wjfakelocation.xposed.hooks.LocationApiHooks
import com.steadywj.wjfakelocation.xposed.hooks.SystemServicesHooks
import com.steadywj.wjfakelocation.xposed.hooks.TelephonyHook
import com.steadywj.wjfakelocation.xposed.hooks.WifiHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {
    private val tag = "[WJFakeLocation-Hook]"

    private var context: Context? = null

    private var locationApiHooks: LocationApiHooks? = null
    private var systemServicesHooks: SystemServicesHooks? = null
    private var telephonyHook: TelephonyHook? = null
    private var wifiHook: WifiHook? = null

    @Suppress("TooGenericExceptionCaught")
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // 避免 hook 自身应用导致递归，但需要挂钩自身以检测模块状态
        if (lpparam.packageName == "com.steadywj.wjfakelocation") {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.steadywj.wjfakelocation.manager.MainActivity",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
                )
            } catch (e: Throwable) {
                XposedBridge.log("$tag Error hooking isModuleActive: ${e.message}")
            }
            return
        }

        // 在 android 进程中 hook 系统服务
        if (lpparam.packageName == "android") {
            systemServicesHooks = SystemServicesHooks(lpparam).also { it.initHooks() }
            XposedBridge.log("$tag System services hook initialized")
            return
        }

        initHookingLogic(lpparam)
    }

    private fun initHookingLogic(lpparam: LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Instrumentation",
                lpparam.classLoader,
                "callApplicationOnCreate",
                Application::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        context =
                            (param.args[0] as Application).applicationContext.also {
                                XposedBridge.log(
                                    "$tag Target app context acquired successfully for ${lpparam.packageName}"
                                )
                            }

                        locationApiHooks = LocationApiHooks(lpparam, context!!).also { it.initHooks() }
                        telephonyHook = TelephonyHook(lpparam, context!!).also { it.initHooks() }
                        wifiHook = WifiHook(lpparam, context!!).also { it.initHooks() }
                    }
                },
            )
        } catch (e: Exception) {
            XposedBridge.log("$tag Error initializing hook logic: ${e.message}")
        }
    }
}

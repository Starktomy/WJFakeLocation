package com.steadywj.wjfakelocation.xposed.hooks.apps

import android.content.Context
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 针对具体应用的差异化 Hook 接口
 */
interface IAppHook {
    /**
     * 该策略适用的包名列表
     */
    val targetPackages: List<String>

    /**
     * 执行定制化的 Hook 逻辑
     */
    fun hook(
        appLpparam: XC_LoadPackage.LoadPackageParam,
        context: Context
    )
}

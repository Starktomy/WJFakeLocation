// TelephonyHook.kt
package com.steadywj.wjfakelocation.xposed.hooks

import android.telephony.CellIdentityCdma
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityTdscdma
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.TelephonyManager
import com.steadywj.wjfakelocation.data.model.FakeCellInfo
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 基站信息 Hook
 *
 * 功能:
 * - 伪造基站列表
 * - 修改服务小区信息
 * - 支持 2G/3G/4G/5G 全制式
 */
class TelephonyHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook TelephonyManager.getAllCellInfo()
        hookGetAllCellInfo(lpparam)

        // Hook TelephonyManager.getCellLocation() (API 33+)
        hookGetCellLocation(lpparam)

        // Hook SignalStrength 相关方法
        hookSignalStrength(lpparam)
    }

    /**
     * Hook getAllCellInfo() - 获取所有基站信息
     */
    private fun hookGetAllCellInfo(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClass(TelephonyManager::class.java.name, lpparam.classLoader)

            XposedBridge.hookAllMethods(
                clazz,
                "getAllCellInfo",
                object : XC_MethodReplacement() {
                    @Throws(Throwable::class)
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        // 检查是否启用基站伪造
                        if (!isFakeCellEnabled()) {
                            return de.robv.android.xposed.XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }

                        try {
                            val fakeCells = createFakeCellInfoList()
                            XposedBridge.log("[TelephonyHook] 返回伪造基站列表：${fakeCells.size}个")
                            return fakeCells
                        } catch (e: Exception) {
                            XposedBridge.log("[TelephonyHook] 创建假基站失败：${e.message}")
                            return de.robv.android.xposed.XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                    }
                },
            )
        } catch (e: Throwable) {
            XposedBridge.log("[TelephonyHook] Hook getAllCellInfo 失败：${e.message}")
        }
    }

    /**
     * Hook getCellLocation() - 获取服务小区位置 (API 33+)
     */
    private fun hookGetCellLocation(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClass(TelephonyManager::class.java.name, lpparam.classLoader)

            XposedBridge.hookAllMethods(
                clazz,
                "getCellLocation",
                object : XC_MethodReplacement() {
                    @Throws(Throwable::class)
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        if (!isFakeCellEnabled()) {
                            return de.robv.android.xposed.XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }

                        try {
                            val fakeLocation = createFakeCellLocation()
                            XposedBridge.log("[TelephonyHook] 返回伪造服务小区位置")
                            return fakeLocation
                        } catch (e: Exception) {
                            XposedBridge.log("[TelephonyHook] 创建假服务小区失败：${e.message}")
                            return de.robv.android.xposed.XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                    }
                },
            )
        } catch (e: Throwable) {
            XposedBridge.log("[TelephonyHook] Hook getCellLocation 失败：${e.message}")
        }
    }

    /**
     * Hook SignalStrength - 信号强度
     */
    private fun hookSignalStrength(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val signalStrengthClass =
                XposedHelpers.findClass(
                    "android.telephony.SignalStrength",
                    lpparam.classLoader,
                )

            // Hook getLevel() - 信号级别
            XposedBridge.hookAllMethods(
                signalStrengthClass,
                "getLevel",
                object : XC_MethodReplacement() {
                    @Throws(Throwable::class)
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        if (!isFakeCellEnabled()) {
                            return de.robv.android.xposed.XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }

                        // 返回满格信号（4 格）
                        return 4
                    }
                },
            )

            // Hook getCdmaDbm, cdmaEcio 等具体方法
            hookSignalStrengthMethods(signalStrengthClass, lpparam)
        } catch (e: Throwable) {
            XposedBridge.log("[TelephonyHook] Hook SignalStrength 失败：${e.message}")
        }
    }

    /**
     * Hook 信号强度具体方法
     */
    private fun hookSignalStrengthMethods(
        clazz: Class<*>,
        lpparam: XC_LoadPackage.LoadPackageParam,
    ) {
        val methodsToHook =
            listOf(
                "getCdmaDbm",
                "getCdmaEcio",
                "getEvdoDbm",
                "getEvdoEcio",
                "getGsmSignalStrength",
                "getGsmBitErrorRate",
                "getLteSignalStrength",
                "getLteRsrp",
                "getLteRsrq",
                "getLteRssnr",
                "getLteCqi",
            )

        methodsToHook.forEach { methodName ->
            try {
                XposedBridge.hookAllMethods(
                    clazz,
                    methodName,
                    object : XC_MethodReplacement() {
                        @Throws(Throwable::class)
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            if (!isFakeCellEnabled()) {
                                return de.robv.android.xposed.XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                            }

                            // 返回良好的信号值
                            return when (methodName) {
                                "getCdmaDbm", "getEvdoDbm", "getLteRsrp" -> -75 // dBm，值越大越好
                                "getCdmaEcio", "getEvdoEcio" -> -5 // dB，值越大越好
                                "getGsmSignalStrength", "getLteSignalStrength" -> 31 // 最大值
                                "getGsmBitErrorRate" -> 0
                                "getLteRsrq" -> -5 // dB
                                "getLteRssnr" -> 20
                                "getLteCqi" -> 15 // 最大值
                                else -> de.robv.android.xposed.XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                            }
                        }
                    },
                )
            } catch (e: Throwable) {
                // 忽略不存在的方法
            }
        }
    }

    /**
     * 创建假基站列表
     */
    private fun createFakeCellInfoList(): List<CellInfo> {
        val fakeCells = mutableListOf<CellInfo>()

        // 从配置读取假基站信息
        val fakeCellInfos = FakeCellInfo.getFakeCells()

        fakeCellInfos.forEachIndexed { index, fakeInfo ->
            try {
                val cellInfo =
                    when (fakeInfo.type) {
                        com.steadywj.wjfakelocation.data.model.CellType.GSM -> createGsmCellInfo(fakeInfo, index == 0)
                        com.steadywj.wjfakelocation.data.model.CellType.CDMA -> createCdmaCellInfo(fakeInfo, index == 0)
                        com.steadywj.wjfakelocation.data.model.CellType.LTE -> createLteCellInfo(fakeInfo, index == 0)
                        com.steadywj.wjfakelocation.data.model.CellType.WCDMA -> createWcdmaCellInfo(fakeInfo, index == 0)
                        com.steadywj.wjfakelocation.data.model.CellType.TDSCDMA -> createTdscdmaCellInfo(fakeInfo, index == 0)
                        com.steadywj.wjfakelocation.data.model.CellType.NR -> createNrCellInfo(fakeInfo, index == 0)
                    }
                fakeCells.add(cellInfo)
            } catch (e: Exception) {
                XposedBridge.log("[TelephonyHook] 创建${fakeInfo.type}基站失败：${e.message}")
            }
        }

        return fakeCells
    }

    /**
     * 创建假的服务小区位置
     */
    private fun createFakeCellLocation(): Any? {
        val fakeInfo = FakeCellInfo.getPrimaryCell()

        return when (fakeInfo.type) {
            com.steadywj.wjfakelocation.data.model.CellType.GSM -> createGsmCellLocation(fakeInfo)
            com.steadywj.wjfakelocation.data.model.CellType.CDMA -> createCdmaCellLocation(fakeInfo)
            com.steadywj.wjfakelocation.data.model.CellType.LTE -> createLteCellLocation(fakeInfo)
            com.steadywj.wjfakelocation.data.model.CellType.WCDMA -> createWcdmaCellLocation(fakeInfo)
            com.steadywj.wjfakelocation.data.model.CellType.TDSCDMA -> createTdscdmaCellLocation(fakeInfo)
            com.steadywj.wjfakelocation.data.model.CellType.NR -> createNrCellLocation(fakeInfo)
        }
    }

    // ==================== 创建各类型基站信息 ====================

    private fun createGsmCellInfo(
        fakeInfo: FakeCellInfo,
        isRegistered: Boolean,
    ): CellInfo {
        val cellInfoGsm =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellInfoGsm::class.java
            ) as android.telephony.CellInfoGsm
        XposedHelpers.callMethod(cellInfoGsm, "setRegistered", isRegistered)

        val cellIdentity =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellIdentityGsm::class.java,
                fakeInfo.mcc?.toIntOrNull() ?: 460,
                fakeInfo.mnc?.toIntOrNull() ?: 0,
                fakeInfo.cid ?: 0,
                fakeInfo.lac ?: 0,
                null,
            )

        XposedHelpers.callMethod(cellInfoGsm, "setCellIdentity", cellIdentity)

        return cellInfoGsm
    }

    private fun createCdmaCellInfo(
        fakeInfo: FakeCellInfo,
        isRegistered: Boolean,
    ): CellInfo {
        val cellInfoCdma =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellInfoCdma::class.java
            ) as android.telephony.CellInfoCdma
        XposedHelpers.callMethod(cellInfoCdma, "setRegistered", isRegistered)

        val cellIdentity =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellIdentityCdma::class.java,
                fakeInfo.basestationId ?: 0,
                fakeInfo.longitude ?: 0.0,
                fakeInfo.latitude ?: 0.0,
                null,
            )

        XposedHelpers.callMethod(cellInfoCdma, "setCellIdentity", cellIdentity)

        return cellInfoCdma
    }

    private fun createLteCellInfo(
        fakeInfo: FakeCellInfo,
        isRegistered: Boolean,
    ): CellInfo {
        val cellInfoLte =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellInfoLte::class.java
            ) as android.telephony.CellInfoLte
        XposedHelpers.callMethod(cellInfoLte, "setRegistered", isRegistered)

        val cellIdentity =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellIdentityLte::class.java,
                fakeInfo.mcc?.toIntOrNull() ?: 460,
                fakeInfo.mnc?.toIntOrNull() ?: 0,
                fakeInfo.ci ?: fakeInfo.cid ?: 0,
                fakeInfo.tac ?: fakeInfo.lac ?: 0,
                0,
                null,
            )

        XposedHelpers.callMethod(cellInfoLte, "setCellIdentity", cellIdentity)

        return cellInfoLte
    }

    private fun createWcdmaCellInfo(
        fakeInfo: FakeCellInfo,
        isRegistered: Boolean,
    ): CellInfo {
        val cellInfoWcdma =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellInfoWcdma::class.java
            ) as android.telephony.CellInfoWcdma
        XposedHelpers.callMethod(cellInfoWcdma, "setRegistered", isRegistered)

        val cellIdentity =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellIdentityWcdma::class.java,
                fakeInfo.mcc?.toIntOrNull() ?: 460,
                fakeInfo.mnc?.toIntOrNull() ?: 0,
                fakeInfo.cid ?: 0,
                fakeInfo.lac ?: 0,
                fakeInfo.psc ?: 0,
                null,
            )

        XposedHelpers.callMethod(cellInfoWcdma, "setCellIdentity", cellIdentity)

        return cellInfoWcdma
    }

    private fun createTdscdmaCellInfo(
        fakeInfo: FakeCellInfo,
        isRegistered: Boolean,
    ): CellInfo {
        val cellInfoTdscdma =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellInfoTdscdma::class.java
            ) as android.telephony.CellInfoTdscdma
        XposedHelpers.callMethod(cellInfoTdscdma, "setRegistered", isRegistered)

        val cellIdentity =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellIdentityTdscdma::class.java,
                fakeInfo.mcc?.toIntOrNull() ?: 460,
                fakeInfo.mnc?.toIntOrNull() ?: 0,
                fakeInfo.cid ?: 0,
                fakeInfo.lac ?: 0,
                fakeInfo.cpid ?: 0,
                null,
            )

        XposedHelpers.callMethod(cellInfoTdscdma, "setCellIdentity", cellIdentity)

        return cellInfoTdscdma
    }

    private fun createNrCellInfo(
        fakeInfo: FakeCellInfo,
        isRegistered: Boolean,
    ): CellInfo {
        val cellInfoNr =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellIdentityNr::class.java
            ) as android.telephony.CellInfo
        XposedHelpers.callMethod(cellInfoNr, "setRegistered", isRegistered)

        val cellIdentity =
            de.robv.android.xposed.XposedHelpers.newInstance(
                android.telephony.CellIdentityNr::class.java,
                fakeInfo.mcc?.toIntOrNull() ?: 460,
                fakeInfo.mnc?.toIntOrNull() ?: 0,
                fakeInfo.nci ?: fakeInfo.cid ?: 0,
                fakeInfo.tac ?: fakeInfo.lac ?: 0,
                null,
            )

        XposedHelpers.callMethod(cellInfoNr, "setCellIdentity", cellIdentity)

        return cellInfoNr
    }

    // ==================== CellLocation 创建 ====================

    private fun createGsmCellLocation(fakeInfo: FakeCellInfo): Any {
        return XposedHelpers.newInstance(
            XposedHelpers.findClass("android.telephony.gsm.GsmCellLocation", null),
            fakeInfo.lac ?: 0,
            fakeInfo.cid ?: 0,
        )
    }

    private fun createLteCellLocation(fakeInfo: FakeCellInfo): Any {
        // API 33+ 使用 CellLocation.createCdma(...)
        val cellLocationClass = XposedHelpers.findClass("android.telephony.CellLocation", null)
        return XposedHelpers.callStaticMethod(
            cellLocationClass,
            "createCdma",
            fakeInfo.basestationId ?: 0,
            fakeInfo.longitude ?: 0.0,
            fakeInfo.latitude ?: 0.0,
            0,
        )
    }

    // ... 其他类型类似

    // ==================== 工具方法 ====================

    private fun isFakeCellEnabled(): Boolean {
        // 从 Preferences 读取开关状态
        // TODO: 注入 PreferencesRepository 或使用静态方法读取
        return true // 默认启用
    }

    private enum class CellType {
        GSM,
        CDMA,
        LTE,
        WCDMA,
        TDSCDMA,
        NR,
    }
}

private fun createCdmaCellLocation(fakeInfo: com.steadywj.wjfakelocation.data.model.FakeCellInfo): android.telephony.CellLocation? {
    return null
}

private fun createWcdmaCellLocation(fakeInfo: com.steadywj.wjfakelocation.data.model.FakeCellInfo): android.telephony.CellLocation? {
    return null
}

private fun createTdscdmaCellLocation(fakeInfo: com.steadywj.wjfakelocation.data.model.FakeCellInfo): android.telephony.CellLocation? {
    return null
}

private fun createNrCellLocation(fakeInfo: com.steadywj.wjfakelocation.data.model.FakeCellInfo): android.telephony.CellLocation? {
    return null
}

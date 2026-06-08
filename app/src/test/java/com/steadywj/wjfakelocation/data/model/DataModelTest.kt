package com.steadywj.wjfakelocation.domain.service

import com.steadywj.wjfakelocation.data.model.FakeCellInfo
import com.steadywj.wjfakelocation.data.model.FakeWifiInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 数据模型测试
 */
class DataModelTest {
    @Test
    fun `FakeCellInfo should create valid LTE cell info`() {
        // Given
        val cellInfo =
            FakeCellInfo(
                name = "Test Cell",
                type = com.steadywj.wjfakelocation.data.model.CellType.LTE,
                mcc = "460",
                mnc = "0",
                lac = 12345,
                cid = 67890,
                enabled = true,
            )

        // When
        val cells = FakeCellInfo.getFakeCells()

        // Then
        assertTrue("应该有预设基站", cells.isNotEmpty())
        assertEquals(
            "第一个基站应该是 LTE",
            com.steadywj.wjfakelocation.data.model.CellType.LTE,
            cells.first().type,
        )
    }

    @Test
    fun `FakeWifiInfo should create valid WiFi info`() {
        // Given
        val wifiInfo =
            FakeWifiInfo(
                ssid = "TestWiFi",
                securityType = com.steadywj.wjfakelocation.data.model.SecurityType.WPA2_PSK,
                band = com.steadywj.wjfakelocation.data.model.WifiBand.GHZ_5,
                signalStrength = -50,
                enabled = true,
            )

        // Then
        assertEquals("SSID 应该匹配", "TestWiFi", wifiInfo.ssid)
        assertEquals(
            "加密类型应该匹配",
            com.steadywj.wjfakelocation.data.model.SecurityType.WPA2_PSK,
            wifiInfo.securityType,
        )
        assertEquals(
            "频段应该匹配",
            com.steadywj.wjfakelocation.data.model.WifiBand.GHZ_5,
            wifiInfo.band,
        )
        assertEquals("信号强度应该匹配", -50, wifiInfo.signalStrength)
    }

    @Test
    fun `SecurityType enum should have all types`() {
        // When
        val securityTypes = com.steadywj.wjfakelocation.data.model.SecurityType.values()

        // Then
        assertTrue("应该包含 WPA2", securityTypes.any { it.name == "WPA2_PSK" })
        assertTrue("应该包含 WPA3", securityTypes.any { it.name == "WPA3_SAE" })
        assertEquals("总共应该有 6 种加密类型", 6, securityTypes.size)
    }
}

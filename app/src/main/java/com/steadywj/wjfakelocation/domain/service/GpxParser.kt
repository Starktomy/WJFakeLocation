// GpxParser.kt
package com.steadywj.wjfakelocation.domain.service

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GPX 文件解析器
 *
 * GPX (GPS Exchange Format) 是一种用于存储 GPS 数据的 XML 格式
 *
 * 示例结构:
 * <?xml version="1.0" encoding="UTF-8"?>
 * <gpx version="1.1">
 *   <trk>
 *     <name>我的轨迹</name>
 *     <trkseg>
 *       <trkpt lat="39.908823" lon="116.397470">
 *         <ele>50.0</ele>
 *         <time>2024-01-01T10:00:00Z</time>
 *       </trkpt>
 *       <!-- 更多轨迹点 -->
 *     </trkseg>
 *   </trk>
 * </gpx>
 */
@Singleton
class GpxParser
    @Inject
    constructor() {
        /**
         * 解析 GPX 文件
         * @param inputStream GPX 文件输入流
         * @return 轨迹段列表
         */
        fun parse(inputStream: InputStream): List<TrackSegment> {
            val segments = mutableListOf<TrackSegment>()
            var currentSegment: TrackSegment? = null
            var currentTrackName: String? = null

            try {
                val parserFactory = XmlPullParserFactory.newInstance()
                parserFactory.isNamespaceAware = false
                val parser = parserFactory.newPullParser()

                parser.setInput(inputStream, "UTF-8")

                var eventType = parser.eventType

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "trk" -> {
                                    currentTrackName = parser.getAttributeValue(null, "name")
                                }
                                "trkseg" -> {
                                    currentSegment = TrackSegment(name = currentTrackName)
                                }
                                "trkpt" -> {
                                    val lat = parser.getAttributeValue(null, "lat").toDoubleOrNull() ?: 0.0
                                    val lon = parser.getAttributeValue(null, "lon").toDoubleOrNull() ?: 0.0

                                    val trackPoint =
                                        TrackPoint(
                                            latitude = lat,
                                            longitude = lon,
                                            elevation = 0.0,
                                            time = System.currentTimeMillis(),
                                        )

                                    currentSegment?.addPoint(trackPoint)
                                }
                                "ele" -> {
                                    parser.nextText().toDoubleOrNull()?.let { elevation ->
                                        currentSegment?.lastPoint()?.copy(elevation = elevation)
                                    }
                                }
                                "time" -> {
                                    val timeString = parser.nextText()
                                    // 解析 ISO 8601 时间格式
                                    val timestamp = parseISO8601(timeString)
                                    currentSegment?.lastPoint()?.copy(time = timestamp)
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "trkseg" && currentSegment != null) {
                                segments.add(currentSegment)
                                currentSegment = null
                            }
                        }
                    }

                    eventType = parser.next()
                }
            } catch (e: Exception) {
                throw GpxParseException("GPX 解析失败：${e.message}", e)
            }

            return segments
        }

        /**
         * 解析 ISO 8601 时间字符串
         */
        private fun parseISO8601(timeString: String): Long {
            return try {
                // 简单处理：2024-01-01T10:00:00Z
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                format.parse(timeString)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
    }

/**
 * GPX 解析异常
 */
class GpxParseException(message: String, cause: Throwable?) : Exception(message, cause)

/**
 * 轨迹段
 */
data class TrackSegment(
    val name: String? = null,
    val points: MutableList<TrackPoint> = mutableListOf(),
) {
    fun addPoint(point: TrackPoint) {
        points.add(point)
    }

    fun lastPoint(): TrackPoint? {
        return points.lastOrNull()
    }

    /**
     * 计算总距离（米）
     */
    fun getTotalDistance(): Double {
        var totalDistance = 0.0

        for (i in 1 until points.size) {
            totalDistance +=
                calculateDistance(
                    points[i - 1].latitude,
                    points[i - 1].longitude,
                    points[i].latitude,
                    points[i].longitude,
                )
        }

        return totalDistance
    }

    /**
     * 预计行驶时间（秒）
     * @param speedMps 速度（米/秒）
     */
    fun getEstimatedDuration(speedMps: Float): Long {
        return (getTotalDistance() / speedMps).toLong()
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
        val earthRadius = 6371000.0 // 米
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

/**
 * 轨迹点
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double = 0.0,
    val time: Long = System.currentTimeMillis(),
)

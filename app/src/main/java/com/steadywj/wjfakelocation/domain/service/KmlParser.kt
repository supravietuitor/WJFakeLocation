// KmlParser.kt
package com.steadywj.wjfakelocation.domain.service

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KML 文件解析器
 * 
 * KML (Keyhole Markup Language) 是一种基于 XML 的地理空间数据格式
 * 常用于 Google Earth 和 Google Maps
 * 
 * 示例结构:
 * <?xml version="1.0" encoding="UTF-8"?>
 * <kml xmlns="http://www.opengis.net/kml/2.2">
 *   <Document>
 *     <Placemark>
 *       <name>北京天安门</name>
 *       <Point>
 *         <coordinates>116.397470,39.908823,0</coordinates>
 *       </Point>
 *     </Placemark>
 *     <Placemark>
 *       <name>路线</name>
 *       <LineString>
 *         <coordinates>
 *           116.397470,39.908823,0
 *           116.398000,39.909000,0
 *         </coordinates>
 *       </LineString>
 *     </Placemark>
 *   </Document>
 * </kml>
 */
@Singleton
class KmlParser @Inject constructor() {
    
    /**
     * 解析 KML 文件
     * @param inputStream KML 文件输入流
     * @return KML 文档对象
     */
    fun parse(inputStream: InputStream): KmlDocument {
        val document = KmlDocument()
        var currentPlacemark: Placemark? = null
        var currentCoordinates = mutableListOf<TrackPoint>()
        var inCoordinates = false
        
        try {
            val parserFactory = XmlPullParserFactory.newInstance()
            parserFactory.isNamespaceAware = true // KML 有命名空间
            val parser = parserFactory.newPullParser()
            
            parser.setInput(inputStream, "UTF-8")
            
            var eventType = parser.eventType
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "Placemark" -> {
                                currentPlacemark = Placemark()
                            }
                            "name" -> {
                                val name = parser.nextText()
                                if (currentPlacemark != null) {
                                    currentPlacemark.name = name
                                } else if (document.name == null) {
                                    document.name = name
                                }
                            }
                            "description" -> {
                                currentPlacemark?.description = parser.nextText()
                            }
                            "coordinates" -> {
                                inCoordinates = true
                                currentCoordinates.clear()
                            }
                            "Point", "LineString", "Polygon" -> {
                                // 几何类型
                                currentPlacemark?.geometryType = parser.name
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inCoordinates && parser.text.isNotBlank()) {
                            // 解析坐标字符串
                            val coordinatePairs = parser.text.trim().split("\\s+".toRegex())
                            
                            coordinatePairs.forEach { coord ->
                                val parts = coord.split(",")
                                if (parts.size >= 2) {
                                    try {
                                        val lon = parts[0].toDoubleOrNull()
                                        val lat = parts[1].toDoubleOrNull()
                                        
                                        if (lon != null && lat != null) {
                                            currentCoordinates.add(
                                                TrackPoint(
                                                    latitude = lat,
                                                    longitude = lon,
                                                    elevation = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                                                )
                                            )
                                        }
                                    } catch (e: Exception) {
                                        // 忽略解析失败的坐标
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "coordinates" -> {
                                inCoordinates = false
                                currentPlacemark?.coordinates = currentCoordinates.toList()
                            }
                            "Placemark" -> {
                                if (currentPlacemark != null) {
                                    document.placemarks.add(currentPlacemark!!)
                                    currentPlacemark = null
                                }
                            }
                        }
                    }
                }
                
                eventType = parser.next()
            }
        } catch (e: Exception) {
            throw KmlParseException("KML 解析失败：${e.message}", e)
        }
        
        return document
    }
    
    /**
     * 将 KML 转换为 GPX（便于路径播放）
     */
    fun convertToGpx(kmlDocument: KmlDocument): List<TrackSegment> {
        val segments = mutableListOf<TrackSegment>()
        
        kmlDocument.placemarks.forEach { placemark ->
            if (placemark.coordinates.isNotEmpty()) {
                val segment = TrackSegment(name = placemark.name)
                segment.points.addAll(placemark.coordinates)
                segments.add(segment)
            }
        }
        
        return segments
    }
}

/**
 * KML 解析异常
 */
class KmlParseException(message: String, cause: Throwable?) : Exception(message, cause)

/**
 * KML 文档
 */
data class KmlDocument(
    var name: String? = null,
    val placemarks: MutableList<Placemark> = mutableListOf()
) {
    /**
     * 获取所有轨迹点
     */
    fun getAllPoints(): List<TrackPoint> {
        return placemarks.flatMap { it.coordinates }
    }
    
    /**
     * 计算总距离
     */
    fun getTotalDistance(): Double {
        return getAllPoints().let { points ->
            var totalDistance = 0.0
            for (i in 1 until points.size) {
                totalDistance += calculateDistance(
                    points[i - 1].latitude,
                    points[i - 1].longitude,
                    points[i].latitude,
                    points[i].longitude
                )
            }
            totalDistance
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
}

/**
 * KML 地标
 */
data class Placemark(
    var name: String? = null,
    var description: String? = null,
    var geometryType: String = "Point",
    var coordinates: List<TrackPoint> = emptyList()
) {
    /**
     * 是否为点
     */
    fun isPoint(): Boolean = geometryType == "Point"
    
    /**
     * 是否为线（路径）
     */
    fun isLine(): Boolean = geometryType == "LineString"
    
    /**
     * 是否为多边形
     */
    fun isPolygon(): Boolean = geometryType == "Polygon"
}

// BaiduMapManager.kt
package com.steadywj.wjfakelocation.manager.map.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaiduMapManager @Inject constructor(
    private val context: Context
) {

    fun bd09ToWgs84(bdLat: Double, bdLng: Double): Pair<Double, Double> {
        val (gcjLat, gcjLng) = bd09ToGcj02(bdLat, bdLng)
        return gcj02ToWgs84(gcjLat, gcjLng)
    }

    fun wgs84ToBd09(wgsLat: Double, wgsLng: Double): Pair<Double, Double> {
        val (gcjLat, gcjLng) = wgs84ToGcj02(wgsLat, wgsLng)
        return gcj02ToBd09(gcjLat, gcjLng)
    }

    private fun bd09ToGcj02(bdLat: Double, bdLng: Double): Pair<Double, Double> {
        val xPi = Math.PI * 3000.0 / 180.0
        val x = bdLng - 0.0065
        val y = bdLat - 0.006
        val z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * xPi)
        val theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * xPi)

        val gcjLng = z * Math.cos(theta)
        val gcjLat = z * Math.sin(theta)

        return gcjLat to gcjLng
    }

    private fun gcj02ToBd09(gcjLat: Double, gcjLng: Double): Pair<Double, Double> {
        val xPi = Math.PI * 3000.0 / 180.0
        val z = Math.sqrt(gcjLng * gcjLng + gcjLat * gcjLat) + 0.00002 * Math.sin(gcjLat * xPi)
        val theta = Math.atan2(gcjLat, gcjLng) + 0.000003 * Math.cos(gcjLng * xPi)

        val bdLng = z * Math.cos(theta) + 0.0065
        val bdLat = z * Math.sin(theta) + 0.006

        return bdLat to bdLng
    }

    private fun gcj02ToWgs84(gcjLat: Double, gcjLng: Double): Pair<Double, Double> {
        val ee = 0.00669342162296594323
        val a = 6378245.0

        val dLat = transformLat(gcjLng - 105.0, gcjLat - 35.0)
        val dLng = transformLng(gcjLng - 105.0, gcjLat - 35.0)

        val radLat = gcjLat / 180.0 * Math.PI
        var magic = Math.sin(radLat)
        magic = 1 - ee * magic * magic

        val sqrtMagic = Math.sqrt(magic)

        var wgsLat = gcjLat - dLat
        var wgsLng = gcjLng - dLng

        wgsLat = wgsLat * 180.0 / Math.PI
        wgsLng = wgsLng * 180.0 / Math.PI

        wgsLat = (2 * wgsLat - gcjLat)
        wgsLng = (2 * wgsLng - gcjLng)

        wgsLat -= (dLat / sqrtMagic) * 180.0 / Math.PI * a * (1 - ee) / (magic * RADIUS_EARTH)
        wgsLng -= (dLng / sqrtMagic) * 180.0 / Math.PI * a * (1 - ee) / (magic * RADIUS_EARTH * Math.cos(radLat))

        return Pair(wgsLat, wgsLng)
    }

    private fun wgs84ToGcj02(wgsLat: Double, wgsLng: Double): Pair<Double, Double> {
        val ee = 0.00669342162296594323
        val a = 6378245.0

        val dLat = transformLat(wgsLng - 105.0, wgsLat - 35.0)
        val dLng = transformLng(wgsLng - 105.0, wgsLat - 35.0)

        val radLat = wgsLat / 180.0 * Math.PI
        var magic = Math.sin(radLat)
        magic = 1 - ee * magic * magic

        val sqrtMagic = Math.sqrt(magic)

        var gcjLat = wgsLat + dLat
        var gcjLng = wgsLng + dLng

        gcjLat = gcjLat * 180.0 / Math.PI
        gcjLng = gcjLng * 180.0 / Math.PI

        gcjLat += (dLat / sqrtMagic) * 180.0 / Math.PI * a * (1 - ee) / (magic * RADIUS_EARTH)
        gcjLng += (dLng / sqrtMagic) * 180.0 / Math.PI * a * (1 - ee) / (magic * RADIUS_EARTH * Math.cos(radLat))

        return Pair(gcjLat, gcjLng)
    }

    private fun transformLat(lng: Double, lat: Double): Double {
        var ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
        ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(lng: Double, lat: Double): Double {
        var ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
        ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0
        return ret
    }

    fun geocodeAddress(address: String): Flow<Result<Pair<Double, Double>>> = callbackFlow {
        Log.w("BaiduMapManager", "Geocode not available - Baidu search module not included in JAR")
        trySend(Result.failure(Exception("Baidu geocode not available: search module not included")))
        awaitClose {}
    }

    fun reverseGeocode(latitude: Double, longitude: Double): Flow<Result<String>> = callbackFlow {
        Log.w("BaiduMapManager", "Reverse geocode not available - Baidu search module not included in JAR")
        trySend(Result.failure(Exception("Baidu reverse geocode not available: search module not included")))
        awaitClose {}
    }

    companion object {
        private const val RADIUS_EARTH = 6378137.0
    }
}

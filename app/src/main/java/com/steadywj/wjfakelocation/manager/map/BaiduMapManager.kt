// BaiduMapManager.kt
package com.steadywj.wjfakelocation.manager.map.utils

import android.content.Context
import com.baidu.mapapi.search.geocode.GeoCodeResult
import com.baidu.mapapi.search.geocode.GeoCoder
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ç™¾åº¦åœ°å›¾ç®¡ç†å™?
 * 
 * åŠŸèƒ½:
 * - BD-09 â†?WGS-84 åæ ‡è½¬æ¢
 * - åœ°ç†ç¼–ç æœç´¢
 * - é€†åœ°ç†ç¼–ç ?
 */
@Singleton
class BaiduMapManager @Inject constructor(
    private val context: Context
) {
    
    /**
     * BD-09 è½?WGS-84
     * ç™¾åº¦åæ ‡ç³?â†?ç«æ˜Ÿåæ ‡ç³?â†?GPS åæ ‡ç³?
     */
    fun bd09ToWgs84(bdLat: Double, bdLng: Double): Pair<Double, Double> {
        // BD-09 â†?GCJ-02
        val (gcjLat, gcjLng) = bd09ToGcj02(bdLat, bdLng)
        
        // GCJ-02 â†?WGS-84
        return gcj02ToWgs84(gcjLat, gcjLng)
    }
    
    /**
     * WGS-84 è½?BD-09
     * GPS åæ ‡ç³?â†?ç«æ˜Ÿåæ ‡ç³?â†?ç™¾åº¦åæ ‡ç³?
     */
    fun wgs84ToBd09(wgsLat: Double, wgsLng: Double): Pair<Double, Double> {
        // WGS-84 â†?GCJ-02
        val (gcjLat, gcjLng) = wgs84ToGcj02(wgsLat, wgsLng)
        
        // GCJ-02 â†?BD-09
        return gcj02ToBd09(gcjLat, gcjLng)
    }
    
    /**
     * BD-09 è½?GCJ-02
     */
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
    
    /**
     * GCJ-02 è½?BD-09
     */
    private fun gcj02ToBd09(gcjLat: Double, gcjLng: Double): Pair<Double, Double> {
        val xPi = Math.PI * 3000.0 / 180.0
        val z = Math.sqrt(gcjLng * gcjLng + gcjLat * gcjLat) + 0.00002 * Math.sin(gcjLat * xPi)
        val theta = Math.atan2(gcjLat, gcjLng) + 0.000003 * Math.cos(gcjLng * xPi)
        
        val bdLng = z * Math.cos(theta) + 0.0065
        val bdLat = z * Math.sin(theta) + 0.006
        
        return bdLat to bdLng
    }
    
    /**
     * GCJ-02 è½?WGS-84
     */
    private fun gcj02ToWgs84(gcjLat: Double, gcjLng: Double): Pair<Double, Double> {
        return com.steadywj.wjfakelocation.xposed.common.LocationUtil.gcj02ToWgs84(gcjLat, gcjLng)
    }
    
    /**
     * WGS-84 è½?GCJ-02
     */
    private fun wgs84ToGcj02(wgsLat: Double, wgsLng: Double): Pair<Double, Double> {
        return com.steadywj.wjfakelocation.xposed.common.LocationUtil.wgs84ToGcj02(wgsLat, wgsLng)
    }
    
    /**
     * åœ°ç†ç¼–ç æœç´¢ï¼ˆåœ°å€è½¬åæ ‡ï¼‰
     */
    fun geocodeAddress(address: String): Flow<Result<Pair<Double, Double>>> = callbackFlow {
        try {
            val geoCoder = GeoCoder.newInstance()
            
            geoCoder.setOnGetGeoCodeResultListener(object : OnGetGeoCoderResultListener {
                override fun onGetGeoCodeResult(result: GeoCodeResult?) {
                    if (result != null && result.error == 0) {
                        val latLng = result.location
                        trySend(Result.success(latLng.latitude to latLng.longitude))
                    } else {
                        trySend(Result.failure(Exception("åœ°ç†ç¼–ç å¤±è´¥")))
                    }
                    close()
                }
                
                override fun onGetReverseGeoCodeResult(result: Any?) {
                    // ä¸ä½¿ç”¨é€†åœ°ç†ç¼–ç ?
                }
            })
            
            // æ‰§è¡Œåœ°ç†ç¼–ç 
            geoCoder.geoCodeLocation(address, "å…¨å›½")
        } catch (e: Exception) {
            trySend(Result.failure(e))
            close()
        }
        
        awaitClose {}
    }
    
    /**
     * é€†åœ°ç†ç¼–ç ï¼ˆåæ ‡è½¬åœ°å€ï¼?
     */
    fun reverseGeocode(latitude: Double, longitude: Double): Flow<Result<String>> = callbackFlow {
        try {
            val geoCoder = GeoCoder.newInstance()
            
            geoCoder.setOnGetReverseGeoCodeResultListener(object : OnGetGeoCoderResultListener {
                override fun onGetReverseGeoCodeResult(result: Any?) {
                    // é€‚é…ç™¾åº¦åœ°å›¾çš„é€†åœ°ç†ç¼–ç ç»“æž?
                    try {
                        if (result != null) {
                            // ä½¿ç”¨åå°„èŽ·å– address å­—æ®µ
                            val addressField = result.javaClass.getDeclaredMethod("getAddress")
                            val address = addressField.invoke(result) as? String
                            
                            if (!address.isNullOrBlank()) {
                                trySend(Result.success(address))
                            } else {
                                trySend(Result.failure(Exception("æ— æ³•è§£æžåœ°å€")))
                            }
                        } else {
                            trySend(Result.failure(Exception("Done"))
                        }
                    } catch (e: Exception) {
                        trySend(Result.failure(e))
                    } finally {
                        close()
                    }
                }
                
                override fun onGetGeoCodeResult(result: GeoCodeResult?) {
                    // ä¸ä½¿ç”?
                }
            })
            
            val location = com.baidu.mapapi.model.LatLng(latitude, longitude)
            geoCoder.reverseGeoCode(ReverseGeoCodeOption().location(location))
        } catch (e: Exception) {
            trySend(Result.failure(e))
            close()
        }
        
        awaitClose {}
    }
}

// AMapManager.kt
package com.steadywj.wjfakelocation.manager.map.utils

import android.content.Context
import com.amap.api.maps.MapView
import com.amap.api.services.core.AMapException
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 擃噸�啣蝞∠��?
 * 韐提�啣����蝝Ｗ���頧祆
 */
@Singleton
class AMapManager @Inject constructor() {

    /**
     * ����?SDK
     * @param context 摨銝��?
     */
    fun initialize(context: Context) {
        // 擃噸�啣 SDK 隡�其� Manifest 霂餃� API Key
        // ������?
    }

    /**
     * �啁�蝻��揣嚗�頧砍���
     * @param address �啣�摮泵銝?
     * @return Flow<LatLng> ��瘚?
     */
    fun geocodeAddress(address: String): Flow<Result<LatLng>> = callbackFlow {
        val geocodeSearch = GeocodeSearch(null)
        
        geocodeSearch.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
            override fun onGeocodeSearched(result: GeocodeResult?, errorCode: Int) {
                if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null) {
                    val geocodeAddress = result.geocodeAddressList.firstOrNull()
                    if (geocodeAddress != null) {
                        trySend(
                            Result.success(
                                LatLng(
                                    latitude = geocodeAddress.latLonPoint.latitude,
                                    longitude = geocodeAddress.latLonPoint.longitude,
                                    address = address
                                )
                            )
                        )
                    } else {
                        trySend(Result.failure(Exception("�芣�啣���啣�")))
                    }
                } else {
                    trySend(Result.failure(Exception("�揣憭梯揖嚗?errorCode")))
                }
            }

            override fun onRegeocodeSearched(result: RegeocodeResult?, errorCode: Int) {
                // ������閬?
            }
        })

        val query = GeocodeQuery(address, "010") // ����舫?
        geocodeSearch.getFromLocationNameAsyn(query)

        awaitClose {
            // 皜�韏�
        }
    }

    /**
     * GCJ-02 頧?WGS-84
     * 擃噸�啣餈�� GCJ-02 ��蝟鳴��閬蓮�Ｖ蛹 WGS-84 �其�摰�隡芷?
     * @param gcjLat GCJ-02 蝥砍漲
     * @param gcjLng GCJ-02 蝏漲
     * @return WGS-84 �� [蝥砍漲嚗�摨因
     */
    fun gcj02ToWgs84(gcjLat: Double, gcjLng: Double): Pair<Double, Double> {
        return com.steadywj.wjfakelocation.xposed.common.LocationUtil.gcj02ToWgs84(gcjLat, gcjLng)
    }

    /**
     * WGS-84 頧?GCJ-02
     * 撠�琿�?WGS-84 ��頧祆銝?GCJ-02 �其��啣�曄內
     * @param wgsLat WGS-84 蝥砍漲
     * @param wgsLng WGS-84 蝏漲
     * @return GCJ-02 �� [蝥砍漲嚗�摨因
     */
    fun wgs84ToGcj02(wgsLat: Double, wgsLng: Double): Pair<Double, Double> {
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
    
    companion object {
        private const val RADIUS_EARTH = 6378137.0
        private const val PI = 3.14159265359
    }
}

/**
 * 蝏漪摨行�桃掩
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)

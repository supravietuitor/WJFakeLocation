// PoiSearchManager.kt
package com.steadywj.wjfakelocation.manager.search.utils

import android.content.Context
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.poi.PoiItem
import com.amap.api.services.poi.PoiSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * POI æœç´¢ç®¡ç†å™?
 * 
 * åŠŸèƒ½:
 * - å‘¨è¾¹æœç´¢ï¼ˆç¾Žé£Ÿã€é…’åº—ç­‰ï¼?
 * - POI åˆ†ç±»ç­›é€?
 * - å…³é”®è¯æœç´?
 */
@Singleton
class PoiSearchManager @Inject constructor(
    private val context: Context
) {
    
    /**
     * å‘¨è¾¹æœç´¢
     * @param latitude çº¬åº¦
     * @param longitude ç»åº¦
     * @param radius åŠå¾„ï¼ˆç±³ï¼‰ï¼Œé»˜è®¤ 3000 ç±?
     * @param type POI ç±»åž‹ï¼ˆé¤é¥®ã€é…’åº—ç­‰ï¼?
     * @return POI åˆ—è¡¨
     */
    fun searchNearby(
        latitude: Double,
        longitude: Double,
        radius: Int = 3000,
        type: PoiType = PoiType.FOOD
    ): Flow<List<PoiResult>> = callbackFlow {
        withContext(Dispatchers.IO) {
            try {
                val poiSearch = PoiSearch(context, "")
                val query = PoiSearch.Query("", type.code, "åŒ—äº¬å¸?) // åŸŽå¸‚
                
                // è®¾ç½®èŒƒå›´
                query.setPageSize(20) // æ¯é¡µ 20 æ?
                query.setPageNum(1) // ç¬?1 é¡?
                
                poiSearch.query = query
                
                // è®¾ç½®å‘¨è¾¹æœç´¢ä¸­å¿ƒç‚?
                poiSearch.setBound(LatLonPoint(latitude, longitude), radius)
                
                poiSearch.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiSearch.Result?, errorCode: Int) {
                        if (errorCode == 0 && result != null) {
                            val pois = result.pois.mapNotNull { poiItem ->
                                poiItem.toPoiResult()
                            }
                            trySend(pois)
                        } else {
                            trySend(emptyList())
                        }
                    }
                    
                    override fun onPoiItemSearched(poiItem: PoiItem?, errorCode: Int) {
                        // å•ä¸ª POI æœç´¢ç»“æžœï¼ˆä¸ä½¿ç”¨ï¼?
                    }
                })
                
                // æ‰§è¡Œæœç´¢
                poiSearch.searchPOIAsyn()
            } catch (e: Exception) {
                trySend(emptyList())
            }
        }
        
        awaitClose {}
    }
    
    /**
     * å…³é”®è¯æœç´?
     * @param keyword å…³é”®è¯?
     * @param city åŸŽå¸‚
     * @return POI åˆ—è¡¨
     */
    fun searchByKeyword(
        keyword: String,
        city: String = "å…¨å›½"
    ): Flow<List<PoiResult>> = callbackFlow {
        withContext(Dispatchers.IO) {
            try {
                val poiSearch = PoiSearch(context, keyword)
                val query = PoiSearch.Query(keyword, "", city)
                
                query.setPageSize(20)
                query.setPageNum(1)
                
                poiSearch.query = query
                
                poiSearch.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiSearch.Result?, errorCode: Int) {
                        if (errorCode == 0 && result != null) {
                            val pois = result.pois.mapNotNull { poiItem ->
                                poiItem.toPoiResult()
                            }
                            trySend(pois)
                        } else {
                            trySend(emptyList())
                        }
                    }
                    
                    override fun onPoiItemSearched(poiItem: PoiItem?, errorCode: Int) {
                        // å•ä¸ª POI æœç´¢ç»“æžœ
                    }
                })
                
                poiSearch.searchPOIAsyn()
            } catch (e: Exception) {
                trySend(emptyList())
            }
        }
        
        awaitClose {}
    }
    
    /**
     * èŽ·å–çƒ­é—¨æŽ¨è
     * @return çƒ­é—¨ POI ç±»åž‹åˆ—è¡¨
     */
    fun getPopularTypes(): List<PoiType> {
        return listOf(
            PoiType.FOOD,         // ç¾Žé£Ÿ
            PoiType.HOTEL,        // é…’åº—
            PoiType.SHOPPING,     // è´­ç‰©
            PoiType.TRANSPORT,    // äº¤é€?
            PoiType.ENTERTAINMENT // å¨±ä¹
        )
    }
}

/**
 * POI ç»“æžœ
 */
data class PoiResult(
    val id: String,
    val name: String,
    val type: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Float?, // è·ç¦»ï¼ˆç±³ï¼?
    val tel: String?, // ç”µè¯
    val rating: Float? // è¯„åˆ†
)

/**
 * POI ç±»åž‹æžšä¸¾ï¼ˆé«˜å¾·åœ°å›?POI ç¼–ç ï¼?
 */
enum class PoiType(val code: String, val displayName: String) {
    FOOD("food", "Food"),
    HOTEL("hotel", "Hotel"),
    SHOPPING("shopping", "Shopping"),
    TRANSPORT("transport", "Transport"),
    ENTERTAINMENT("entertainment", "Entertainment"),
    EDUCATION("Education", "Education"),
    MEDICAL("Medical", "Medical"),
    FINANCE("Finance", "Finance"),
    GOVERNMENT("Government", "Government"),
    TOURIST("Tourist", "Tourist"),
    DEFAULT("", "Default")
}

/**
 * PoiItem æ‰©å±•å‡½æ•°
 */
fun PoiItem.toPoiResult(): PoiResult {
    return PoiResult(
        id = this.poiId,
        name = this.title,
        type = this.type,
        address = this.snippet ?: this.address ?: "",
        latitude = this.latLonPoint.latitude,
        longitude = this.latLonPoint.longitude,
        distance = this.distance,
        tel = this.tel,
        rating = null // é«˜å¾· POI ä¸ç›´æŽ¥æä¾›è¯„åˆ†ï¼Œéœ€è¦é¢å¤–æŸ¥è¯?
    )
}

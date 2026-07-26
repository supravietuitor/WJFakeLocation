package com.steadywj.wjfakelocation.manager.search.utils

import android.content.Context
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.poisearch.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoiSearchManager @Inject constructor(
    private val context: Context
) {

    fun searchNearby(
        latitude: Double,
        longitude: Double,
        radius: Int = 3000,
        type: PoiType = PoiType.FOOD
    ): Flow<List<PoiResult>> = callbackFlow {
        withContext(Dispatchers.IO) {
            try {
                val query = PoiSearch.Query("", type.code, "")
                query.setPageSize(20)
                query.setPageNum(1)

                val poiSearch = PoiSearch(context, query)
                poiSearch.setBound(PoiSearch.SearchBound(LatLonPoint(latitude, longitude), radius))

                poiSearch.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiResult?, errorCode: Int) {
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
                    }
                })

                poiSearch.searchPOIAsyn()
            } catch (e: Exception) {
                trySend(emptyList())
            }
        }

        awaitClose {}
    }

    fun searchByKeyword(
        keyword: String,
        city: String = ""
    ): Flow<List<PoiResult>> = callbackFlow {
        withContext(Dispatchers.IO) {
            try {
                val query = PoiSearch.Query(keyword, "", city)
                query.setPageSize(20)
                query.setPageNum(1)

                val poiSearch = PoiSearch(context, query)

                poiSearch.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiResult?, errorCode: Int) {
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
                    }
                })

                poiSearch.searchPOIAsyn()
            } catch (e: Exception) {
                trySend(emptyList())
            }
        }

        awaitClose {}
    }

    fun getPopularTypes(): List<PoiType> {
        return listOf(
            PoiType.FOOD,
            PoiType.HOTEL,
            PoiType.SHOPPING,
            PoiType.TRANSPORT,
            PoiType.ENTERTAINMENT
        )
    }
}

data class PoiResult(
    val id: String,
    val name: String,
    val type: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Float?,
    val tel: String?,
    val rating: Float?
)

enum class PoiType(val code: String, val displayName: String) {
    FOOD("food", "Food"),
    HOTEL("hotel", "Hotel"),
    SHOPPING("shopping", "Shopping"),
    TRANSPORT("transport", "Transport"),
    ENTERTAINMENT("entertainment", "Entertainment"),
    EDUCATION("education", "Education"),
    MEDICAL("medical", "Medical"),
    FINANCE("finance", "Finance"),
    GOVERNMENT("government", "Government"),
    TOURIST("tourist", "Tourist"),
    DEFAULT("", "Default")
}

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
        rating = null
    )
}

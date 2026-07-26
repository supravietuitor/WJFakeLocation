package com.steadywj.wjfakelocation.manager.search.utils

import android.content.Context
import com.amap.api.services.core.LatLonPoint
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
                    override fun onPoiSearched(result: com.amap.api.services.poisearch.PoiResult?, errorCode: Int) {
                        if (errorCode == 0 && result != null) {
                            try {
                                val poisList = result.pois
                                if (poisList != null) {
                                    val pois = mutableListOf<PoiResult>()
                                    for (i in 0 until poisList.size) {
                                        val poiItem = poisList[i]
                                        val id = poiItem?.poiId ?: continue
                                        val title = poiItem.title ?: ""
                                        val snippet = poiItem.snippet ?: ""
                                        val point = poiItem.latLonPoint
                                        val lat = point?.latitude ?: latitude
                                        val lng = point?.longitude ?: longitude
                                        val dist = try { poiItem.distance.toFloat() } catch (_: Exception) { 0f }
                                        val tel = try { poiItem.tel } catch (_: Exception) { null }
                                        pois.add(PoiResult(
                                            id = id,
                                            name = title,
                                            type = "",
                                            address = snippet,
                                            latitude = lat,
                                            longitude = lng,
                                            distance = dist,
                                            tel = tel,
                                            rating = null
                                        ))
                                    }
                                    trySend(pois)
                                } else {
                                    trySend(emptyList())
                                }
                            } catch (e: Exception) {
                                trySend(emptyList())
                            }
                        } else {
                            trySend(emptyList())
                        }
                    }

                    override fun onPoiItemSearched(poiItem: Any?, errorCode: Int) {
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
                    override fun onPoiSearched(result: com.amap.api.services.poisearch.PoiResult?, errorCode: Int) {
                        if (errorCode == 0 && result != null) {
                            try {
                                val poisList = result.pois
                                if (poisList != null) {
                                    val pois = mutableListOf<PoiResult>()
                                    for (i in 0 until poisList.size) {
                                        val poiItem = poisList[i]
                                        val id = poiItem?.poiId ?: continue
                                        val title = poiItem.title ?: ""
                                        val snippet = poiItem.snippet ?: ""
                                        val point = poiItem.latLonPoint
                                        val lat = point?.latitude ?: 0.0
                                        val lng = point?.longitude ?: 0.0
                                        val dist = try { poiItem.distance.toFloat() } catch (_: Exception) { 0f }
                                        val tel = try { poiItem.tel } catch (_: Exception) { null }
                                        pois.add(PoiResult(
                                            id = id,
                                            name = title,
                                            type = "",
                                            address = snippet,
                                            latitude = lat,
                                            longitude = lng,
                                            distance = dist,
                                            tel = tel,
                                            rating = null
                                        ))
                                    }
                                    trySend(pois)
                                } else {
                                    trySend(emptyList())
                                }
                            } catch (e: Exception) {
                                trySend(emptyList())
                            }
                        } else {
                            trySend(emptyList())
                        }
                    }

                    override fun onPoiItemSearched(poiItem: Any?, errorCode: Int) {
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

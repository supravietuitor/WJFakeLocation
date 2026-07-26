// MapInteractionManager.kt
package com.steadywj.wjfakelocation.manager.map.utils

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.amap.api.maps.AMap
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * �啣鈭支�蝞∠��?
 * 
 * �:
 * - �踵�瘛餃��扇�?
 * - �敺株�雿蔭
 * - ��曉之/蝻拙�
 * - �霂
 */
@Singleton
class MapInteractionManager @Inject constructor() {
    
    /** 敶��扇�?*/
    private val _currentMarker = MutableStateFlow<Marker?>(null)
    val currentMarker: StateFlow<Marker?> = _currentMarker.asStateFlow()
    
    /** ���颱�蝵?*/
    private var lastTapPosition: LatLng? = null
    
    /** �璉瘚 */
    private var gestureDetector: GestureDetector? = null
    
    /** �臬甇�� */
    private var isDragging = false
    
    /**
     * �����踵�瘚
     */
    fun initialize(context: Context, aMap: AMap) {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            
            override fun onLongPress(e: MotionEvent) {
                // �踵�瘛餃��扇�?
                val screenX = e.x
                val screenY = e.y
                
                aMap.projection.fromScreenLocation(
                    android.graphics.Point(screenX.toInt(), screenY.toInt())
                )?.let { latLng ->
                    addMarkerAtLocation(aMap, latLng, "Done")
                }
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // ��曉之
                val screenX = e.x
                val screenY = e.y
                
                aMap.projection.fromScreenLocation(
                    android.graphics.Point(screenX.toInt(), screenY.toInt())
                )?.let { latLng ->
                    // 隞亦�颱�蝵桐蛹銝剖��曉之
                    aMap.animateMap(
                        com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                            latLng,
                            (aMap.cameraPosition.zoom + 2).coerceIn(3f, 18f)
                        )
                    )
                }
                return true
            }
            
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // ���扇�?
                val screenX = e.x
                val screenY = e.y
                
                aMap.projection.fromScreenLocation(
                    android.graphics.Point(screenX.toInt(), screenY.toInt())
                )?.let { latLng ->
                    lastTapPosition = latLng
                    
                    // 璉�交�衣�餃�唳��扇�?
                    _currentMarker.value?.let { marker ->
                        val distance = calculateDistance(latLng, marker.position)
                        if (distance < 50) { // 50 蝐唾��游�
                            selectMarker(marker)
                        }
                    }
                }
                return true
            }
        })
        
        // 霈曄蔭�啣閫行�
        aMap.setOnMapTouchListener { event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    isDragging = true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // �剛�蝳餌宏�刻�銝箇�?
                        gestureDetector?.onTouchEvent(event)
                    }
                    isDragging = false
                }
            }
        }
        
        // 霈曄蔭�扇�寞��函��?
        aMap.setOnMarkerDragListener(object : AMap.OnMarkerDragListener {
            override fun onMarkerStartDrag(marker: Marker) {
                // 撘憪��?
                selectMarker(marker)
            }
            
            override fun onMarkerDrag(marker: Marker) {
                // �銝?- 摰�湔雿蔭
                updateMarkerPosition(marker)
            }
            
            override fun onMarkerEndDrag(marker: Marker) {
                // �蝏� - 靽��蝏�蝵?
                finalizeMarkerPosition(marker)
            }
        })
    }
    
    /**
     * �冽�摰�蝵格溶��霈啁
     */
    fun addMarkerAtLocation(aMap: AMap, latLng: LatLng, title: String = "�格�雿蔭"): Marker? {
        // 蝘駁�扳�霈啁
        _currentMarker.value?.remove()
        
        try {
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet("${latLng.latitude}, ${latLng.longitude}")
                .draggable(true) // �舀��?
                .icon(com.amap.api.maps.model.BitmapDescriptorFactory.defaultMarker())
            
            val marker = aMap.addMarker(markerOptions)
            _currentMarker.value = marker
            
            // 蝘餃�唳�霈啁雿蔭
            aMap.animateMap(
                com.amap.api.maps.CameraUpdateFactory.newLatLng(latLng)
            )
            
            return marker
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * ��扇�?
     */
    fun selectMarker(marker: Marker) {
        _currentMarker.value = marker
        marker.showInfoWindow()
    }
    
    /**
     * �湔�扇�嫣�蝵殷��銝哨�
     */
    fun updateMarkerPosition(marker: Marker) {
        // �臭誑�刻����嗆�?UI �蝷箏��縑�?
    }
    
    /**
     * 蝖株恕�扇�寞�蝏�蝵殷��蝏�嚗?
     */
    fun finalizeMarkerPosition(marker: Marker) {
        val position = marker.position
        marker.snippet = "${position.latitude}, ${position.longitude}"
        
        // � ViewModel �湔�葉��蝵?
        // �����?Flow 摰
    }
    
    /**
     * 皜���霈啁
     */
    fun clearMarkers() {
        _currentMarker.value?.remove()
        _currentMarker.value = null
    }
    
    /**
     * 霈∠�銝斤�渲�蝳鳴�蝐喉�
     */
    private fun calculateDistance(from: LatLng, to: LatLng): Double {
        val earthRadius = 6371000.0 // 蝐?
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(from.latitude)) * Math.cos(Math.toRadians(to.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * �韏�
     */
    fun destroy() {
        clearMarkers()
        gestureDetector = null
    }
}

/**
 * Compose �拙�嚗�曆漱鈭耨擖啁泵
 */
@Composable
fun rememberMapInteraction(
    context: Context,
    aMap: AMap?,
    onMarkerMoved: ((LatLng) -> Unit)? = null
): MapInteractionManager {
    val manager = remember { MapInteractionManager() }
    
    DisposableEffect(context, aMap) {
        aMap?.let { map ->
            manager.initialize(context, map)
        }
        
        onDispose {
            manager.destroy()
        }
    }
    
    return manager
}

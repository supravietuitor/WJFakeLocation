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
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapInteractionManager @Inject constructor() {
    
    private val _currentMarker = MutableStateFlow<Marker?>(null)
    val currentMarker: StateFlow<Marker?> = _currentMarker.asStateFlow()
    
    private var lastTapPosition: LatLng? = null
    
    private var gestureDetector: GestureDetector? = null
    
    private var isDragging = false
    
    fun initialize(context: Context, aMap: AMap) {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            
            override fun onLongPress(e: MotionEvent) {
                val screenX = e.x
                val screenY = e.y
                
                aMap.projection.fromScreenLocation(
                    android.graphics.Point(screenX.toInt(), screenY.toInt())
                )?.let { latLng ->
                    addMarkerAtLocation(aMap, latLng, "Marker")
                }
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val screenX = e.x
                val screenY = e.y
                
                aMap.projection.fromScreenLocation(
                    android.graphics.Point(screenX.toInt(), screenY.toInt())
                )?.let { latLng ->
                    aMap.animateCamera(
                        com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                            latLng,
                            (aMap.cameraPosition.zoom + 2).coerceIn(3f, 18f)
                        )
                    )
                }
                return true
            }
            
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val screenX = e.x
                val screenY = e.y
                
                aMap.projection.fromScreenLocation(
                    android.graphics.Point(screenX.toInt(), screenY.toInt())
                )?.let { latLng ->
                    lastTapPosition = latLng
                    
                    _currentMarker.value?.let { marker ->
                        val distance = calculateDistance(latLng, marker.position)
                        if (distance < 50) {
                            selectMarker(marker)
                        }
                    }
                }
                return true
            }
        })
        
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
                        gestureDetector?.onTouchEvent(event)
                    }
                    isDragging = false
                }
            }
        }
        
        aMap.setOnMarkerDragListener(object : AMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
                selectMarker(marker)
            }
            
            override fun onMarkerDrag(marker: Marker) {
                updateMarkerPosition(marker)
            }
            
            override fun onMarkerDragEnd(marker: Marker) {
                finalizeMarkerPosition(marker)
            }
        })
    }
    
    fun addMarkerAtLocation(aMap: AMap, latLng: LatLng, title: String = "Marker"): Marker? {
        _currentMarker.value?.remove()
        
        try {
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet("${latLng.latitude}, ${latLng.longitude}")
                .draggable(true)
                .icon(com.amap.api.maps.model.BitmapDescriptorFactory.defaultMarker())
            
            val marker = aMap.addMarker(markerOptions)
            _currentMarker.value = marker
            
            aMap.animateCamera(
                com.amap.api.maps.CameraUpdateFactory.newLatLng(latLng)
            )
            
            return marker
        } catch (e: Exception) {
            return null
        }
    }
    
    fun selectMarker(marker: Marker) {
        _currentMarker.value = marker
        marker.showInfoWindow()
    }
    
    fun updateMarkerPosition(marker: Marker) {
    }
    
    fun finalizeMarkerPosition(marker: Marker) {
        val position = marker.position
        marker.snippet = "${position.latitude}, ${position.longitude}"
    }
    
    fun clearMarkers() {
        _currentMarker.value?.remove()
        _currentMarker.value = null
    }
    
    private fun calculateDistance(from: LatLng, to: LatLng): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(from.latitude)) * Math.cos(Math.toRadians(to.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    fun destroy() {
        clearMarkers()
        gestureDetector = null
    }
}

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

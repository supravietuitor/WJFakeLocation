// AMapView.kt
package com.steadywj.wjfakelocation.manager.map.components

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions

/**
 * é«˜å¾·åœ°å›¾ MapView åŒ…è£…ç»„ä»¶ï¼ˆä¼˜åŒ–ç‰ˆï¼?
 * 
 * ç‰¹æ€?
 * - é¢„åŠ è½½ç¼“å­?
 * - åŠ è½½è¿›åº¦æŒ‡ç¤ºå™?
 * - ç”Ÿå‘½å‘¨æœŸè‡ªåŠ¨ç®¡ç†
 * 
 * @param modifier Compose ä¿®é¥°ç¬?
 * @param onMapReady åœ°å›¾å‡†å¤‡å°±ç»ªå›žè°ƒ
 * @param initialLatitude åˆå§‹çº¬åº¦
 * @param initialLongitude åˆå§‹ç»åº¦
 * @param zoomLevel ç¼©æ”¾çº§åˆ«ï¼ˆé»˜è®?15ï¼?
 */
@Composable
fun AMapView(
    modifier: Modifier = Modifier,
    onMapReady: ((AMap) -> Unit)? = null,
    initialLatitude: Double = 39.908823, // åŒ—äº¬
    initialLongitude: Double = 116.397470,
    zoomLevel: Float = 15f
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var aMap by remember { mutableStateOf<AMap?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    mapView = this
                    
                    // èŽ·å– AMap å®žä¾‹
                    getMapAsync { map ->
                        aMap = map
                        
                        // è®¾ç½®åˆå§‹ä½ç½®å’Œç¼©æ”¾çº§åˆ?
                        val latLng = LatLng(initialLatitude, initialLongitude)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
                        
                        // å¯ç”¨å®šä½å›¾å±‚ï¼ˆéœ€è¦æƒé™ï¼‰
                        map.isMyLocationEnabled = true
                        
                        // æ ‡è®°ä¸ºå·²åŠ è½½
                        isMapLoaded = true
                        
                        // é€šçŸ¥åœ°å›¾å·²å‡†å¤‡å¥½
                        onMapReady?.invoke(map)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // å¯ä»¥åœ¨è¿™é‡Œæ›´æ–°åœ°å›¾é…ç½?
            }
        )
        
        // æ˜¾ç¤ºåŠ è½½è¿›åº¦æŒ‡ç¤ºå™?
        if (!isMapLoaded) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * åœ¨åœ°å›¾ä¸Šæ·»åŠ æ ‡è®°ç‚?
 * 
 * @param latitude çº¬åº¦
 * @param longitude ç»åº¦
 * @param title æ ‡é¢˜
 * @param snippet æè¿°ä¿¡æ¯
 * @param draggable æ˜¯å¦å¯æ‹–æ‹½ï¼ˆé»˜è®¤ trueï¼?
 * @param onClick ç‚¹å‡»å›žè°ƒ
 */
@Composable
fun MapMarker(
    latitude: Double,
    longitude: Double,
    title: String? = null,
    snippet: String? = null,
    draggable: Boolean = true,
    onClick: (() -> Unit)? = null,
    map: AMap?
) {
    DisposableEffect(latitude, longitude, title, snippet, map) {
        var marker: com.amap.api.maps.model.Marker? = null
        
        if (map != null) {
            val markerOptions = MarkerOptions()
                .position(LatLng(latitude, longitude))
                .title(title)
                .snippet(snippet)
                .draggable(draggable)
            
            marker = map.addMarker(markerOptions)
            
            // è®¾ç½®ç‚¹å‡»ç›‘å¬å™?
            if (onClick != null) {
                map.setOnMarkerClickListener { clickedMarker ->
                    if (clickedMarker == marker) {
                        onClick()
                        true
                    } else {
                        false
                    }
                }
            }
        }
        
        onDispose {
            // æ¸…ç†æ ‡è®°ç‚?
            marker?.remove()
        }
    }
}

/**
 * é«˜å¾·åœ°å›¾ç”Ÿå‘½å‘¨æœŸç®¡ç†
 * éœ€è¦åœ¨ Composable ä¸­è°ƒç”¨ä»¥æ­£ç¡®å¤„ç†ç”Ÿå‘½å‘¨æœŸ
 */
@Composable
fun AMapLifecycleHandler(mapView: MapView?) {
    val context = LocalContext.current
    
    DisposableEffect(context, mapView) {
        mapView?.onResume()
        
        onDispose {
            mapView?.onDestroy()
        }
    }
    
    DisposableEffect(Unit) {
        val lifecycleObserver = object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                    androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                    else -> {}
                }
            }
        }
        
        val lifecycleOwner = LocalContext.current as? androidx.lifecycle.LifecycleOwner
        lifecycleOwner?.lifecycle?.addObserver(lifecycleObserver)
        
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        }
    }
}

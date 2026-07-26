// BaiduMapView.kt
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun BaiduMapView(
    modifier: Modifier = Modifier,
    onMapReady: ((BaiduMap) -> Unit)? = null,
    initialLatitude: Double = 39.908823,
    initialLongitude: Double = 116.397470,
    zoomLevel: Float = 15f
) {
    val context = LocalContext.current
    var isMapLoaded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    val baiduMap = map
                    
                    val currentLatLng = LatLng(initialLatitude, initialLongitude)
                    val update = MapStatusUpdateFactory.newLatLngZoom(currentLatLng, zoomLevel)
                    baiduMap.setMapStatus(update)
                    
                    baiduMap.isMyLocationEnabled = true
                    
                    isMapLoaded = true
                    
                    onMapReady?.invoke(baiduMap)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (!isMapLoaded) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BaiduMapMarker(
    latitude: Double,
    longitude: Double,
    title: String? = null,
    snippet: String? = null,
    draggable: Boolean = true,
    onClick: (() -> Unit)? = null,
    baiduMap: BaiduMap?
) {
    DisposableEffect(latitude, longitude, title, snippet, baiduMap) {
        var marker: Marker? = null
        if (baiduMap != null) {
            val latLng = LatLng(latitude, longitude)
            
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title(title)
                .draggable(draggable)
            
            marker = baiduMap.addOverlay(markerOptions) as? Marker
            
            if (onClick != null) {
                baiduMap.setOnMarkerClickListener { clickedMarker ->
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
            marker?.remove()
        }
    }
}

@Composable
fun rememberBaiduMapLifecycle(mapView: MapView?) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = if (mapView != null) {
            mapView.onCreate(context, Bundle())
            
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }.also { lifecycleOwner.lifecycle.addObserver(it) }
        } else null
        
        onDispose {
            observer?.let { lifecycleOwner.lifecycle.removeObserver(it) }
        }
    }
}

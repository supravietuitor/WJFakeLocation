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

@Composable
fun AMapView(
    modifier: Modifier = Modifier,
    onMapReady: ((AMap) -> Unit)? = null,
    initialLatitude: Double = 39.908823,
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

                    onCreate(null)
                    val map = getMap()
                    if (map != null) {
                        aMap = map

                        val latLng = LatLng(initialLatitude, initialLongitude)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))

                        map.isMyLocationEnabled = true

                        isMapLoaded = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                aMap?.let { map ->
                    onMapReady?.invoke(map)
                }
            }
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
            marker?.remove()
        }
    }
}

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

// CompassOverlay.kt
package com.steadywj.wjfakelocation.manager.map.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompassOverlay @Inject constructor() : SensorEventListener {
    
    private val _heading = MutableStateFlow(0f)
    val heading: StateFlow<Float> = _heading.asStateFlow()
    
    private var sensorManager: SensorManager? = null
    
    private var compassMarker: Marker? = null
    
    private var aMap: AMap? = null
    
    private var isEnabled = false
    
    fun initialize(context: Context, map: AMap) {
        aMap = map
        
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        addCompassMarker()
        
        enable()
    }
    
    private fun addCompassMarker() {
        if (aMap == null) return
        
        try {
            val markerOptions = MarkerOptions()
                .position(aMap!!.cameraPosition.target)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .anchor(0.5f, 0.5f)
                .draggable(false)
                .visible(true)
            
            compassMarker = aMap!!.addMarker(markerOptions)
        } catch (e: Exception) {
        }
    }
    
    fun enable() {
        if (isEnabled) return
        
        isEnabled = true
        
        sensorManager?.registerListener(
            this,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_UI
        )
    }
    
    fun disable() {
        if (!isEnabled) return
        
        isEnabled = false
        sensorManager?.unregisterListener(this)
    }
    
    private fun updateCompassRotation(heading: Float) {
        compassMarker?.setRotateAngle(heading)
    }
    
    fun destroy() {
        disable()
        compassMarker?.remove()
        compassMarker = null
        aMap = null
        sensorManager = null
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ORIENTATION) return
        
        val azimuth = event.values[0]
        
        if (kotlin.math.abs(azimuth - _heading.value) > 2.0f) {
            _heading.value = azimuth
            updateCompassRotation(azimuth)
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}

@Composable
fun rememberCompassOverlay(map: AMap?): CompassOverlay {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val compassOverlay = remember { CompassOverlay() }
    
    DisposableEffect(context, map) {
        map?.let { aMap ->
            compassOverlay.initialize(context, aMap)
        }
        
        onDispose {
            compassOverlay.destroy()
        }
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> compassOverlay.enable()
                Lifecycle.Event.ON_PAUSE -> compassOverlay.disable()
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    return compassOverlay
}

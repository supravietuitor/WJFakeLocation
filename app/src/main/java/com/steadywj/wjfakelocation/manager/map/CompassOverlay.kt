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

/**
 * ç½—ç›˜æ–¹å‘æŒ‡ç¤ºå™?
 * 
 * åŠŸèƒ½:
 * - å®žæ—¶èŽ·å–è®¾å¤‡æœå‘
 * - åœ¨åœ°å›¾ä¸Šæ˜¾ç¤ºæ–¹å‘ç®­å¤´
 * - è‡ªåŠ¨æ—‹è½¬æ›´æ–°
 */
@Singleton
class CompassOverlay @Inject constructor() : SensorEventListener {
    
    /** å½“å‰æœå‘è§’åº¦ï¼?-360 åº¦ï¼Œæ­£åŒ—ä¸?0ï¼?*/
    private val _heading = MutableStateFlow(0f)
    val heading: StateFlow<Float> = _heading.asStateFlow()
    
    /** ä¼ æ„Ÿå™¨ç®¡ç†å™¨ */
    private var sensorManager: SensorManager? = null
    
    /** æ–¹å‘æ ‡è®°ç‚?*/
    private var compassMarker: Marker? = null
    
    /** åœ°å›¾å®žä¾‹ */
    private var aMap: AMap? = null
    
    /** æ˜¯å¦å¯ç”¨ç½—ç›˜ */
    private var isEnabled = false
    
    /**
     * åˆå§‹åŒ–ç½—ç›?
     * @param context ä¸Šä¸‹æ–?
     * @param map åœ°å›¾å®žä¾‹
     */
    fun initialize(context: Context, map: AMap) {
        aMap = map
        
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        // æ·»åŠ æ–¹å‘ç®­å¤´æ ‡è®°ç‚¹ï¼ˆé»˜è®¤æŒ‡å‘æ­£åŒ—ï¼?
        addCompassMarker()
        
        enable()
    }
    
    /**
     * æ·»åŠ æ–¹å‘ç®­å¤´æ ‡è®°ç‚?
     */
    private fun addCompassMarker() {
        if (aMap == null) return
        
        try {
            // ä½¿ç”¨é«˜å¾·åœ°å›¾å†…ç½®çš„æ–¹å‘å›¾æ ?
            val markerOptions = MarkerOptions()
                .position(aMap!!.cameraPosition.target)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .anchor(0.5f, 0.5f) // ä¸­å¿ƒå¯¹é½
                .draggable(false)
                .visible(true)
            
            compassMarker = aMap!!.addMarker(markerOptions)
        } catch (e: Exception) {
            // å¿½ç•¥æ·»åŠ å¤±è´¥
        }
    }
    
    /**
     * å¯ç”¨ç½—ç›˜
     */
    fun enable() {
        if (isEnabled) return
        
        isEnabled = true
        
        // æ³¨å†Œæ–¹å‘ä¼ æ„Ÿå™?
        sensorManager?.registerListener(
            this,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_UI // UI åˆ·æ–°é¢‘çŽ‡
        )
    }
    
    /**
     * ç¦ç”¨ç½—ç›˜
     */
    fun disable() {
        if (!isEnabled) return
        
        isEnabled = false
        sensorManager?.unregisterListener(this)
    }
    
    /**
     * æ›´æ–°ç®­å¤´æ–¹å‘
     */
    private fun updateCompassRotation(heading: Float) {
        compassMarker?.let { marker ->
            // å¹³æ»‘æ—‹è½¬åˆ°ç›®æ ‡è§’åº?
            marker.rotation = heading
        }
    }
    
    /**
     * é‡Šæ”¾èµ„æº
     */
    fun destroy() {
        disable()
        compassMarker?.remove()
        compassMarker = null
        aMap = null
        sensorManager = null
    }
    
    // ==================== SensorEventListener ====================
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ORIENTATION) return
        
        val azimuth = event.values[0] // æ–¹ä½è§’ï¼ˆ0-360 åº¦ï¼‰
        
        // è¿‡æ»¤æŠ–åŠ¨
        if (kotlin.math.abs(azimuth - _heading.value) > 2.0f) {
            _heading.value = azimuth
            updateCompassRotation(azimuth)
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // ç²¾åº¦å˜åŒ–æ—¶çš„å›žè°ƒï¼ˆå¯é€‰å¤„ç†ï¼‰
    }
}

/**
 * Compose æ‰©å±•ï¼šè®°ä½ç½—ç›˜ç»„ä»?
 */
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
    
    // ç›‘å¬ç”Ÿå‘½å‘¨æœŸ
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

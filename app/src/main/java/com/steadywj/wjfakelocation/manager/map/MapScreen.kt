// MapScreen.kt
package com.steadywj.wjfakelocation.manager.map

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.steadywj.wjfakelocation.R
import com.steadywj.wjfakelocation.manager.map.components.AMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onLocationSelected: ((Double, Double) -> Unit)? = null
) {
    var showDrawer by remember { mutableStateOf(false) }
    var currentLat by remember { mutableDoubleStateOf(39.908823) }
    var currentLng by remember { mutableDoubleStateOf(116.397470) }
    var zoomLevel by remember { mutableFloatStateOf(15f) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.nav_map)) },
                navigationIcon = {
                    IconButton(onClick = { showDrawer = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Locate current position */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Current location")
            }
        }
    ) { paddingValues ->
        AMapView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            initialLatitude = currentLat,
            initialLongitude = currentLng,
            zoomLevel = zoomLevel,
            onMapReady = { aMap ->
                Log.d("MapScreen", "AMap loaded")
            }
        )
    }
}

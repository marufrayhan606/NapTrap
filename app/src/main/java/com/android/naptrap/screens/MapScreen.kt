package com.android.naptrap.screens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.android.naptrap.ui.theme.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onNavigateBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: com.android.naptrap.ui.DestinationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    var selectedLat by remember { mutableStateOf<Double?>(null) }
    var selectedLon by remember { mutableStateOf<Double?>(null) }
    var name by remember { mutableStateOf("") }
    var mapView: org.osmdroid.views.MapView? = null
    var zoomLevel by remember { mutableStateOf(15.0) }
    var markerTitle by remember { mutableStateOf("Selected Location") }
    var showNameSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    data class SearchResult(val displayName: String, val lat: Double, val lon: Double)
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var showDropdown by remember { mutableStateOf(false) }
    var currentLocationLat by remember { mutableStateOf<Double?>(null) }
    var currentLocationLon by remember { mutableStateOf<Double?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        Column {
            // Modern Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Text(
                        text = "Your Route",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
                }
            }
            
            // Modern Search Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { 
                            Text(
                                "Find Destination",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ) 
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            isSearching = true
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val client = OkHttpClient()
                                    val url = "https://nominatim.openstreetmap.org/search?format=json&q=" + java.net.URLEncoder.encode(searchQuery, "UTF-8") + "&limit=5"
                                    val request = Request.Builder()
                                        .url(url)
                                        .header("User-Agent", "NapTrap/1.0")
                                        .build()
                                    val response = client.newCall(request).execute()
                                    val body = response.body?.string()
                                    val results = mutableListOf<SearchResult>()
                                    if (body != null) {
                                        val arr = JSONArray(body)
                                            for (i in 0 until arr.length()) {
                                                val obj = arr.getJSONObject(i)
                                                val name = obj.getString("display_name")
                                                val lat = obj.getDouble("lat")
                                                val lon = obj.getDouble("lon")
                                                results.add(SearchResult(name, lat, lon))
                                            }
                                        }
                                        kotlinx.coroutines.Dispatchers.Main.let {
                                            searchResults = results
                                            showDropdown = results.isNotEmpty()
                                            if (results.isEmpty()) {
                                                android.widget.Toast.makeText(context, "No locations found", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        kotlinx.coroutines.Dispatchers.Main.let {
                                            searchResults = emptyList()
                                            showDropdown = false
                                            android.widget.Toast.makeText(context, "Error searching location", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    isSearching = false
                                }
                            },
                            enabled = searchQuery.isNotBlank() && !isSearching
                        ) {
                            Text("Search")
                        }
                    // Dropdown for search results
                    if (showDropdown && searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column {
                                searchResults.forEach { result ->
                                    TextButton(
                                        onClick = {
                                            selectedLat = result.lat
                                            selectedLon = result.lon
                                            markerTitle = result.displayName
                                            mapView?.controller?.setCenter(org.osmdroid.util.GeoPoint(result.lat, result.lon))
                                            mapView?.controller?.setZoom(zoomLevel)
                                            mapView?.invalidate()
                                            showDropdown = false
                                            searchResults = emptyList()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(result.displayName)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Map Container
            Box(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            mapView = org.osmdroid.views.MapView(ctx)
                            mapView!!.layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            mapView!!.setMultiTouchControls(true)
                            mapView!!.controller.setZoom(zoomLevel)
                            mapView!!.controller.setCenter(
                                org.osmdroid.util.GeoPoint(
                                    23.8103,
                                    90.4125
                                )
                            ) // Default to Dhaka
                            var downTime: Long = 0L
                            var downX: Float = 0f
                            var downY: Float = 0f
                            var isLongPress = false
                            mapView!!.setOnTouchListener { v, event ->
                                when (event.action) {
                                    android.view.MotionEvent.ACTION_DOWN -> {
                                        downTime = event.eventTime
                                        downX = event.x
                                        downY = event.y
                                        isLongPress = false
                                    }

                                    android.view.MotionEvent.ACTION_MOVE -> {
                                        // If user moves more than a threshold, cancel long press
                                        if (Math.abs(event.x - downX) > 20 || Math.abs(event.y - downY) > 20) {
                                            isLongPress = false
                                        }
                                    }

                                    android.view.MotionEvent.ACTION_UP -> {
                                        val duration = event.eventTime - downTime
                                        // Long press threshold: 500ms, no movement
                                        if (duration > 500 && Math.abs(event.x - downX) < 20 && Math.abs(
                                                event.y - downY
                                            ) < 20
                                        ) {
                                            isLongPress = true
                                        }
                                        if (isLongPress) {
                                            val proj = mapView!!.projection
                                            val geoPoint = proj.fromPixels(
                                                event.x.toInt(),
                                                event.y.toInt()
                                            ) as org.osmdroid.util.GeoPoint
                                            selectedLat = geoPoint.latitude
                                            selectedLon = geoPoint.longitude
                                            markerTitle = "Selected Location"
                                            showNameSheet = true
                                            // Add marker for selected location
                                            mapView!!.overlays.clear()
                                            val marker = org.osmdroid.views.overlay.Marker(mapView)
                                            marker.position = org.osmdroid.util.GeoPoint(
                                                selectedLat!!,
                                                selectedLon!!
                                            )
                                            marker.setAnchor(
                                                org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                                                org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM
                                            )
                                            marker.title = markerTitle
                                            mapView!!.overlays.add(marker)
                                            mapView!!.invalidate()
                                        }
                                    }
                                }
                                false
                            }
                            mapView!!

                        },
                        update = {
                            // Always show marker for selectedLat/Lon and current location
                            if (mapView != null) {
                                mapView!!.overlays.clear()
                                // Selected location marker
                                if (selectedLat != null && selectedLon != null) {
                                    val marker = org.osmdroid.views.overlay.Marker(mapView)
                                    marker.position = org.osmdroid.util.GeoPoint(selectedLat!!, selectedLon!!)
                                    marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER)
                                    marker.title = markerTitle
                                    mapView!!.overlays.add(marker)
                                }
                                // Current location marker (blue circle)
                                if (currentLocationLat != null && currentLocationLon != null) {
                                    val currMarker = org.osmdroid.views.overlay.Marker(mapView)
                                    currMarker.position = org.osmdroid.util.GeoPoint(currentLocationLat!!, currentLocationLon!!)
                                    currMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER)
                                    currMarker.title = "Current Location"
                                    // Use a blue circle drawable for current location
                                    val blueCircle = android.graphics.drawable.ShapeDrawable(android.graphics.drawable.shapes.OvalShape())
                                    blueCircle.paint.color = android.graphics.Color.BLUE
                                    blueCircle.setIntrinsicWidth(32)
                                    blueCircle.setIntrinsicHeight(32)
                                    currMarker.icon = blueCircle
                                    mapView!!.overlays.add(currMarker)
                                }
                                mapView!!.invalidate()
                            }
                        }
                    )

                    // Overlay: Current location FAB (bottom right)
                    Box(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.BottomEnd)
                            .padding(24.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                // Center map on current location
                                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    val fusedLocationClient =
                                        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(
                                            context
                                        )
                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        if (location != null) {
                                            currentLocationLat = location.latitude
                                            currentLocationLon = location.longitude
                                            mapView?.controller?.setCenter(
                                                org.osmdroid.util.GeoPoint(
                                                    location.latitude,
                                                    location.longitude
                                                )
                                            )
                                            mapView?.controller?.setZoom(zoomLevel)
                                            mapView?.invalidate()
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Location permission not granted.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Current Location")
                        }
                    }

                    // Overlay: Drop pin FAB removed (no longer needed)

                    // Overlay: Selected location text (top left)
                    if (selectedLat != null && selectedLon != null) {
                        Box(
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                "Selected: Lat ${selectedLat}, Lon ${selectedLon}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Modal bottom sheet for naming destination
                    if (showNameSheet && selectedLat != null && selectedLon != null) {
                        ModalBottomSheet(
                            onDismissRequest = { showNameSheet = false },
                            sheetState = sheetState,
                            dragHandle = null,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Name your destination",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Name/Address") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                )
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.addDestination(
                                                com.android.naptrap.data.Destination(
                                                    name = if (name.isBlank()) "Map Destination" else name,
                                                    latitude = selectedLat!!,
                                                    longitude = selectedLon!!
                                                )
                                            )
                                            android.widget.Toast.makeText(
                                                context,
                                                "Destination saved!",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            showNameSheet = false
                                            name = ""
                                            if (onNavigateBack != null) onNavigateBack()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Save")
                                        Spacer(Modifier.width(8.dp))
                                        Text("Save")
                                    }
                                    OutlinedButton(
                                        onClick = { showNameSheet = false },
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                                        Spacer(Modifier.width(8.dp))
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


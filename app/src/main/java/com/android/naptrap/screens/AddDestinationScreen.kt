package com.android.naptrap.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import android.view.ViewGroup
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import androidx.compose.ui.viewinterop.AndroidView
import com.android.naptrap.ui.DestinationViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun AddDestinationScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: DestinationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    var selectedLat by remember { mutableStateOf<Double?>(null) }
    var selectedLon by remember { mutableStateOf<Double?>(null) }
    var name by remember { mutableStateOf("") }
    var mapView: MapView? = null
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tap on the map to select a destination.", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search location/address") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Button(onClick = {
                coroutineScope.launch {
                    isSearching = true
                    searchError = ""
                    try {
                        val url = java.net.URL("https://nominatim.openstreetmap.org/search?format=json&q=" + java.net.URLEncoder.encode(searchQuery, "UTF-8"))
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.setRequestProperty("User-Agent", "NapTrap/1.0")
                        val response = conn.inputStream.bufferedReader().readText()
                        conn.disconnect()
                        val results = org.json.JSONArray(response)
                        if (results.length() > 0) {
                            val obj = results.getJSONObject(0)
                            val lat = obj.getDouble("lat")
                            val lon = obj.getDouble("lon")
                            selectedLat = lat
                            selectedLon = lon
                            mapView?.controller?.setCenter(GeoPoint(lat, lon))
                            mapView?.controller?.setZoom(16.0)
                            mapView?.invalidate()
                        } else {
                            searchError = "No results found."
                        }
                    } catch (e: Exception) {
                        searchError = "Search failed: ${e.message}"
                    }
                    isSearching = false
                }
            }, modifier = Modifier.weight(1f)) {
                Text(if (isSearching) "Searching..." else "Search")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                // Center map on current location
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            selectedLat = location.latitude
                            selectedLon = location.longitude
                            mapView?.controller?.setCenter(GeoPoint(location.latitude, location.longitude))
                            mapView?.controller?.setZoom(16.0)
                            mapView?.invalidate()
                        }
                    }
                } else {
                    searchError = "Location permission not granted."
                }
            }, modifier = Modifier.weight(1f)) {
                Text("Current Location")
            }
        }
        if (searchError.isNotEmpty()) {
            Text(searchError, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { ctx ->
                mapView = MapView(ctx)
                mapView!!.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                mapView!!.setMultiTouchControls(true)
                mapView!!.controller.setZoom(15.0)
                mapView!!.controller.setCenter(GeoPoint(23.8103, 90.4125)) // Default to Dhaka
                var downTime: Long = 0L
                mapView!!.setOnTouchListener { v, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            downTime = event.eventTime
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            val duration = event.eventTime - downTime
                            // Long press threshold: 500ms
                            if (duration > 500) {
                                val proj = mapView!!.projection
                                val geoPoint = proj.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                                selectedLat = geoPoint.latitude
                                selectedLon = geoPoint.longitude
                                // Add marker
                                mapView!!.overlays.clear()
                                val marker = org.osmdroid.views.overlay.Marker(mapView)
                                marker.position = GeoPoint(selectedLat!!, selectedLon!!)
                                marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                                marker.title = "Selected Location"
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
                // Show marker if selectedLat/Lon is set
                if (selectedLat != null && selectedLon != null && mapView != null) {
                    mapView!!.overlays.clear()
                    val marker = org.osmdroid.views.overlay.Marker(mapView)
                    marker.position = GeoPoint(selectedLat!!, selectedLon!!)
                    marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                    marker.title = "Selected Location"
                    mapView!!.overlays.add(marker)
                    mapView!!.invalidate()
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (selectedLat != null && selectedLon != null) {
            Text("Selected: Lat ${selectedLat}, Lon ${selectedLon}", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name/Address") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Button(
                onClick = {
                    viewModel.addDestination(com.android.naptrap.data.Destination(name = name.ifEmpty { "Map Destination" }, latitude = selectedLat!!, longitude = selectedLon!!))
                    navController.popBackStack()
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Save Destination")
            }
        }
    }
}

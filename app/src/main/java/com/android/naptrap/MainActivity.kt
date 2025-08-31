package com.android.naptrap

import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.naptrap.ui.DestinationViewModel
import com.android.naptrap.ui.theme.NapTrapTheme
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for shared location intent
        val sharedText = intent?.getStringExtra(android.content.Intent.EXTRA_TEXT)
        var sharedLocation: ParsedLocation? = null
        if (sharedText != null) {
            sharedLocation = parseSharedLocation(sharedText)
        }

        setContent {
            NapTrapTheme {
                // Inject ViewModel using Hilt
                val viewModel: DestinationViewModel by viewModels()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (sharedLocation != null) {
                        SaveLocationPrompt(sharedLocation, viewModel, Modifier.padding(innerPadding))
                    } else {
                        DestinationListScreen(viewModel, Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    // Helper to parse geo URI, Google Maps URL, or address from shared text
    private fun parseSharedLocation(text: String): ParsedLocation? {
        // Try geo URI: geo:lat,lon
        val geoRegex = Regex("geo:([\\d.-]+),([\\d.-]+)")
        val geoMatch = geoRegex.find(text)
        if (geoMatch != null) {
            val (lat, lon) = geoMatch.destructured
            return ParsedLocation(lat.toDouble(), lon.toDouble(), null)
        }

        // Try Google Maps URL: https://maps.google.com/?q=lat,lon or https://www.google.com/maps/place/.../@lat,lon,...
        val urlLatLonRegex = Regex("[?&]q=([\\d.-]+),([\\d.-]+)")
        val urlMatch = urlLatLonRegex.find(text)
        if (urlMatch != null) {
            val (lat, lon) = urlMatch.destructured
            return ParsedLocation(lat.toDouble(), lon.toDouble(), null)
        }

        val atLatLonRegex = Regex("@([\\d.-]+),([\\d.-]+)")
        val atMatch = atLatLonRegex.find(text)
        if (atMatch != null) {
            val (lat, lon) = atMatch.destructured
            return ParsedLocation(lat.toDouble(), lon.toDouble(), null)
        }

        // Try to resolve short Google Maps URLs online
        if (text.startsWith("https://goo.gl") || text.startsWith("https://maps.app.goo.gl")) {
            try {
                val url = java.net.URL(text.trim())
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connect()
                val locationHeader = connection.getHeaderField("Location")
                connection.disconnect()
                if (locationHeader != null) {
                    // Recursively parse the resolved URL
                    return parseSharedLocation(locationHeader)
                }
            } catch (e: Exception) {
                // Ignore errors, fallback to address
            }
        }

        // Fallback: try to extract address (not robust, but works for most cases)
        return ParsedLocation(null, null, text)
    }
}

data class ParsedLocation(val latitude: Double?, val longitude: Double?, val address: String?)

@Composable
fun SaveLocationPrompt(location: ParsedLocation, viewModel: com.android.naptrap.ui.DestinationViewModel, modifier: Modifier = Modifier) {
    val displayText = when {
        location.latitude != null && location.longitude != null ->
            "Save destination: Lat ${location.latitude}, Lon ${location.longitude}"
        location.address != null ->
            "Save destination: ${location.address}"
        else -> "Unknown location format"
    }
    val saved = remember { mutableStateOf(false) }
    if (!saved.value) {
        Text(text = displayText, modifier = modifier)
        if (location.latitude == null || location.longitude == null) {
            Text(
                text = "Tip: For best results, drop a pin in Google Maps and share the location to NapTrap. Some links or addresses may not include coordinates.",
                color = androidx.compose.ui.graphics.Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Button(onClick = {
            val name = location.address ?: "Destination"
            val lat = location.latitude ?: 0.0
            val lon = location.longitude ?: 0.0
            viewModel.addDestination(com.android.naptrap.data.Destination(name = name, latitude = lat, longitude = lon))
            saved.value = true
        }) {
            Text("Save Destination")
        }
    } else {
        Text(text = "Destination saved!", modifier = modifier)
    }
}

@Composable
fun DestinationListScreen(viewModel: com.android.naptrap.ui.DestinationViewModel, modifier: Modifier = Modifier) {
    // Live location state
    val liveLat = remember { mutableStateOf<Double?>(null) }
    val liveLon = remember { mutableStateOf<Double?>(null) }
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    liveLat.value = loc.latitude
                    liveLon.value = loc.longitude
                }
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (_: Exception) {}
    }
    val destinations by viewModel.destinations.collectAsState()
    val isTracking = remember { mutableStateOf(false) }
    val manualLat = remember { mutableStateOf("") }
    val manualLon = remember { mutableStateOf("") }
    val manualName = remember { mutableStateOf("") }
    val manualError = remember { mutableStateOf("") }
    // Load destinations when screen is shown
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.loadDestinations() }
    Surface(modifier = modifier) {
        val activity = context as? androidx.activity.ComponentActivity
        val permissionState = remember { mutableStateOf(true) }
        androidx.compose.foundation.layout.Column {
            // Live coordinates display
            androidx.compose.material3.Text(
                text = "Live Location: " +
                    (if (liveLat.value != null && liveLon.value != null) "Lat: ${liveLat.value}, Lon: ${liveLon.value}" else "Fetching..."),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            // Tracking and alarm controls at top
            androidx.compose.foundation.layout.Row {
                Button(onClick = {
                    // Request location permissions before starting tracking
                    val permissions = arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                    if (activity != null) {
                        val notGranted = permissions.filter {
                            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        }
                        if (notGranted.isNotEmpty()) {
                            androidx.core.app.ActivityCompat.requestPermissions(activity, notGranted.toTypedArray(), 1001)
                            permissionState.value = false
                        } else {
                            permissionState.value = true
                        }
                    }
                    if (permissionState.value) {
                        val intent = android.content.Intent(context, com.android.naptrap.location.LocationService::class.java)
                        context.startService(intent)
                        isTracking.value = true
                    }
                }, enabled = !isTracking.value) {
                    Text("Start Tracking")
                }
                Button(onClick = {
                    val intent = android.content.Intent(context, com.android.naptrap.location.LocationService::class.java)
                    context.stopService(intent)
                    isTracking.value = false
                }, enabled = isTracking.value) {
                    Text("Stop Tracking")
                }
                Button(onClick = {
                    val intent = android.content.Intent(context, com.android.naptrap.location.LocationService::class.java)
                    intent.action = "STOP_ALARM"
                    context.startService(intent)
                }, modifier = Modifier.padding(start = 8.dp)) {
                    Text("Stop Alarm")
                }
            }
            // Manual entry UI
            androidx.compose.material3.Text(text = "Add destination manually:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            androidx.compose.material3.OutlinedTextField(
                value = manualName.value,
                onValueChange = { manualName.value = it },
                label = { Text("Name/Address") },
                modifier = Modifier.padding(top = 8.dp)
            )
            androidx.compose.material3.OutlinedTextField(
                value = manualLat.value,
                onValueChange = {
                    if (it.contains(",")) {
                        val parts = it.split(",")
                        if (parts.size == 2) {
                            manualLat.value = parts[0].trim()
                            manualLon.value = parts[1].trim()
                        } else {
                            manualLat.value = it
                        }
                    } else {
                        manualLat.value = it
                    }
                },
                label = { Text("Latitude (or paste lat,lon)") },
                modifier = Modifier.padding(top = 8.dp)
            )
            androidx.compose.material3.OutlinedTextField(
                value = manualLon.value,
                onValueChange = { manualLon.value = it },
                label = { Text("Longitude") },
                modifier = Modifier.padding(top = 8.dp)
            )
            if (manualError.value.isNotEmpty()) {
                Text(text = manualError.value, color = androidx.compose.ui.graphics.Color.Red, modifier = Modifier.padding(top = 4.dp))
            }
            Button(onClick = {
                try {
                    val lat = manualLat.value.trim().toDouble()
                    val lon = manualLon.value.trim().toDouble()
                    val name = manualName.value.trim().ifEmpty { "Manual Destination" }
                    viewModel.addDestination(com.android.naptrap.data.Destination(name = name, latitude = lat, longitude = lon))
                    manualLat.value = ""
                    manualLon.value = ""
                    manualName.value = ""
                    manualError.value = ""
                } catch (e: Exception) {
                    manualError.value = "Please enter valid latitude and longitude."
                }
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Add Destination")
            }
            LazyColumn {
                items(destinations) { destination ->
                    Card(
                        modifier = Modifier.padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        androidx.compose.foundation.layout.Row {
                            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                                Text(text = destination.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = "Lat: ${destination.latitude}, Lon: ${destination.longitude}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Button(onClick = { viewModel.removeDestination(destination) }, modifier = Modifier.padding(start = 8.dp)) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

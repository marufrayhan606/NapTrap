package com.android.naptrap.screens

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.android.naptrap.ui.theme.NapTrapTheme
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.naptrap.ui.DestinationViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val viewModel: DestinationViewModel = hiltViewModel()
    val destinations by viewModel.destinations.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadDestinations() }

    // Track which destinations are being tracked
    val trackedIds = remember { mutableStateOf(mutableSetOf<Int>()) }
    LaunchedEffect(destinations) {
        trackedIds.value.clear()
        destinations.forEach { dest ->
            if (dest.isTracked) trackedIds.value.add(dest.id)
        }
    }
    NapTrapTheme {
        val context = androidx.compose.ui.platform.LocalContext.current

        // Listen for untrack broadcast using DisposableEffect
        androidx.compose.runtime.DisposableEffect(destinations) {
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                    val destName = intent?.getStringExtra("destination_name")
                    val dest = destinations.find { it.name == destName }
                    if (dest != null) {
                        trackedIds.value.remove(dest.id)
                        viewModel.updateTracking(dest.id, false)
                        // Update LocationService
                        val serviceIntent = android.content.Intent(context, com.android.naptrap.location.LocationService::class.java)
                        serviceIntent.putIntegerArrayListExtra("tracked_ids", ArrayList(trackedIds.value))
                        if (trackedIds.value.isNotEmpty()) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                }
            }
            val filter = android.content.IntentFilter("com.android.naptrap.UNTRACK_DESTINATION")
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

    // ...existing code...
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("NapTrap") },
                    navigationIcon = { Text("🛌📍") }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { onNavigate("map") },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") },
                    text = { Text("Map") }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                Text(
                    "Your Destinations",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
                if (destinations.isEmpty()) {
                    Text(
                        "No destinations yet. Add one!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(destinations) { destination ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            destination.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Lat: ${destination.latitude}, Lon: ${destination.longitude}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    val isTracking = destination.isTracked
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    IconToggleButton(
                                        checked = isTracking,
                                        onCheckedChange = { checked ->
                                            viewModel.updateTracking(destination.id, checked)
                                            if (checked) {
                                                trackedIds.value.add(destination.id)
                                            } else {
                                                trackedIds.value.remove(destination.id)
                                            }
                                            val intent = android.content.Intent(context, com.android.naptrap.location.LocationService::class.java)
                                            intent.putIntegerArrayListExtra("tracked_ids", ArrayList(trackedIds.value))
                                            if (trackedIds.value.isNotEmpty()) {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                    context.startForegroundService(intent)
                                                } else {
                                                    context.startService(intent)
                                                }
                                            } else {
                                                context.startService(intent) // Send empty tracked_ids to stop tracking
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                if (isTracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = if (isTracking) "Tracking" else "Track",
                                            tint = if (isTracking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.removeDestination(destination) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Manual entry at bottom, floating style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { onNavigate("manual_entry") },
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = "Manual Entry") },
                        text = { Text("Add Destination Manually") },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


@Composable
fun AnimatedButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .padding(vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Text(text)
    }
}

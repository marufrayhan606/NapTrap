package com.android.naptrap.screens

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.android.naptrap.ui.theme.NapTrapTheme
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OnlinePrediction
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.naptrap.ui.DestinationViewModel
import com.android.naptrap.ui.theme.*


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

        // Modern gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Section
                ModernHeader()
                
                // Search Section
//                SearchSection()
                
                // Main Content Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Categories Header
                        Text(
                            text = "Your Destinations",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        if (destinations.isEmpty()) {
                            EmptyState(onNavigate)
                        } else {
                            DestinationsList(
                                destinations = destinations,
                                viewModel = viewModel,
                                trackedIds = trackedIds,
                                context = context
                            )
                        }
                    }
                }
            }
            
            // Floating Action Buttons
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Column {
                    FloatingActionButton(
                        onClick = { onNavigate("map") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Map")
                    }
                    
                    FloatingActionButton(
                        onClick = { onNavigate("manual_entry") },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Destination")
                    }
                }
            }
        }
    }
}


@Composable
fun ModernHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Find Your Route",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Track your destinations smartly",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            )
        }
    }
}

@Composable
fun SearchSection() {
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
                Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = "Search your route",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
fun EmptyState(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Illustration card
        Card(
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Text(
            text = "No destinations yet",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Add your first destination to start tracking your routes",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Button(
            onClick = { onNavigate("manual_entry") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Add Destination")
        }
    }
}

@Composable
fun DestinationsList(
    destinations: List<com.android.naptrap.data.Destination>,
    viewModel: DestinationViewModel,
    trackedIds: androidx.compose.runtime.MutableState<MutableSet<Int>>,
    context: android.content.Context
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item{
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(destinations) { destination ->
            ModernDestinationCard(
                destination = destination,
                viewModel = viewModel,
                trackedIds = trackedIds,
                context = context
            )
        }
    }
}

@Composable
fun ModernDestinationCard(
    destination: com.android.naptrap.data.Destination,
    viewModel: DestinationViewModel,
    trackedIds: androidx.compose.runtime.MutableState<MutableSet<Int>>,
    context: android.content.Context
) {
    val isTracking = destination.isTracked
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            // Content section
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTracking) "● Tracking" else "● Stopped",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isTracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
            
            // Action buttons
            Row {
                // Track/Stop button
                IconButton(
                    onClick = {
                        val checked = !isTracking
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
                            context.startService(intent)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isTracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
//                        imageVector = if (isTracking) Icons.Default.OnlinePrediction else Icons.Default.LocationOn,
                        imageVector = Icons.Default.OnlinePrediction,
                        contentDescription = if (isTracking) "Stop Tracking" else "Start Tracking",
                        tint = if (isTracking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Delete button
                IconButton(
                    onClick = { viewModel.removeDestination(destination) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Destination",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
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
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

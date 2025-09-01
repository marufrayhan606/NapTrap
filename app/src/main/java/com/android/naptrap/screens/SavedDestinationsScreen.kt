package com.android.naptrap.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.naptrap.data.Destination

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDestinationsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Saved Destinations") }, navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            })
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            val viewModel: com.android.naptrap.ui.DestinationViewModel = hiltViewModel()
            val destinations by viewModel.destinations.collectAsState()
            androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.loadDestinations() }

            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text("Saved Destinations", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                LazyColumn {
                    items(destinations) { destination ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(destination.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Lat: ${destination.latitude}, Lon: ${destination.longitude}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(onClick = { /* TODO: Track destination */ }, modifier = Modifier.padding(end = 8.dp)) {
                                Text("Track")
                            }
                            Button(onClick = { viewModel.removeDestination(destination) }) {
                                Text("Delete")
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

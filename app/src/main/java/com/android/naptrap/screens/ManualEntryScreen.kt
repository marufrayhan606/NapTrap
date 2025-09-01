package com.android.naptrap.screens


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Manual Entry") }, navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            })
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            // ManualEntry composable moved here
            val context = androidx.compose.ui.platform.LocalContext.current
            var name by remember { mutableStateOf("") }
            var latitude by remember { mutableStateOf("") }
            var longitude by remember { mutableStateOf("") }
            var error by remember { mutableStateOf("") }
            val viewModel: com.android.naptrap.ui.DestinationViewModel = androidx.hilt.navigation.compose.hiltViewModel()

            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text("Manual Entry", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Destination Name") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
                Button(
                    onClick = {
                        val lat = latitude.toDoubleOrNull()
                        val lon = longitude.toDoubleOrNull()
                        if (name.isBlank() || lat == null || lon == null) {
                            error = "Please enter a valid name, latitude, and longitude."
                        } else {
                            error = ""
                            viewModel.addDestination(
                                com.android.naptrap.data.Destination(
                                    name = name,
                                    latitude = lat,
                                    longitude = lon
                                )
                            )
                            android.widget.Toast.makeText(context, "Destination saved!", android.widget.Toast.LENGTH_SHORT).show()
                            name = ""
                            latitude = ""
                            longitude = ""
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Add Destination")
                }
            }
        }
    }
}

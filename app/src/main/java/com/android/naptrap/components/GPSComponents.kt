package com.android.naptrap.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.naptrap.utils.GPSUtils

@Composable
fun GPSStatusBanner() {
    val context = LocalContext.current
    var isGPSEnabled by remember { mutableStateOf(GPSUtils.isGPSEnabled(context)) }
    
    // GPS Enable launcher
    val gpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // Check GPS status after user interaction
        isGPSEnabled = GPSUtils.isGPSEnabled(context)
    }
    
    // Recheck GPS status periodically
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000) // Check every 2 seconds
        isGPSEnabled = GPSUtils.isGPSEnabled(context)
    }
    
    if (!isGPSEnabled) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = "GPS Disabled",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GPS is disabled",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Location tracking requires GPS to be enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (context is androidx.activity.ComponentActivity) {
                            GPSUtils.promptEnableGPSWithLauncher(
                                activity = context,
                                launcher = gpsLauncher,
                                onSuccess = {
                                    isGPSEnabled = true
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(
                        text = "Enable",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GPSStatusIndicator() {
    val context = LocalContext.current
    var isGPSEnabled by remember { mutableStateOf(GPSUtils.isGPSEnabled(context)) }
    
    // Recheck GPS status periodically
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000) // Check every 3 seconds
            isGPSEnabled = GPSUtils.isGPSEnabled(context)
        }
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = if (isGPSEnabled) Icons.Default.LocationOff else Icons.Default.Warning,
            contentDescription = if (isGPSEnabled) "GPS Enabled" else "GPS Disabled",
            tint = if (isGPSEnabled) Color.Green else Color.Red,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isGPSEnabled) "GPS On" else "GPS Off",
            style = MaterialTheme.typography.bodySmall,
            color = if (isGPSEnabled) Color.Green else Color.Red,
            fontWeight = FontWeight.Medium
        )
    }
}

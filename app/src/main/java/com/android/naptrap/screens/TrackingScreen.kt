package com.android.naptrap.screens

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
fun TrackingScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tracking Controls") }, navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            })
        }
    ) { padding ->
//        TrackingControls()
    }
}

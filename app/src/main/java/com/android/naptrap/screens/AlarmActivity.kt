package com.android.naptrap.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.naptrap.ui.theme.NapTrapTheme

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make activity appear above lock screen and other apps
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        setContent {
            NapTrapTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.errorContainer) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Wake up! Approaching destination!", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(onClick = {
                                // Send intent to stop alarm
                                val stopIntent = Intent(this@AlarmActivity, com.android.naptrap.location.LocationService::class.java)
                                stopIntent.action = "STOP_ALARM"
                                startService(stopIntent)
                                // Broadcast to untrack destination
                                val destName = intent.getStringExtra("destination_name")
                                val untrackIntent = Intent("com.android.naptrap.UNTRACK_DESTINATION")
                                untrackIntent.putExtra("destination_name", destName)
                                sendBroadcast(untrackIntent)
                                finish()
                            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                Text("Stop Alarm", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

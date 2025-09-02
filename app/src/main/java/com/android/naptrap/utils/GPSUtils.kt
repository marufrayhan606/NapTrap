package com.android.naptrap.utils

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

object GPSUtils {
    
    /**
     * Check if GPS is enabled on the device
     */
    fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Prompt user to enable GPS using Google Play Services Location Settings API
     */
    fun promptEnableGPS(
        activity: Activity,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()
        
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Important: this forces the dialog to show
            .build()
        
        val settingsClient = LocationServices.getSettingsClient(activity)
        val task = settingsClient.checkLocationSettings(locationSettingsRequest)
        
        task.addOnSuccessListener {
            // GPS is already enabled
            onSuccess()
        }
        
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Show the dialog to enable GPS
                    exception.startResolutionForResult(activity, GPS_ENABLE_REQUEST_CODE)
                } catch (sendEx: IntentSender.SendIntentException) {
                    onFailure(sendEx)
                }
            } else {
                onFailure(exception)
            }
        }
    }
    
    /**
     * Prompt user to enable GPS using Activity Result API (for Compose)
     */
    fun promptEnableGPSWithLauncher(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()
        
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()
        
        val settingsClient = LocationServices.getSettingsClient(activity)
        val task = settingsClient.checkLocationSettings(locationSettingsRequest)
        
        task.addOnSuccessListener {
            // GPS is already enabled
            onSuccess()
        }
        
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    launcher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    onFailure(sendEx)
                }
            } else {
                onFailure(exception)
            }
        }
    }
    
    const val GPS_ENABLE_REQUEST_CODE = 1001
}

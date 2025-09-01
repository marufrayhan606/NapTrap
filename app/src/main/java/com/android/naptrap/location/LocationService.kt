package com.android.naptrap.location

import android.Manifest
import android.app.Service
import android.content.Intent
import android.location.Location
import android.media.MediaPlayer
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.naptrap.R
import com.android.naptrap.data.AppDatabase
import com.android.naptrap.data.Destination
import androidx.room.Room
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.Vibrator
import android.os.VibrationEffect
import android.media.RingtoneManager
import android.media.Ringtone
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var db: AppDatabase
    private val proximityThreshold = 200.0 // meters
    private val triggeredDestinations = mutableSetOf<Int>() // Track by destination id
    private var mediaPlayer: MediaPlayer? = null
    private var trackedIds: Set<Int> = emptySet()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "naptrap_db").build()
        createNotificationChannel()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    checkProximity(location)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "naptrap_channel",
                "NapTrap Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always start foreground immediately
        val notification = NotificationCompat.Builder(this, "naptrap_channel")
            .setContentTitle("NapTrap Running")
            .setContentText("Tracking your destinations")
            .setSmallIcon(com.android.naptrap.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }

        if (intent?.action == "STOP_ALARM") {
            stopAlarmSound()
            return START_NOT_STICKY
        }
        // Get tracked IDs from intent
        val ids = intent?.getIntegerArrayListExtra("tracked_ids")
        trackedIds = ids?.toSet() ?: emptySet()
        if (trackedIds.isEmpty()) {
            // No destinations to track, stop location updates and service
            fusedLocationClient.removeLocationUpdates(locationCallback)
            stopSelf()
            return START_NOT_STICKY
        }
        startLocationUpdates()
        return START_STICKY
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun checkProximity(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            val allDestinations = db.destinationDao().getAllDestinations()
            val destinations = allDestinations.filter { trackedIds.contains(it.id) }
            for (dest in destinations) {
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, dest.latitude, dest.longitude, results)
                if (results[0] < proximityThreshold && !triggeredDestinations.contains(dest.id)) {
                    triggeredDestinations.add(dest.id)
                    triggerAlarm(dest.name)
                }
            }
        }
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    private fun triggerAlarm(destinationName: String) {
        // Notification with Stop Alarm action
        val stopIntent = Intent(this, LocationService::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this,
            0,
            stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) android.app.PendingIntent.FLAG_MUTABLE else 0
        )
        val notification = NotificationCompat.Builder(this, "naptrap_channel")
            .setContentTitle("Approaching destination!")
            .setContentText("You are near $destinationName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "Stop Alarm", stopPendingIntent)
            .build()
        NotificationManagerCompat.from(this).notify(destinationName.hashCode(), notification)

        // Vibration
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                it.vibrate(1000)
            }
        }

        // Alarm sound
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(this, alarmUri)
            mediaPlayer?.setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
            mediaPlayer?.isLooping = false
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Fallback to notification sound
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone?.play()
        }

        // Launch full-screen alarm activity
        val alarmIntent = Intent(this, com.android.naptrap.screens.AlarmActivity::class.java)
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        alarmIntent.action = "com.android.naptrap.ALARM_SCREEN"
        alarmIntent.putExtra("destination_name", destinationName)
        startActivity(alarmIntent)
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
    fusedLocationClient.removeLocationUpdates(locationCallback)
    stopAlarmSound()
    super.onDestroy()
    }
}

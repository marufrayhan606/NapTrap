package com.android.naptrap.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.naptrap.ui.theme.NapTrapTheme
import com.android.naptrap.ui.theme.*
import com.android.naptrap.data.AppDatabase
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make activity appear above lock screen and other apps
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Hide system UI for true fullscreen experience
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        val destinationName = intent.getStringExtra("destination_name") ?: "your destination"
        
        setContent {
            NapTrapTheme {
                MindBlowingAlarmScreen(
                    destinationName = destinationName,
                    onStopAlarm = {
                        stopAlarmAndFinish()
                    }
                )
            }
        }
    }
    
    private fun stopAlarmAndFinish() {
        val destName = intent.getStringExtra("destination_name") ?: ""
        
        // Stop alarm sound immediately
        val stopIntent = Intent(this, com.android.naptrap.location.LocationService::class.java)
        stopIntent.action = "STOP_ALARM"
        startService(stopIntent)
        
        // Update database to disable tracking for this destination
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(
                    applicationContext, 
                    AppDatabase::class.java, 
                    "naptrap_db"
                ).build()
                
                // Find and update the destination to stop tracking
                val destinations = db.destinationDao().getAllDestinations()
                val destination = destinations.find { it.name == destName }
                
                destination?.let { dest ->
                    // Update destination to stop tracking
                    db.destinationDao().updateTracking(dest.id, false)
                    
                    // Send broadcast to update UI and stop tracking
                    val untrackIntent = Intent("com.android.naptrap.UNTRACK_DESTINATION")
                    untrackIntent.putExtra("destination_name", destName)
                    untrackIntent.putExtra("destination_id", dest.id)
                    sendBroadcast(untrackIntent)
                    
                    // Stop the location service completely if no destinations are being tracked
                    val remainingTrackedDestinations = db.destinationDao().getAllDestinations()
                        .filter { it.isTracked }
                    
                    if (remainingTrackedDestinations.isEmpty()) {
                        // Stop location service completely
                        val stopServiceIntent = Intent(this@AlarmActivity, com.android.naptrap.location.LocationService::class.java)
                        stopService(stopServiceIntent)
                    } else {
                        // Update service with remaining tracked destinations
                        val updateServiceIntent = Intent(this@AlarmActivity, com.android.naptrap.location.LocationService::class.java)
                        val remainingIds = remainingTrackedDestinations.map { it.id }
                        updateServiceIntent.putIntegerArrayListExtra("tracked_ids", ArrayList(remainingIds))
                        startService(updateServiceIntent)
                    }
                }
                
                // Close the alarm activity
                runOnUiThread {
                    finish()
                }
                
            } catch (e: Exception) {
                // Fallback: just send broadcast and finish
                val untrackIntent = Intent("com.android.naptrap.UNTRACK_DESTINATION")
                untrackIntent.putExtra("destination_name", destName)
                sendBroadcast(untrackIntent)
                
                runOnUiThread {
                    finish()
                }
            }
        }
    }
}

@Composable
fun MindBlowingAlarmScreen(
    destinationName: String,
    onStopAlarm: () -> Unit
) {
    var isAnimating by remember { mutableStateOf(true) }
    
    // Infinite animations
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_animation")
    
    // Pulsing effect
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Color animation
    val backgroundColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF1A1A2E),
        targetValue = Color(0xFF16213E),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_color"
    )
    
    // Breathing text effect
    val textScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_scale"
    )
    
    // Particle system state
    var particles by remember { mutableStateOf(generateParticles()) }
    
    LaunchedEffect(Unit) {
        while (isAnimating) {
            delay(50)
            particles = particles.map { it.update() }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        backgroundColor,
                        Color(0xFF0F3460),
                        Color(0xFF533483),
                        Color(0xFFE94560)
                    ),
                    radius = 1200f
                )
            )
    ) {
        // Particle system background
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            particles.forEach { particle ->
                drawParticle(particle)
            }
            
            // Draw animated rings
            drawAnimatedRings(rotation, size)
        }
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated alarm icon with glow effect
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect layers
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size((160 + index * 20).dp)
                            .background(
                                Color.Red.copy(alpha = 0.1f - index * 0.03f),
                                CircleShape
                            )
                            .blur((10 + index * 5).dp)
                    )
                }
                
                // Main icon
                Card(
                    modifier = Modifier
                        .size(160.dp)
                        .rotate(rotation / 4),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(60.dp))
            
            // Animated wake up text
            Text(
                text = "⚡ WAKE UP! ⚡",
                style = TextStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = Color.Red.copy(alpha = 0.8f),
                        offset = Offset(4f, 4f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier.scale(textScale)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Destination text with typewriter effect
            Text(
                text = "📍 Approaching $destinationName",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Additional info text
            Text(
                text = "Stopping the alarm will automatically\ndisable tracking for this destination",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Interactive stop button with slide to stop
            SlideToStopButton(onStopAlarm = onStopAlarm)
        }
        
        // Floating action elements
        FloatingActionElements(modifier = Modifier.fillMaxSize())
    }
}

// Data class for particles
data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val alpha: Float,
    val life: Float
) {
    fun update(): Particle {
        return copy(
            x = x + vx,
            y = y + vy,
            alpha = (alpha - 0.01f).coerceAtLeast(0f),
            life = life - 0.01f
        )
    }
}

fun generateParticles(): List<Particle> {
    return (0..50).map {
        Particle(
            x = Random.nextFloat() * 1000,
            y = Random.nextFloat() * 2000,
            vx = Random.nextFloat() * 2 - 1,
            vy = Random.nextFloat() * 2 - 1,
            size = Random.nextFloat() * 4 + 2,
            color = listOf(Color.Red, Color.Yellow, Color.Magenta, Color.White).random(),
            alpha = Random.nextFloat() * 0.8f + 0.2f,
            life = 1f
        )
    }
}

fun DrawScope.drawParticle(particle: Particle) {
    if (particle.alpha > 0f && particle.life > 0f) {
        drawCircle(
            color = particle.color.copy(alpha = particle.alpha),
            radius = particle.size,
            center = Offset(particle.x % size.width, particle.y % size.height)
        )
    }
}

fun DrawScope.drawAnimatedRings(rotation: Float, canvasSize: androidx.compose.ui.geometry.Size) {
    val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
    
    // Draw multiple rotating rings
    repeat(3) { ring ->
        val radius = 100f + ring * 50f
        val strokeWidth = 3f + ring * 2f
        val alpha = 0.3f - ring * 0.1f
        
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(20f, 10f),
                    phase = rotation + ring * 45f
                )
            )
        )
    }
}

@Composable
fun SlideToStopButton(onStopAlarm: () -> Unit) {
    var isSliding by remember { mutableStateOf(false) }
    var slideOffset by remember { mutableStateOf(0f) }
    var isStopping by remember { mutableStateOf(false) }
    val maxSlideDistance = 200.dp
    val maxSlideDistancePx = with(LocalDensity.current) { maxSlideDistance.toPx() }
    
    val buttonColor by animateColorAsState(
        targetValue = when {
            isStopping -> Color(0xFF4CAF50) // Green when stopping
            isSliding -> Color(0xFFFF9800) // Orange when sliding
            else -> Color(0xFFF44336) // Red by default
        },
        animationSpec = tween(300),
        label = "button_color"
    )
    
    val backgroundText = when {
        isStopping -> "Stopping Alarm & Disabling Tracking..."
        slideOffset >= maxSlideDistancePx * 0.6f -> "Release to Stop & Disable Tracking"
        else -> "Slide to Stop Alarm & Disable Tracking"
    }
    
    Box(
        modifier = Modifier
            .width(350.dp)
            .height(80.dp)
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(40.dp)
            )
            .border(
                2.dp,
                Color.White.copy(alpha = 0.3f),
                RoundedCornerShape(40.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Background text
        Text(
            text = backgroundText,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f)
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        // Sliding button
        Box(
            modifier = Modifier
                .offset(x = with(LocalDensity.current) { slideOffset.toDp() })
                .size(70.dp)
                .background(buttonColor, CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { 
                            if (!isStopping) {
                                isSliding = true 
                            }
                        },
                        onDragEnd = {
                            if (!isStopping && slideOffset >= maxSlideDistancePx * 0.8f) {
                                isStopping = true
                                onStopAlarm()
                            } else if (!isStopping) {
                                slideOffset = 0f
                                isSliding = false
                            }
                        }
                    ) { _, dragAmount ->
                        if (!isStopping) {
                            slideOffset = (slideOffset + dragAmount.x)
                                .coerceIn(0f, maxSlideDistancePx)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                isStopping -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                }
                slideOffset >= maxSlideDistancePx * 0.8f -> {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingActionElements(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_elements")
    
    val float1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )
    
    val float2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )
    
    Box(modifier = modifier) {
        // Floating emoji elements
        Text(
            text = "⏰",
            fontSize = 40.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 50.dp, y = (100 + float1).dp)
                .alpha(0.6f)
        )
        
        Text(
            text = "🚨",
            fontSize = 35.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-80).dp, y = (150 + float2).dp)
                .alpha(0.7f)
        )
        
        Text(
            text = "📍",
            fontSize = 30.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = (-200 + float1).dp)
                .alpha(0.5f)
        )
        
        Text(
            text = "⚡",
            fontSize = 45.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-60).dp, y = (-250 + float2).dp)
                .alpha(0.8f)
        )
    }
}

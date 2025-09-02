package com.android.naptrap.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.android.naptrap.auth.AuthState
import com.android.naptrap.auth.AuthViewModel

import com.android.naptrap.screens.HomeScreen
import com.android.naptrap.screens.LoginScreen
import com.android.naptrap.screens.MapScreen
import com.android.naptrap.screens.ManualEntryScreen
import com.android.naptrap.screens.ProfileScreen
import com.android.naptrap.screens.SavedDestinationsScreen
import com.android.naptrap.screens.SignUpScreen

object Routes {
    const val LOADING = "loading"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
    const val MAP = "map"
    const val MANUAL_ENTRY = "manual_entry"
    const val SAVED_DESTINATIONS = "saved_destinations"
    const val PROFILE = "profile"
    const val TRACKING = "tracking"
    const val ALARM = "alarm"
}


@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    
    // Always start with loading screen
    NavHost(navController = navController, startDestination = Routes.LOADING) {
        
        // Loading screen that determines where to go
        composable(Routes.LOADING) {
            LaunchedEffect(authState) {
                when (authState) {
                    is AuthState.Authenticated -> {
                        // Small delay to prevent flash and show splash
                        kotlinx.coroutines.delay(300)
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOADING) { inclusive = true }
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        // Small delay to prevent flash and show splash
                        kotlinx.coroutines.delay(300)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOADING) { inclusive = true }
                        }
                    }
                    is AuthState.Error -> {
                        // Small delay to prevent flash and show splash
                        kotlinx.coroutines.delay(300)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOADING) { inclusive = true }
                        }
                    }
                    // For AuthState.Loading, stay on loading screen
                    AuthState.Loading -> {
                        // Stay on loading screen until auth state is determined
                    }
                }
            }
            
            // Splash Screen UI
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
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NapTrap",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        // Authentication screens
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Routes.SIGNUP) },
                onLoginSuccess = { 
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.SIGNUP) {
            SignUpScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignUpSuccess = { 
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        // Main app screens (protected)
        composable(Routes.HOME) {
            HomeScreen { route -> 
                if (route == "profile") {
                    navController.navigate(Routes.PROFILE)
                } else {
                    navController.navigate(route)
                }
            }
        }
        
        composable(Routes.MAP) {
            MapScreen { navController.popBackStack() }
        }
        
        composable(Routes.MANUAL_ENTRY) {
            ManualEntryScreen { navController.popBackStack() }
        }
        
        composable(Routes.SAVED_DESTINATIONS) {
            SavedDestinationsScreen { navController.popBackStack() }
        }
        
        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

package com.android.naptrap.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.android.naptrap.screens.HomeScreen
import com.android.naptrap.screens.MapScreen
import com.android.naptrap.screens.ManualEntryScreen
import com.android.naptrap.screens.SavedDestinationsScreen

object Routes {
    const val HOME = "home"
    const val MAP = "map"
    const val MANUAL_ENTRY = "manual_entry"
    const val SAVED_DESTINATIONS = "saved_destinations"
    const val TRACKING = "tracking"
    const val ALARM = "alarm"
}


@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen { route -> navController.navigate(route) }
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
    }
}

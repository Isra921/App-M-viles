package com.escom.escomorienta.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.escom.escomorienta.ui.screens.HomeScreen
import com.escom.escomorienta.ui.screens.MapScreen
import com.escom.escomorienta.ui.screens.ProceduresScreen
import com.escom.escomorienta.ui.screens.ProcedureDetailScreen
import com.escom.escomorienta.ui.screens.ScheduleScreen
import com.escom.escomorienta.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val MAP = "map"
    const val MAP_QUERY_ARGUMENT = "query"
    const val MAP_WITH_QUERY = "map?query={query}"
    const val MAP_ROUTE_ORIGIN_ARGUMENT = "origin"
    const val MAP_ROUTE_DESTINATION_ARGUMENT = "destination"
    const val MAP_WITH_ROUTE = "map-route?origin={origin}&destination={destination}"
    const val SCHEDULE = "schedule"
    const val PROCEDURES = "procedures"
    const val PROCEDURE_ID_ARGUMENT = "resourceId"
    const val PROCEDURE_DETAIL = "procedures/{resourceId}"
    const val SETTINGS = "settings"

    fun procedureDetail(resourceId: String) = "procedures/${Uri.encode(resourceId)}"
    fun mapLocation(mapLocationId: String) = "map?query=${Uri.encode(mapLocationId)}"

    fun mapRoute(origin: String, destination: String): String {
        return "map-route?origin=${Uri.encode(origin)}&destination=${Uri.encode(destination)}"
    }
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Inicio", Routes.HOME, Icons.Filled.Home),
    BottomNavItem("Mapa", Routes.MAP, Icons.Filled.Map),
    BottomNavItem("Horario", Routes.SCHEDULE, Icons.Filled.Schedule),
    BottomNavItem("Trámites", Routes.PROCEDURES, Icons.Filled.Description)
)

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(180)) }
    ) {
        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }

        composable(Routes.MAP) {
            MapScreen(navController = navController)
        }

        composable(
            route = Routes.MAP_WITH_QUERY,
            arguments = listOf(
                navArgument(Routes.MAP_QUERY_ARGUMENT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            MapScreen(
                navController = navController,
                initialQuery = backStackEntry.arguments?.getString(Routes.MAP_QUERY_ARGUMENT)
            )
        }

        composable(
            route = Routes.MAP_WITH_ROUTE,
            arguments = listOf(
                navArgument(Routes.MAP_ROUTE_ORIGIN_ARGUMENT) {
                    type = NavType.StringType
                },
                navArgument(Routes.MAP_ROUTE_DESTINATION_ARGUMENT) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            MapScreen(
                navController = navController,
                initialRouteOrigin = backStackEntry.arguments
                    ?.getString(Routes.MAP_ROUTE_ORIGIN_ARGUMENT),
                initialRouteDestination = backStackEntry.arguments
                    ?.getString(Routes.MAP_ROUTE_DESTINATION_ARGUMENT)
            )
        }


        composable(Routes.SCHEDULE) {
            ScheduleScreen(navController = navController)
        }


        composable(Routes.PROCEDURES) {
            ProceduresScreen(navController = navController)
        }

        composable(
            Routes.PROCEDURE_DETAIL,
            arguments = listOf(navArgument(Routes.PROCEDURE_ID_ARGUMENT) { type = NavType.StringType })
        ) { backStackEntry ->
            ProcedureDetailScreen(
                navController,
                backStackEntry.arguments?.getString(Routes.PROCEDURE_ID_ARGUMENT).orEmpty()
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
    }
}

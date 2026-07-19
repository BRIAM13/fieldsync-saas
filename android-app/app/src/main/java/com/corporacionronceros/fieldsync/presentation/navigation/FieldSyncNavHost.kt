package com.corporacionronceros.fieldsync.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.corporacionronceros.fieldsync.presentation.detail.TaskDetailScreen
import com.corporacionronceros.fieldsync.presentation.tasks.TasksScreen

/** Grafo de navegación de la app: lista de órdenes → detalle de una orden. */
@Composable
fun FieldSyncNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Tasks.route) {

        composable(Screen.Tasks.route) {
            TasksScreen(
                onOrderClick = { orderId ->
                    navController.navigate(Screen.TaskDetail.routeFor(orderId))
                }
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument(Screen.TaskDetail.ARG_ORDER_ID) {
                type = NavType.StringType
            })
        ) {
            // El ViewModel lee el argumento vía SavedStateHandle (Hilt lo provee).
            TaskDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}

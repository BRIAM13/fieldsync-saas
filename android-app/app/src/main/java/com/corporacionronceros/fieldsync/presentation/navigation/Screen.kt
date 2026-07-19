package com.corporacionronceros.fieldsync.presentation.navigation

/** Rutas type-safe de la app. El detalle recibe el id de la orden como argumento. */
sealed class Screen(val route: String) {
    data object Tasks : Screen("tasks")

    data object TaskDetail : Screen("tasks/{orderId}") {
        const val ARG_ORDER_ID = "orderId"
        fun routeFor(orderId: String) = "tasks/$orderId"
    }
}

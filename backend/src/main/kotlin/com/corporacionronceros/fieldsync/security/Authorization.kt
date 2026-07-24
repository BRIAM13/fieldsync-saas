package com.corporacionronceros.fieldsync.security

import com.corporacionronceros.fieldsync.model.ApiError
import com.corporacionronceros.fieldsync.model.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

/** Rol del portador del token, o null si el claim falta / es inválido. */
fun ApplicationCall.role(): UserRole? =
    principal<JWTPrincipal>()?.payload?.getClaim(JwtService.CLAIM_ROLE)?.asString()
        ?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }

/** true si el claim `role` del token es literalmente "CUSTOMER" (no es un [UserRole]). */
private fun ApplicationCall.isCustomerRole(): Boolean =
    principal<JWTPrincipal>()?.payload?.getClaim(JwtService.CLAIM_ROLE)?.asString() == "CUSTOMER"

private class AuthorizedRouteSelector(private val description: String) : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
        RouteSelectorEvaluation.Transparent

    override fun toString() = "(authorize: $description)"
}

/**
 * Restringe las rutas hijas a los roles indicados (RBAC). Debe usarse **dentro** de un
 * bloque `authenticate` (necesita el principal). Un rol no permitido recibe 403.
 */
fun Route.authorize(vararg roles: UserRole, build: Route.() -> Unit): Route {
    val route = createChild(AuthorizedRouteSelector(roles.joinToString()))
    // Fase Call (no Plugins): el plugin de Authentication resuelve el JWTPrincipal en esa
    // fase. Interceptar en Plugins corre ANTES de esa resolución — call.role() vería siempre
    // null y todo el mundo recibiría 403, sin importar su rol real.
    route.intercept(ApplicationCallPipeline.Call) {
        val role = call.role()
        if (role == null || role !in roles) {
            call.respond(
                HttpStatusCode.Forbidden,
                ApiError("Tu rol no tiene permiso para esta acción")
            )
            finish()
        }
    }
    route.build()
    return route
}

/**
 * Defensa en profundidad para rutas de cliente: la barrera principal ya es el proveedor
 * `AUTH_JWT_CUSTOMER` (un token de staff nunca autentica ahí, ver `Security.kt`), pero se
 * chequea también el claim `role` explícitamente, siguiendo el mismo hábito que [authorize].
 */
fun Route.authorizeCustomer(build: Route.() -> Unit): Route {
    val route = createChild(AuthorizedRouteSelector("CUSTOMER"))
    route.intercept(ApplicationCallPipeline.Call) {
        if (!call.isCustomerRole()) {
            call.respond(
                HttpStatusCode.Forbidden,
                ApiError("Tu rol no tiene permiso para esta acción")
            )
            finish()
        }
    }
    route.build()
    return route
}

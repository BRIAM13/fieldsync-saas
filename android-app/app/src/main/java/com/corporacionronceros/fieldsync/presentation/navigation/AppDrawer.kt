package com.corporacionronceros.fieldsync.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Contenido del drawer de navegación: cabecera con la sesión activa (nombre, email, empresa),
 * accesos a las secciones de la app y cierre de sesión.
 */
@Composable
fun AppDrawerContent(
    userName: String,
    userEmail: String,
    companyName: String,
    onOrdersClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(userName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (userEmail.isNotBlank()) {
                Text(userEmail, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(companyName, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }

        Spacer(Modifier.height(8.dp))

        NavigationDrawerItem(
            label = { Text("Mis órdenes") },
            selected = true,
            icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
            onClick = onOrdersClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
            label = { Text("Historial") },
            selected = false,
            icon = { Icon(Icons.Filled.History, contentDescription = null) },
            onClick = onHistoryClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text("Cerrar sesión") },
            selected = false,
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            onClick = onLogoutClick,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedIconColor = MaterialTheme.colorScheme.error,
                unselectedTextColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

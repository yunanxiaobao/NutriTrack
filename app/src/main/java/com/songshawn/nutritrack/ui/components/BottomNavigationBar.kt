package com.songshawn.nutritrack.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.songshawn.nutritrack.navigation.Routes

/**
 * Bottom navigation bar component for app-wide navigation
 *
 * @param currentRoute Current active route
 * @param navController Navigation controller to handle navigation between destinations
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    navController: NavController
) {
    // Define navigation item data structure for better type safety
    data class NavigationItem(
        val label: String,
        val route: String,
        val icon: ImageVector
    )

    // Define navigation items
    val navigationItems = listOf(
        NavigationItem("Home", Routes.HOME_GRAPH, Icons.Outlined.Home),
        NavigationItem("Insights", Routes.INSIGHTS, Icons.Outlined.Search),
        NavigationItem("NutriCoach", Routes.NUTRICOACH, Icons.Outlined.Face),
        NavigationItem("Settings", Routes.SETTINGS, Icons.Outlined.Settings)
    )

    NavigationBar(modifier = Modifier.fillMaxWidth()) {
        navigationItems.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                label = { Text(item.label) },
                selected = isSelected,
                enabled = !isSelected, // Disable current selected item
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            // Prevent multiple copies of the same destination
                            launchSingleTop = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                }
            )
        }
    }
}
package com.songshawn.nutritrack.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.songshawn.nutritrack.R
import com.songshawn.nutritrack.navigation.Routes
import com.songshawn.nutritrack.ui.components.BottomNavigationBar
import com.songshawn.nutritrack.util.DemoDataConfig
import com.songshawn.nutritrack.viewmodel.SettingsViewModel
import com.songshawn.nutritrack.worker.DailyMealReminderWorker

/**
 * Settings screen showing app settings and configuration options
 *
 * @param userId ID of the current user
 * @param navController Navigation controller for screen transitions
 * @param viewModel ViewModel for settings functionality
 */
@Composable
fun SettingsScreen(
    userId: String,
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    Scaffold(
        bottomBar = {
            val currentRoute = navController
                .currentBackStackEntry
                ?.destination
                ?.route
                ?: ""
            BottomNavigationBar(
                currentRoute = currentRoute,
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            SettingsHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // Settings items
            SettingsItemsList(navController)
        }
    }
}

/**
 * Settings screen header
 */
@Composable
private fun SettingsHeader() {
    Text(
        text = "Settings",
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily(Font(R.font.poppins)),
        fontSize = 30.sp,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

/**
 * List of settings items
 */
@Composable
private fun SettingsItemsList(navController: NavController) {
    val context = LocalContext.current
    var reminderEnabled by remember {
        mutableStateOf(DailyMealReminderWorker.isReminderEnabled(context))
    }

    SettingItem(
        title = "Account",
        icon = Icons.Outlined.AccountCircle,
        onClick = { navController.navigate(Routes.ACCOUNT) }
    )

    SettingItem(
        title = "Scores",
        icon = Icons.Outlined.Search,
        onClick = { navController.navigate(Routes.INSIGHTS) }
    )

    SettingSwitchItem(
        title = "Daily Meal Reminder",
        checked = reminderEnabled,
        onCheckedChange = { enabled ->
            reminderEnabled = enabled
            DailyMealReminderWorker.setReminderEnabled(context, enabled)
        }
    )

    SettingItem(
        title = "About",
        icon = Icons.Outlined.Info,
        onClick = { navController.navigate(Routes.ABOUT) }
    )

    if (DemoDataConfig.isEnabled) {
        SettingItem(
            title = "Clinician Dashboard (Demo Cohort)",
            icon = Icons.Outlined.AccountBox,
            onClick = { navController.navigate(Routes.CLINICIAN) }
        )
    }
}

/**
 * Individual setting item component
 *
 * @param title Title of the setting
 * @param icon Icon for the setting
 * @param onClick Callback when item is clicked
 */
@Composable
fun SettingItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate to $title",
                tint = Color.Gray
            )
        }
    }
}

/**
 * Setting item with a switch for toggle options
 */
@Composable
fun SettingSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(2.dp, RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
package com.songshawn.nutritrack.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.songshawn.nutritrack.R

/**
 * About screen showing app and developer information
 *
 * @param userId ID of the current user
 * @param navController Navigation controller for screen transitions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    userId: String,
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        AboutContent(padding = innerPadding)
    }
}

/**
 * Main content for the about screen
 */
@Composable
private fun AboutContent(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        // App name and version
        AppNameAndVersion()

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Contact information
        ContactInformation()

        Spacer(modifier = Modifier.height(32.dp))

        // Copyright information
        CopyrightInfo()
    }
}

/**
 * App name and version section
 */
@Composable
private fun AppNameAndVersion() {
    Text(
        text = stringResource(R.string.app_name),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF4C7040)
    )

    Text(
        text = stringResource(R.string.aboutScreen_version),
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Gray,
        modifier = Modifier.padding(top = 4.dp)
    )
}

/**
 * Contact information section
 */
@Composable
private fun ContactInformation() {
    AboutInfoItem(
        title = "Address",
        content = stringResource(R.string.aboutScreen_address)
    )

    AboutInfoItem(
        title = "Support Email",
        content = stringResource(R.string.aboutScreen_supportEmail)
    )

    AboutInfoItem(
        title = "Hours",
        content = stringResource(R.string.aboutScreen_hours)
    )
}

/**
 * Copyright information section
 */
@Composable
private fun CopyrightInfo() {
    Column (
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.aboutScreen_copyrightInfo),
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * Individual about information item
 */
@Composable
fun AboutInfoItem(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        Text(
            text = content,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
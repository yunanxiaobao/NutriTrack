package com.songshawn.nutritrack.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.songshawn.nutritrack.navigation.Routes
import com.songshawn.nutritrack.ui.components.BottomNavigationBar
import com.songshawn.nutritrack.viewmodel.ClinicianViewModel

/**
 * Admin login screen for clinician access
 *
 * @param userId ID of the current user
 * @param navController Navigation controller for screen transitions
 * @param viewModel ViewModel for clinician functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
    userId: String,
    navController: NavController,
    viewModel: ClinicianViewModel = hiltViewModel()
) {
    // Observe login state from ViewModel
    val isLoginError by viewModel.isLoginError.observeAsState(false)
    val loginErrorMessage by viewModel.loginErrorMessage.observeAsState("")
    val isLoginSuccessful by viewModel.isLoginSuccessful.observeAsState(false)

    // Password input state
    var password by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Handle successful login navigation
    LaunchedEffect(isLoginSuccessful) {
        if (isLoginSuccessful) {
            navController.navigate(Routes.CLINICIAN)
            // Reset login state to avoid returning to successful state
            viewModel.resetLoginState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Login") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
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
        AdminLoginContent(
            password = password,
            onPasswordChange = {
                password = it
                // Clear error state
                if (isLoginError) {
                    viewModel.resetLoginState()
                }
            },
            isLoginError = isLoginError,
            loginErrorMessage = loginErrorMessage,
            onLoginClick = { viewModel.verifyAdminPassword(password) },
            focusRequester = focusRequester,
            padding = innerPadding
        )
    }

    // Request focus to password field when screen appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * Main content for admin login screen
 */
@Composable
private fun AdminLoginContent(
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoginError: Boolean,
    loginErrorMessage: String,
    onLoginClick: () -> Unit,
    focusRequester: FocusRequester,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Enter Admin Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Password input field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password"
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
            isError = isLoginError,
            supportingText = {
                if (isLoginError) {
                    Text(
                        text = loginErrorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login button
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text("Login", fontSize = 16.sp)
        }
    }
}
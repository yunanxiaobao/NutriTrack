package com.haoshuang_34517812.nutritrack.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.haoshuang_34517812.nutritrack.R
import com.haoshuang_34517812.nutritrack.data.models.UserInfo
import com.haoshuang_34517812.nutritrack.navigation.Routes
import com.haoshuang_34517812.nutritrack.theme.MediumGreen
import com.haoshuang_34517812.nutritrack.util.AuthenticationManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.haoshuang_34517812.nutritrack.viewmodel.SettingsUiState
import com.haoshuang_34517812.nutritrack.viewmodel.SettingsViewModel

/**
 * Account screen showing user information and account options
 *
 * @param navController Navigation controller for screen transitions
 * @param viewModel ViewModel for account functionality
 * @param userId ID of the current user, obtained from AuthenticationManager
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    userId: String? = AuthenticationManager.getCurrentUserId()
) {
    val userInfo by viewModel.userInfo.observeAsState()
    val uiState by viewModel.uiState.observeAsState()

    // Dialog states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editField by remember { mutableStateOf("") }
    var editValue by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Handle not logged in case
    if (userId == null) {
        SideEffect {
            navController.navigate(Routes.LOGIN) {
                launchSingleTop = true
            }
        }
        return
    }

    // Load user information
    DisposableEffect(userId) {
        viewModel.setUserId(userId)
        onDispose {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AccountContent(
            userInfo = userInfo,
            showLogoutDialog = { showLogoutDialog = true },
            showDeleteDialog = { showDeleteDialog = true },
            showEditDialog = { field, value ->
                editField = field
                editValue = value
                showEditDialog = true
            },
            padding = innerPadding
        )
    }

    // Edit dialog
    if (showEditDialog) {
        EditDialog(
            field = editField,
            value = editValue,
            onValueChange = { editValue = it },
            onConfirm = {
                if (editValue.isNotBlank()) {
                    when (editField.lowercase()) {
                        "name" -> viewModel.updateUserInfo(name = editValue)
                        "phone", "phone number" -> viewModel.updateUserInfo(phone = editValue)
                    }
                    showEditDialog = false
                }
            },
            onDismiss = { showEditDialog = false }
        )
    }

    // Navigate away only once the account is actually deleted
    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.AccountDeleted) {
            showDeleteDialog = false
            navController.navigate(Routes.LOGIN) {
                launchSingleTop = true
                popUpTo(Routes.ACCOUNT) { inclusive = true }
            }
        }
    }

    // Delete account dialog (Firebase deletion needs password re-authentication)
    if (showDeleteDialog) {
        DeleteAccountDialog(
            errorMessage = (uiState as? SettingsUiState.Error)?.message,
            isDeleting = uiState is SettingsUiState.Loading,
            onConfirm = { password -> viewModel.deleteAccount(password) },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Logout dialog
    if (showLogoutDialog) {
        ConfirmDialog(
            title = "Logout?",
            text = stringResource(R.string.accountScreen_logoutMsg),
            onConfirm = {
                viewModel.logout(context)
                showLogoutDialog = false
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
            },
            onDismiss = { showLogoutDialog = false },
            confirmButtonText = "Logout",
            confirmButtonColor = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Main content of account screen
 */
@Composable
private fun AccountContent(
    userInfo: UserInfo?,
    showLogoutDialog: () -> Unit,
    showDeleteDialog: () -> Unit,
    showEditDialog: (String, String) -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        userInfo?.let { info ->
            // User info items
            AccountInfoItem("User ID", info.userId)
            Spacer(modifier = Modifier.height(12.dp))

            AccountInfoItem("Name", info.name) {
                showEditDialog("Name", info.name)
            }
            Spacer(modifier = Modifier.height(12.dp))

            AccountInfoItem("Sex", info.sex)
            Spacer(modifier = Modifier.height(12.dp))

            AccountInfoItem("Phone Number", info.phoneNumber) {
                showEditDialog("Phone Number", info.phoneNumber)
            }
        }

        Spacer(modifier = Modifier.height(42.dp))

        // Action buttons
        AccountActionButtons(
            onLogoutClick = showLogoutDialog,
            onDeleteClick = showDeleteDialog
        )
    }
}

/**
 * Account action buttons (logout and delete)
 */
@Composable
private fun AccountActionButtons(
    onLogoutClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Button(
        onClick = onLogoutClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MediumGreen,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text("Logout", fontSize = 16.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onDeleteClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text("Delete Account", fontSize = 16.sp)
    }
}

/**
 * Individual account information item
 */
@Composable
fun AccountInfoItem(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * Dialog for editing account information
 */
@Composable
fun EditDialog(
    field: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $field") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Confirmation dialog for destructive actions
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String,
    dismissButtonText: String = "Cancel",
    confirmButtonColor: Color = MaterialTheme.colorScheme.primary
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmButtonColor)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText)
            }
        }
    )
}
/**
 * Password-confirmation dialog for permanent account deletion.
 */
@Composable
private fun DeleteAccountDialog(
    errorMessage: String?,
    isDeleting: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account?") },
        text = {
            Column {
                Text(
                    stringResource(R.string.accountScreen_deleteAccountMsg) +
                        " This permanently deletes your cloud data as well. Enter your password to confirm."
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isDeleting,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank() && !isDeleting
            ) {
                Text(if (isDeleting) "Deleting..." else "Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Cancel")
            }
        }
    )
}

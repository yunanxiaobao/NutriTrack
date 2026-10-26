package com.haoshuang_34517812.nutritrack.navigation

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.haoshuang_34517812.nutritrack.ui.screens.*
import com.haoshuang_34517812.nutritrack.ui.screens.questionnaire.QuestionnaireScreen
import com.haoshuang_34517812.nutritrack.ui.screens.settings.*
import com.haoshuang_34517812.nutritrack.util.AuthenticationManager

/**
 * Main navigation component for NutriTrack app
 * Handles routing between screens and authentication state management
 */
@Composable
fun NutriTrackNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onContinue = {
                    // Returning users with a live Firebase session skip login.
                    val userId = AuthenticationManager.getCurrentUserId()
                    when {
                        userId == null -> navController.navigate(Routes.LOGIN)
                        AuthenticationManager.hasCompletedQuestionnaire(userId) ->
                            navController.navigate(Routes.HOME_GRAPH) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        else ->
                            navController.navigate(Routes.QUESTIONNAIRE) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { userId ->
                    val hasCompletedQuestionnaire = AuthenticationManager.hasCompletedQuestionnaire(userId)
                    // Navigate based on questionnaire completion status
                    if (hasCompletedQuestionnaire) {
                        navController.navigate(Routes.HOME_GRAPH) {
                            launchSingleTop = true
                            // Clear login page from back stack
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.QUESTIONNAIRE) {
                            launchSingleTop = true
                            // Clear login page from back stack
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onGoRegister = {
                    navController.navigate(Routes.REGISTER)
                },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { userId ->
                    // Registration signs in immediately; new users onboard first.
                    if (AuthenticationManager.hasCompletedQuestionnaire(userId)) {
                        navController.navigate(Routes.HOME_GRAPH) {
                            launchSingleTop = true
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.QUESTIONNAIRE) {
                            launchSingleTop = true
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onGoLogin = {
                    navController.navigate(Routes.LOGIN) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.QUESTIONNAIRE) {
            val userId = AuthenticationManager.getCurrentUserId()

            if (userId == null) {
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
                return@composable
            }

            QuestionnaireScreen(
                userId = userId,
                onComplete = {
                    navController.navigate(Routes.HOME_GRAPH) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.HOME_GRAPH) {
            val userId = AuthenticationManager.getCurrentUserId()

            if (userId == null) {
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
                return@composable
            }

            HomeScreen(userId, navController)
        }

        composable(Routes.INSIGHTS) {
            val userId = AuthenticationManager.getCurrentUserId()

            if (userId == null) {
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
                return@composable
            }

            InsightsScreen(userId, navController)
        }

        composable(Routes.NUTRICOACH) {
            val userId = AuthenticationManager.getCurrentUserId()

            if (userId == null) {
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
                return@composable
            }

            NutriCoachScreen(userId, navController)
        }

        composable(Routes.SETTINGS) {
            val userId = AuthenticationManager.getCurrentUserId()

            if (userId == null) {
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
                return@composable
            }

            SettingsScreen(userId, navController)
        }

        composable(Routes.ACCOUNT) {
            val userId = AuthenticationManager.getCurrentUserId()

            if (userId == null) {
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
                return@composable
            }

            AccountScreen(navController)
        }

        composable(Routes.ABOUT) {
            val userId = AuthenticationManager.getCurrentUserId()

            if (userId == null) {
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
                return@composable
            }

            AboutScreen(userId, navController)
        }

        composable(Routes.ADMIN_LOGIN) {
            val userId = AuthenticationManager.getCurrentUserId()

            if (userId == null) {
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
                return@composable
            }

            AdminLoginScreen(userId, navController)
        }

        composable(Routes.CLINICIAN) {
            val userId = AuthenticationManager.getCurrentUserId()

            if (userId == null) {
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
                return@composable
            }

            ClinicianScreen(userId, navController)
        }
    }
}
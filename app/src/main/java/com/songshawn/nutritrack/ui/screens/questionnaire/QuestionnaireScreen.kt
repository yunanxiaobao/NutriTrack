package com.songshawn.nutritrack.ui.screens.questionnaire

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.songshawn.nutritrack.R
import com.songshawn.nutritrack.data.models.FoodCategory
import com.songshawn.nutritrack.data.models.Persona
import com.songshawn.nutritrack.data.models.QuestionnaireStep
import com.songshawn.nutritrack.theme.MediumGreen
import com.songshawn.nutritrack.viewmodel.QuestionnaireViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.songshawn.nutritrack.theme.LightGrey
import kotlinx.coroutines.launch

/**
 * Main questionnaire screen that manages the multi-step form process using HorizontalPager
 *
 * @param userId ID of the current user
 * @param goBack Callback for navigating back
 * @param onComplete Callback for when questionnaire is completed
 * @param viewModel ViewModel for questionnaire data and logic
 */
@Composable
fun QuestionnaireScreen(
    userId: String,
    onComplete: () -> Unit,
    viewModel: QuestionnaireViewModel = hiltViewModel()
) {
    // Collect states from ViewModel
    val categories by viewModel.selectedCategories.collectAsState()
    val persona by viewModel.selectedPersona.observeAsState()
    val mealTime by viewModel.mealTime.observeAsState("12:00")
    val sleepTime by viewModel.sleepTime.observeAsState("22:00")
    val wakeTime by viewModel.wakeTime.observeAsState("07:00")
    val steps = QuestionnaireStep.entries.toList()

    // Setup Pager State
    val pagerState = rememberPagerState(
        pageCount = { steps.size }
    )

    // Set current step in ViewModel when page changes
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentStep(pagerState.currentPage)
    }

    // Coroutine scope for animations and page changes
    val coroutineScope = rememberCoroutineScope()

    // Load existing data
    LaunchedEffect(userId) {
        viewModel.loadExisting(userId)
    }

    // Manage error messages with Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val error by viewModel.errorMessage.observeAsState()

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            ErrorSnackbarHost(snackbarHostState)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fixed header for all pages
                Text(
                    text = "Let us know more about you...",
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily(Font(R.font.poppins)),
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp)
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Main Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (steps[page]) {
                            QuestionnaireStep.FOOD_CATEGORIES -> {
                                FoodCategoriesStepContent(
                                    selected = categories.toSet(),
                                    onToggleCategory = { viewModel.toggleCategory(it) }
                                )
                            }
                            QuestionnaireStep.PERSONA -> {
                                PersonaStepContent(
                                    selectedPersona = persona,
                                    onPersonaSelected = {
                                        if (it != null) {
                                            viewModel.setPersona(it)
                                        }
                                    }
                                )
                            }
                            QuestionnaireStep.TIMINGS -> {
                                TimingsStepContent(
                                    mealTime = mealTime,
                                    onMealTimeChange = { viewModel.setMealTime(it) },
                                    sleepTime = sleepTime,
                                    onSleepTimeChange = { viewModel.setSleepTime(it) },
                                    wakeTime = wakeTime,
                                    onWakeTimeChange = { viewModel.setWakeTime(it) }
                                )
                            }
                        }
                    }
                }

                // Page Indicators - Now below the content
                PageIndicators(
                    pageCount = steps.size,
                    currentPage = pagerState.currentPage,
                    onPageSelected = { page ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    },
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                )
            }

            // Navigation Arrows
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Back Arrow - Only show if not on first page
                if (pagerState.currentPage > 0) {
                    NavigationButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        modifier = Modifier
                            .align(Alignment.BottomStart),
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    )
                }

                // Forward Arrow or Save Button
                if (pagerState.currentPage < steps.size - 1) {
                    // Regular forward arrow
                    NavigationButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        modifier = Modifier
                            .align(Alignment.BottomEnd),
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    )
                } else {
                    // Save button on last page
                    NavigationButton(
                        icon = Icons.Default.Check,
                        contentDescription = "Save",
                        modifier = Modifier
                            .align(Alignment.BottomEnd),
                        onClick = {
                            // Validate and save
                            viewModel.validateAllSteps {
                                viewModel.onSaveRequested(userId, onComplete)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Custom snackbar host for error messages
 */
@Composable
private fun ErrorSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.visuals.message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = { data.dismiss() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * Navigation button for moving between pages
 */
@Composable
private fun NavigationButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(LightGrey.copy(alpha = 0.7f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MediumGreen,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Page indicators shown at the bottom of the screen
 */
@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { page ->
            val isSelected = page == currentPage
            val size = animateFloatAsState(
                targetValue = if (isSelected) 14f else 10f,
                label = "indicatorSize"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(size.value.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MediumGreen else Color.Gray.copy(alpha = 0.5f))
                    .clickable { onPageSelected(page) }
            )
        }
    }
}

/**
 * Content for Food Categories step
 */
@Composable
private fun FoodCategoriesStepContent(
    selected: Set<FoodCategory>,
    onToggleCategory: (FoodCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        FoodCategoriesPage(
            selected = selected,
            onToggleCategory = onToggleCategory,
        )
    }
}

/**
 * Content for Persona step
 */
@Composable
private fun PersonaStepContent(
    selectedPersona: Persona?,
    onPersonaSelected: (Persona?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        PersonaPage(
            selectedPersona = selectedPersona,
            onPersonaSelected = onPersonaSelected,
        )
    }
}

/**
 * Content for Timings step
 */
@Composable
private fun TimingsStepContent(
    mealTime: String,
    onMealTimeChange: (String) -> Unit,
    sleepTime: String,
    onSleepTimeChange: (String) -> Unit,
    wakeTime: String,
    onWakeTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TimingsPage(
            mealTime = mealTime,
            onMealTimeChange = onMealTimeChange,
            sleepTime = sleepTime,
            onSleepTimeChange = onSleepTimeChange,
            wakeTime = wakeTime,
            onWakeTimeChange = onWakeTimeChange,
        )
    }
}
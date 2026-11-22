package com.songshawn.nutritrack.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.songshawn.nutritrack.R
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import com.songshawn.nutritrack.data.room.entity.QuestionnaireInfoEntity
import com.songshawn.nutritrack.data.room.entity.toScoreList
import com.songshawn.nutritrack.theme.LightGreen
import com.songshawn.nutritrack.theme.LightGrey
import com.songshawn.nutritrack.theme.MediumGreen
import com.songshawn.nutritrack.theme.SoftGreen
import com.songshawn.nutritrack.ui.components.BottomNavigationBar
import com.songshawn.nutritrack.ui.components.GenAIInsightComponent
import com.songshawn.nutritrack.viewmodel.ApiResult
import com.songshawn.nutritrack.viewmodel.ClinicianViewModel
import com.songshawn.nutritrack.viewmodel.GenAIViewModel
import java.text.DecimalFormat

/**
 * Screen displaying admin statistics and patient data for clinicians.
 * Provides visualizations of population health data and AI-powered insights.
 * Features adaptive layout for different orientations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicianScreen(
    userId: String,
    navController: NavController,
    viewModel: ClinicianViewModel = hiltViewModel(),
    genAiViewModel: GenAIViewModel = hiltViewModel()
) {
    // Collect statistics data from StateFlow
    val maleAverageScore by viewModel.maleAverageScore.collectAsStateWithLifecycle()
    val femaleAverageScore by viewModel.femaleAverageScore.collectAsStateWithLifecycle()
    val maleCount by viewModel.maleCount.collectAsStateWithLifecycle()
    val femaleCount by viewModel.femaleCount.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val registeredPatients by viewModel.registeredPatients.collectAsStateWithLifecycle()
    val registeredCount by viewModel.registeredUserCount.collectAsStateWithLifecycle()

    // Collect AI generation results
    val aiResult by genAiViewModel.state.collectAsState()

    // UI state
    val scrollState = rememberScrollState()
    val formatter = DecimalFormat("#.##")
    var userListExpanded by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<PatientEntity?>(null) }
    var showUserDialog by remember { mutableStateOf(false) }

    // Check current screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Reset AI state when leaving the screen
    DisposableEffect(Unit) {
        onDispose { genAiViewModel.reset() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.clinician_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = navController.currentBackStackEntry?.destination?.route ?: "",
                navController = navController
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Loading indicator
            AnimatedVisibility(isLoading, Modifier.align(Alignment.Center)) {
                CircularProgressIndicator(
                    color = MediumGreen,
                    modifier = Modifier.size(if (isLandscape) 48.dp else 64.dp)
                )
            }

            // Main content - render different layouts based on orientation
            AnimatedVisibility(!isLoading) {
                if (isLandscape) {
                    LandscapeClinicianLayout(
                        maleAverageScore = maleAverageScore,
                        femaleAverageScore = femaleAverageScore,
                        maleCount = maleCount,
                        femaleCount = femaleCount,
                        registeredCount = registeredCount,
                        registeredPatients = registeredPatients,
                        userListExpanded = userListExpanded,
                        onExpandChange = { userListExpanded = it },
                        onUserClick = { user ->
                            selectedUser = user
                            showUserDialog = true
                        },
                        aiResult = aiResult,
                        genAiViewModel = genAiViewModel,
                        formatter = formatter,
                        scrollState = scrollState
                    )
                } else {
                    PortraitClinicianLayout(
                        maleAverageScore = maleAverageScore,
                        femaleAverageScore = femaleAverageScore,
                        maleCount = maleCount,
                        femaleCount = femaleCount,
                        registeredCount = registeredCount,
                        registeredPatients = registeredPatients,
                        userListExpanded = userListExpanded,
                        onExpandChange = { userListExpanded = it },
                        onUserClick = { user ->
                            selectedUser = user
                            showUserDialog = true
                        },
                        aiResult = aiResult,
                        genAiViewModel = genAiViewModel,
                        formatter = formatter,
                        scrollState = scrollState
                    )
                }
            }
        }
    }

    // User details dialog
    if (showUserDialog) {
        selectedUser?.let { user ->
            val questionnaireInfo by viewModel.getUserQuestionnaireInfo(user.userId)
                .collectAsState(initial = null)

            UserDetailsDialog(
                user = user,
                questionnaireInfo = questionnaireInfo,
                onDismiss = {
                    showUserDialog = false
                    selectedUser = null
                },
                isLandscape = isLandscape
            )
        }
    }
}

/**
 * Portrait layout for the clinician screen - vertical arrangement
 */
@Composable
private fun PortraitClinicianLayout(
    maleAverageScore: Double,
    femaleAverageScore: Double,
    maleCount: Int,
    femaleCount: Int,
    registeredCount: Int,
    registeredPatients: List<PatientEntity>,
    userListExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onUserClick: (PatientEntity) -> Unit,
    aiResult: ApiResult<String>,
    genAiViewModel: GenAIViewModel,
    formatter: DecimalFormat,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        ScreenHeader()

        // Calculate overall statistics
        val totalUsers = maleCount + femaleCount
        val overallScore = if (totalUsers > 0)
            formatter.format(
                (maleAverageScore * maleCount + femaleAverageScore * femaleCount) /
                        totalUsers
            ).toDouble()
        else 0.0

        // Display statistics cards
        GeneralStatisticCard(
            totalUsers = totalUsers,
            registeredUsers = registeredCount,
            averageScore = overallScore,
            formatter = formatter
        )

        Spacer(Modifier.height(16.dp))

        // Gender statistics cards (side by side)
        GenderStatisticsRow(
            maleCount = maleCount,
            femaleCount = femaleCount,
            maleAverageScore = maleAverageScore,
            femaleAverageScore = femaleAverageScore,
            formatter = formatter
        )

        Spacer(Modifier.height(16.dp))

        // User list expandable card
        UserListCard(
            expanded = userListExpanded,
            onExpandChange = onExpandChange,
            patients = registeredPatients,
            onUserClick = onUserClick
        )

        Spacer(Modifier.height(16.dp))

        // AI insights card
        GenAIInsightComponent(
            totalUsers = totalUsers,
            maleCount = maleCount,
            femaleCount = femaleCount,
            maleAverageScore = maleAverageScore,
            femaleAverageScore = femaleAverageScore,
            aiResult = aiResult,
            genAiViewModel = genAiViewModel,
            title = stringResource(R.string.ai_insights_title),
            promptPlaceholder = stringResource(R.string.clinicianScreen_insightsInput)
        )

        Spacer(Modifier.height(16.dp))

        FooterNoteText()

        // Bottom spacing to ensure content is fully visible when scrolling
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Landscape layout for the clinician screen - two-column arrangement
 */
@Composable
private fun LandscapeClinicianLayout(
    maleAverageScore: Double,
    femaleAverageScore: Double,
    maleCount: Int,
    femaleCount: Int,
    registeredCount: Int,
    registeredPatients: List<PatientEntity>,
    userListExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onUserClick: (PatientEntity) -> Unit,
    aiResult: ApiResult<String>,
    genAiViewModel: GenAIViewModel,
    formatter: DecimalFormat,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Left side: Header, statistics cards, user list
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with adjusted sizing for landscape
            ScreenHeader(isLandscape = true)

            // Calculate overall statistics
            val totalUsers = maleCount + femaleCount
            val overallScore = if (totalUsers > 0)
                formatter.format(
                    (maleAverageScore * maleCount + femaleAverageScore * femaleCount) /
                            totalUsers
                ).toDouble()
            else 0.0

            // Display statistics cards
            GeneralStatisticCard(
                totalUsers = totalUsers,
                registeredUsers = registeredCount,
                averageScore = overallScore,
                formatter = formatter,
                isLandscape = true
            )

            Spacer(Modifier.height(12.dp))

            // Gender statistics cards (vertical arrangement in landscape)
            GenderStatisticsColumn(
                maleCount = maleCount,
                femaleCount = femaleCount,
                maleAverageScore = maleAverageScore,
                femaleAverageScore = femaleAverageScore,
                formatter = formatter
            )

            Spacer(Modifier.height(12.dp))

            // User list expandable card
            UserListCard(
                expanded = userListExpanded,
                onExpandChange = onExpandChange,
                patients = registeredPatients,
                onUserClick = onUserClick,
                isLandscape = true
            )
        }

        // Right side: AI insights and footer
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
                .verticalScroll(scrollState)
        ) {
            val totalUsers = maleCount + femaleCount
            // AI insights card
            GenAIInsightComponent(
                totalUsers = totalUsers,
                maleCount = maleCount,
                femaleCount = femaleCount,
                maleAverageScore = maleAverageScore,
                femaleAverageScore = femaleAverageScore,
                aiResult = aiResult,
                genAiViewModel = genAiViewModel,
                title = stringResource(R.string.ai_insights_title),
                promptPlaceholder = stringResource(R.string.clinicianScreen_insightsInput)
            )

            Spacer(Modifier.height(16.dp))

            FooterNoteText(isLandscape = true)

            // Bottom spacing
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Displays the screen title header
 * Adapts font size based on orientation
 */
@Composable
private fun ScreenHeader(isLandscape: Boolean = false) {
    // Adjust font size and padding for landscape
    val fontSize = if (isLandscape) 24.sp else 28.sp
    val verticalPadding = if (isLandscape) 8.dp else 16.dp

    Text(
        text = stringResource(R.string.clinician_header_title),
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily(Font(R.font.poppins)),
        fontSize = fontSize,
        modifier = Modifier.padding(vertical = verticalPadding)
    )
}

/**
 * Displays a footer note about HEIFA scores
 * Adapts font size based on orientation
 */
@Composable
private fun FooterNoteText(isLandscape: Boolean = false) {
    // Adjust font size for landscape
    val fontSize = if (isLandscape) 12.sp else 14.sp

    Text(
        text = stringResource(R.string.clinicianScreen_note),
        fontSize = fontSize,
        color = Color.Gray,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

/**
 * Displays gender statistics cards in a row (for portrait)
 */
@Composable
private fun GenderStatisticsRow(
    maleCount: Int,
    femaleCount: Int,
    maleAverageScore: Double,
    femaleAverageScore: Double,
    formatter: DecimalFormat
) {
    Row(Modifier.fillMaxWidth()) {
        GenderStatisticCard(
            title = stringResource(R.string.male_users),
            count = maleCount,
            averageScore = maleAverageScore,
            formatter = formatter,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(16.dp))

        GenderStatisticCard(
            title = stringResource(R.string.female_users),
            count = femaleCount,
            averageScore = femaleAverageScore,
            formatter = formatter,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Displays gender statistics cards in a column (for landscape)
 */
@Composable
private fun GenderStatisticsColumn(
    maleCount: Int,
    femaleCount: Int,
    maleAverageScore: Double,
    femaleAverageScore: Double,
    formatter: DecimalFormat
) {
    Column(Modifier.fillMaxWidth()) {
        GenderStatisticCard(
            title = stringResource(R.string.male_users),
            count = maleCount,
            averageScore = maleAverageScore,
            formatter = formatter,
            modifier = Modifier.fillMaxWidth(),
            isLandscape = true
        )

        Spacer(Modifier.height(12.dp))

        GenderStatisticCard(
            title = stringResource(R.string.female_users),
            count = femaleCount,
            averageScore = femaleAverageScore,
            formatter = formatter,
            modifier = Modifier.fillMaxWidth(),
            isLandscape = true
        )
    }
}


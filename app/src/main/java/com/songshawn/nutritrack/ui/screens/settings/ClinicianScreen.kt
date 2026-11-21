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
import androidx.compose.runtime.livedata.observeAsState
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
    // Collect statistics data from LiveData
    val maleAverageScore by viewModel.maleAverageScore.observeAsState(0.0)
    val femaleAverageScore by viewModel.femaleAverageScore.observeAsState(0.0)
    val maleCount by viewModel.maleCount.observeAsState(0)
    val femaleCount by viewModel.femaleCount.observeAsState(0)
    val isLoading by viewModel.isLoading.observeAsState(true)
    val registeredPatients by viewModel.registeredPatients.observeAsState(emptyList())
    val registeredCount by viewModel.registeredUserCount.observeAsState(0)

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

/**
 * Card displaying general statistics about all users
 * Adapts layout based on orientation
 */
@Composable
fun GeneralStatisticCard(
    totalUsers: Int,
    registeredUsers: Int,
    averageScore: Double,
    formatter: DecimalFormat,
    isLandscape: Boolean = false
) {
    // Adjust padding and font sizes for landscape
    val cardPadding = if (isLandscape) 12.dp else 16.dp
    val titleFontSize = if (isLandscape) 18.sp else 20.sp
    val numberFontSize = if (isLandscape) 20.sp else 24.sp
    val labelFontSize = if (isLandscape) 12.sp else 14.sp

    // Create animated score value
    val animatedScore = remember { Animatable(0f) }

    // Start animation
    LaunchedEffect(averageScore) {
        animatedScore.animateTo(
            targetValue = averageScore.toFloat(),
            animationSpec = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Calculate percentage for progress bar
    val progress = (animatedScore.value.coerceIn(0f, 100f) / 100f)

    // Display text changes with animation
    val displayScore = if (totalUsers > 0)
        formatter.format(animatedScore.value)
    else "N/A"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LightGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            Text(
                text = stringResource(R.string.general_statistics),
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = if (isLandscape) 12.dp else 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.total_users),
                        fontSize = labelFontSize,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "$totalUsers",
                        fontSize = numberFontSize,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.registered_users),
                        fontSize = labelFontSize,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "$registeredUsers",
                        fontSize = numberFontSize,
                        fontWeight = FontWeight.Bold
                    )
                }

                ScoreCircle(
                    score = displayScore,
                    progress = progress,
                    showLabel = totalUsers > 0,
                    isLandscape = isLandscape
                )
            }
        }
    }
}

/**
 * Displays a circular progress indicator with score
 * Adapts size based on orientation
 */
@Composable
private fun ScoreCircle(
    score: String,
    progress: Float,
    showLabel: Boolean,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Adjust sizes for landscape
    val circleSize = if (isLandscape) 70.dp else 90.dp
    val strokeSize = if (isLandscape) 60.dp else 80.dp
    val strokeWidth = if (isLandscape) 6.dp else 8.dp
    val scoreFontSize = if (isLandscape) 16.sp else 18.sp
    val labelFontSize = if (isLandscape) 8.sp else 10.sp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(circleSize)
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(strokeSize),
            strokeWidth = strokeWidth,
            color = SoftGreen,
            strokeCap = StrokeCap.Round,
            trackColor = Color.LightGray.copy(alpha = 0.3f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = score,
                fontSize = scoreFontSize,
                fontWeight = FontWeight.Bold
            )

            if (showLabel) {
                Text(
                    text = stringResource(R.string.score_label),
                    fontSize = labelFontSize,
                    color = Color.DarkGray
                )
            }
        }
    }
}

/**
 * Card displaying gender-specific statistics
 * Adapts layout based on orientation
 */
@Composable
fun GenderStatisticCard(
    title: String,
    count: Int,
    averageScore: Double,
    formatter: DecimalFormat,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    // Adjust sizing for landscape
    val cardPadding = if (isLandscape) 12.dp else 16.dp
    val titleFontSize = if (isLandscape) 16.sp else 18.sp
    val countFontSize = if (isLandscape) 12.sp else 14.sp
    val circleSize = if (isLandscape) 60.dp else 80.dp

    // Create animated score value
    val animatedScore = remember { Animatable(0f) }

    // Start animation
    LaunchedEffect(averageScore) {
        animatedScore.animateTo(
            targetValue = averageScore.toFloat(),
            animationSpec = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Display text changes with animation
    val displayScore = if (count > 0)
        formatter.format(animatedScore.value)
    else "N/A"

    // Calculate percentage for progress bar
    val progress = (animatedScore.value.coerceIn(0f, 100f) / 100f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = LightGreen)
    ) {
        if (isLandscape) {
            // Horizontal layout for landscape
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.count_label, count),
                        fontSize = countFontSize,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                ScoreCircle(
                    score = displayScore,
                    progress = progress,
                    showLabel = count > 0,
                    modifier = Modifier.size(circleSize)
                )
            }
        } else {
            // Vertical layout for portrait
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.count_label, count),
                    fontSize = countFontSize,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ScoreCircle(
                    score = displayScore,
                    progress = progress,
                    showLabel = count > 0,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(circleSize)
                )
            }
        }
    }
}

/**
 * Expandable card displaying the list of registered users
 * Adapts layout based on orientation
 */
@Composable
fun UserListCard(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    patients: List<PatientEntity>,
    onUserClick: (PatientEntity) -> Unit,
    isLandscape: Boolean = false
) {
    // Adjust padding for landscape
    val cardPadding = if (isLandscape) 12.dp else 16.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        colors = CardDefaults.cardColors(containerColor = LightGrey)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            // Title and expand/collapse button
            UserListHeader(
                expanded = expanded,
                onExpandChange = onExpandChange,
                isLandscape = isLandscape
            )

            // Conditional content - only shown when expanded
            if (expanded) {
                Spacer(Modifier.height(if (isLandscape) 12.dp else 16.dp))

                if (patients.isEmpty()) {
                    EmptyUserList(isLandscape = isLandscape)
                } else {
                    UserListContent(
                        patients = patients,
                        onUserClick = onUserClick,
                        isLandscape = isLandscape
                    )
                }
            }
        }
    }
}

/**
 * Header for the user list card with expandable controls
 * Adapts font size based on orientation
 */
@Composable
private fun UserListHeader(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    isLandscape: Boolean = false
) {
    // Adjust font size for landscape
    val fontSize = if (isLandscape) 16.sp else 18.sp
    val iconSize = if (isLandscape) 20.dp else 24.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandChange(!expanded) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.registered_users),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )

        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Displays a message when the user list is empty
 * Adapts font size based on orientation
 */
@Composable
private fun EmptyUserList(isLandscape: Boolean = false) {
    // Adjust font size for landscape
    val fontSize = if (isLandscape) 12.sp else 14.sp

    Text(
        text = stringResource(R.string.no_registered_users),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
        color = Color.Gray,
        fontSize = fontSize
    )
}

/**
 * Displays the content of the user list
 * Adapts layout based on orientation
 */
@Composable
private fun UserListContent(
    patients: List<PatientEntity>,
    onUserClick: (PatientEntity) -> Unit,
    isLandscape: Boolean = false
) {
    // Adjust font size for landscape
    val headerFontSize = if (isLandscape) 12.sp else 14.sp

    // Table header row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.user_id_header),
            fontSize = headerFontSize,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = stringResource(R.string.username_header),
            fontSize = headerFontSize,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }

    // Divider
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        thickness = 1.dp,
        color = Color.LightGray
    )

    // User list
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        patients.forEach { patient ->
            UserListItem(
                user = patient,
                onClick = { onUserClick(patient) },
                isLandscape = isLandscape
            )
        }
    }
}

/**
 * Displays a single user list item
 * Adapts font size based on orientation
 */
@Composable
fun UserListItem(
    user: PatientEntity,
    onClick: () -> Unit,
    isLandscape: Boolean = false
) {
    // Adjust font size for landscape
    val fontSize = if (isLandscape) 12.sp else 14.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = user.userId,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = user.name,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Dialog displaying detailed information about a selected user
 * Adapts layout based on orientation
 */
@Composable
fun UserDetailsDialog(
    user: PatientEntity,
    questionnaireInfo: QuestionnaireInfoEntity?,
    onDismiss: () -> Unit,
    isLandscape: Boolean = false
) {
    // Adjust sizing for landscape
    val dialogPadding = if (isLandscape) 12.dp else 16.dp
    val contentPadding = if (isLandscape) 16.dp else 24.dp
    val cornerRadius = if (isLandscape) 16.dp else 24.dp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .let { modifier ->
                    if (isLandscape) {
                        modifier.fillMaxHeight(0.9f)
                    } else {
                        modifier
                    }
                }
                .padding(dialogPadding)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            ) {
                // Title and close button
                DialogHeader(
                    title = stringResource(R.string.user_details_title),
                    onDismiss = onDismiss,
                    isLandscape = isLandscape
                )

                Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 16.dp))

                if (isLandscape) {
                    // Horizontal layout for landscape
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left column: Personal and score info
                        Column(modifier = Modifier.weight(1f)) {
                            UserPersonalInfo(user)

                            SectionDivider()

                            UserScoreInfo(user)
                        }

                        // Right column: Questionnaire info
                        Column(modifier = Modifier.weight(1f)) {
                            if (questionnaireInfo != null) {
                                UserQuestionnaireInfo(questionnaireInfo)
                            } else {
                                NoQuestionnaireInfo()
                            }
                        }
                    }
                } else {
                    // Vertical layout for portrait
                    UserPersonalInfo(user)

                    SectionDivider()

                    UserScoreInfo(user)

                    if (questionnaireInfo != null) {
                        SectionDivider()
                        UserQuestionnaireInfo(questionnaireInfo)
                    } else {
                        SectionDivider()
                        NoQuestionnaireInfo()
                    }
                }
            }
        }
    }
}

/**
 * Displays the dialog header with title and close button
 * Adapts sizing based on orientation
 */
@Composable
private fun DialogHeader(
    title: String,
    onDismiss: () -> Unit,
    isLandscape: Boolean = false
) {
    // Adjust sizing for landscape
    val titleFontSize = if (isLandscape) 18.sp else 22.sp
    val buttonSize = if (isLandscape) 28.dp else 32.dp
    val iconSize = if (isLandscape) 14.dp else 16.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.poppins)),
            modifier = Modifier.align(Alignment.Center)
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(buttonSize)
                .background(
                    color = LightGrey.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                modifier = Modifier.size(iconSize),
                tint = Color.DarkGray
            )
        }
    }
}

/**
 * Displays user's personal information
 * Adapts font size based on orientation
 */
@Composable
private fun UserPersonalInfo(user: PatientEntity, isLandscape: Boolean = false) {
    SectionTitle(stringResource(R.string.personal_information), isLandscape)

    DetailItem(stringResource(R.string.user_id_label), user.userId, isLandscape)
    DetailItem(stringResource(R.string.name_label), user.name, isLandscape)
    DetailItem(stringResource(R.string.phone_label), user.phoneNumber, isLandscape)
    DetailItem(stringResource(R.string.gender_label), user.sex?.name ?: stringResource(R.string.not_specified), isLandscape)
}

/**
 * Displays user's HEIFA score information
 * Adapts font size based on orientation
 */
@Composable
private fun UserScoreInfo(user: PatientEntity, isLandscape: Boolean = false) {
    SectionTitle(stringResource(R.string.heifa_scores), isLandscape)

    DetailItem(stringResource(R.string.total_score_label), String.format("%.2f", user.heifaTotalScore), isLandscape)

    // List all scores
    val scoreList = user.toScoreList()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isLandscape) 6.dp else 8.dp)
    ) {
        scoreList.forEach { (name, score) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isLandscape) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    fontSize = if (isLandscape) 12.sp else 14.sp,
                    color = Color.DarkGray,
                    fontFamily = FontFamily(Font(R.font.poppins))
                )
                Text(
                    text = String.format("%.2f", score),
                    fontSize = if (isLandscape) 12.sp else 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily(Font(R.font.poppins))
                )
            }
        }
    }
}

/**
 * Displays user's questionnaire information
 * Adapts font size based on orientation
 */
@Composable
private fun UserQuestionnaireInfo(questionnaireInfo: QuestionnaireInfoEntity, isLandscape: Boolean = false) {
    SectionTitle(stringResource(R.string.questionnaire_information), isLandscape)

    DetailItem(stringResource(R.string.biggest_meal_time), questionnaireInfo.biggestMealTime, isLandscape)
    DetailItem(stringResource(R.string.sleep_time), questionnaireInfo.sleepTime, isLandscape)
    DetailItem(stringResource(R.string.wake_time), questionnaireInfo.wakeTime, isLandscape)
    DetailItem(stringResource(R.string.persona), questionnaireInfo.persona.toString(), isLandscape)

    // Food categories
    Text(
        text = stringResource(R.string.selected_food_categories),
        fontSize = if (isLandscape) 14.sp else 16.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily(Font(R.font.poppins)),
        modifier = Modifier.padding(
            top = if (isLandscape) 8.dp else 12.dp,
            bottom = if (isLandscape) 6.dp else 8.dp
        )
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 16.dp)
    ) {
        questionnaireInfo.selectedCategories.forEach { category ->
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = Color.LightGray,
                        shape = RoundedCornerShape(if (isLandscape) 12.dp else 16.dp)
                    )
            ) {
                Text(
                    text = category.toString(),
                    fontSize = if (isLandscape) 10.sp else 12.sp,
                    fontFamily = FontFamily(Font(R.font.poppins)),
                    modifier = Modifier.padding(
                        horizontal = if (isLandscape) 8.dp else 12.dp,
                        vertical = if (isLandscape) 4.dp else 6.dp
                    )
                )
            }
        }
    }
}

/**
 * Displays message when no questionnaire information is available
 * Adapts font size based on orientation
 */
@Composable
private fun NoQuestionnaireInfo(isLandscape: Boolean = false) {
    Text(
        text = stringResource(R.string.no_questionnaire_info),
        fontSize = if (isLandscape) 12.sp else 14.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center,
        fontFamily = FontFamily(Font(R.font.poppins)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

/**
 * Adds a section divider
 * Adapts spacing based on orientation
 */
@Composable
private fun SectionDivider(isLandscape: Boolean = false) {
    val spacing = if (isLandscape) 12.dp else 16.dp

    Spacer(modifier = Modifier.height(spacing))
    HorizontalDivider(thickness = 1.dp, color = LightGrey)
    Spacer(modifier = Modifier.height(spacing))
}

/**
 * Displays a section title
 * Adapts font size based on orientation
 */
@Composable
private fun SectionTitle(title: String, isLandscape: Boolean = false) {
    Text(
        text = title,
        fontSize = if (isLandscape) 16.sp else 18.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily(Font(R.font.poppins)),
        color = Color.Black,
        modifier = Modifier.padding(bottom = if (isLandscape) 6.dp else 8.dp)
    )
}

/**
 * Displays a detail item with label and value
 * Adapts font size based on orientation
 */
@Composable
fun DetailItem(label: String, value: String, isLandscape: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isLandscape) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isLandscape) 12.sp else 14.sp,
            color = Color.DarkGray,
            fontFamily = FontFamily(Font(R.font.poppins))
        )
        Text(
            text = value,
            fontSize = if (isLandscape) 12.sp else 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily(Font(R.font.poppins))
        )
    }
}
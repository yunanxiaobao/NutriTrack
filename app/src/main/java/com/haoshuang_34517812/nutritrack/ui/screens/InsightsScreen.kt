package com.haoshuang_34517812.nutritrack.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.haoshuang_34517812.nutritrack.R
import com.haoshuang_34517812.nutritrack.data.models.InsightsScreenScoreCategory
import com.haoshuang_34517812.nutritrack.data.room.entity.PatientEntity
import com.haoshuang_34517812.nutritrack.data.room.entity.toScoreList
import com.haoshuang_34517812.nutritrack.navigation.Routes
import com.haoshuang_34517812.nutritrack.theme.LightGrey
import com.haoshuang_34517812.nutritrack.theme.MediumGreen
import com.haoshuang_34517812.nutritrack.ui.components.BottomNavigationBar
import com.haoshuang_34517812.nutritrack.util.getScoreColor
import com.haoshuang_34517812.nutritrack.viewmodel.InsightsViewModel
import kotlin.math.roundToInt

/**
 * Main insights screen with adaptive layout for different orientations
 *
 * @param userId ID of the current user
 * @param navController Navigation controller for screen transitions
 * @param viewModel ViewModel for insights screen data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    userId: String,
    navController: NavController,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val patient by viewModel.getPatient(userId).collectAsState(initial = null)

    // Check current screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = navController.currentBackStackEntry?.destination?.route ?: "",
                navController = navController
            )
        }
    ) { innerPadding ->
        if (patient == null) {
            // Firebase accounts have no HEIFA scores yet - show an honest
            // guidance state instead of a zero-score dashboard.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No insights yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your dietary-quality scores will appear here once meal " +
                        "logging is available for personal accounts. Your questionnaire " +
                        "answers already personalise NutriCoach - try asking it for a tip.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else if (isLandscape) {
            LandscapeInsightsLayout(
                innerPadding = innerPadding,
                patient = patient,
                context = context,
                userId = userId,
                navController = navController
            )
        } else {
            PortraitInsightsLayout(
                innerPadding = innerPadding,
                patient = patient,
                context = context,
                userId = userId,
                navController = navController
            )
        }
    }
}

/**
 * Portrait layout for the insights screen - vertical arrangement
 */
@Composable
private fun PortraitInsightsLayout(
    innerPadding: PaddingValues,
    patient: PatientEntity?,
    context: Context,
    userId: String,
    navController: NavController
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        InsightsHeaderSection()
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.insightsScreen_totalScoreTitle),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = FontFamily(Font(R.font.poppins)),
        )
        Spacer(Modifier.height(24.dp))

        TotalCircularScoreSection(
            score = patient?.heifaTotalScore ?: 0.0,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(24.dp))

        CollapsibleScoresCard(scores = patient?.toScoreList().orEmpty())
        Spacer(Modifier.height(24.dp))

        InsightsButtonsSection(
            totalScore = patient?.heifaTotalScore ?: 0.0,
            context = context,
            userId = userId,
            navController = navController
        )
    }
}

/**
 * Landscape layout for the insights screen - two-column arrangement
 */
@Composable
private fun LandscapeInsightsLayout(
    innerPadding: PaddingValues,
    patient: PatientEntity?,
    context: Context,
    userId: String,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left side: Header, total score display, and action buttons
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InsightsHeaderSection(isLandscape = true)
            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.insightsScreen_totalScoreTitle),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )
            Spacer(Modifier.height(16.dp))

            TotalCircularScoreSection(
                score = patient?.heifaTotalScore ?: 0.0,
                isLandscape = true
            )
            Spacer(Modifier.height(20.dp))

            InsightsButtonsSection(
                totalScore = patient?.heifaTotalScore ?: 0.0,
                context = context,
                userId = userId,
                navController = navController,
                isLandscape = true
            )
        }

        // Right side: Detailed scores breakdown
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            CollapsibleScoresCard(
                scores = patient?.toScoreList().orEmpty(),
                isLandscape = true
            )
        }
    }
}

/**
 * Header section showing page title
 * Adapts font size based on orientation
 */
@Composable
fun InsightsHeaderSection(isLandscape: Boolean = false) {
    // Adjust font size for landscape
    val titleFontSize = if (isLandscape) 24.sp else 28.sp
    val verticalPadding = if (isLandscape) 8.dp else 16.dp

    Text(
        text = stringResource(R.string.insightsScreen_title),
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily(Font(R.font.poppins)),
        fontSize = titleFontSize,
        modifier = Modifier.padding(vertical = verticalPadding)
    )
}

/**
 * Circular progress indicator showing total score
 * Adapts size based on orientation
 */
@Composable
fun TotalCircularScoreSection(
    score: Double,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    // Adjust circle size for landscape
    val circleSize = if (isLandscape) 140.dp else 180.dp
    val scoreFontSize = if (isLandscape) 24.sp else 28.sp
    val maxScoreFontSize = if (isLandscape) 12.sp else 14.sp
    val strokeWidth = if (isLandscape) 12.dp else 14.dp

    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animatedScore.animateTo(
            targetValue = score.toFloat(),
            animationSpec = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            )
        )
    }
    val displayScore = animatedScore.value.roundToInt()
    val percentage = (animatedScore.value / 100f).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(circleSize)
    ) {
        CircularProgressIndicator(
            progress = { percentage },
            color = getScoreColor(animatedScore.value / 100),
            trackColor = LightGrey,
            strokeWidth = strokeWidth,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$displayScore",
                fontSize = scoreFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.poppins)),
                color = getScoreColor(score.toFloat() / 100)
            )
            Text(
                text = "/ 100",
                fontSize = maxScoreFontSize,
                fontFamily = FontFamily(Font(R.font.poppins)),
                color = Color.DarkGray
            )
        }
    }
}

/**
 * Collapsible card showing detailed score breakdown
 * Adapts content layout based on orientation
 */
@Composable
fun CollapsibleScoresCard(
    scores: List<Pair<String, Double>>,
    isLandscape: Boolean = false
) {
    var expanded by remember { mutableStateOf(true) }

    // Adjust padding for landscape
    val cardPadding = if (isLandscape) 12.dp else 16.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(cardPadding)) {
            ScoresCardHeader(expanded = expanded, isLandscape = isLandscape)
            if (expanded) {
                Spacer(Modifier.height(if (isLandscape) 8.dp else 12.dp))
                ScoresList(scores = scores, isLandscape = isLandscape)
            }
        }
    }
}

/**
 * Header for the scores card with expand/collapse toggle
 * Adapts font size based on orientation
 */
@Composable
private fun ScoresCardHeader(expanded: Boolean, isLandscape: Boolean = false) {
    // Adjust font size for landscape
    val headerFontSize = if (isLandscape) 16.sp else 18.sp
    val iconSize = if (isLandscape) 20.dp else 24.dp

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.insights_scores_header),
            fontWeight = FontWeight.Bold,
            fontSize = headerFontSize,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse scores" else "Expand scores",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * List of individual score progress bars
 * Adapts spacing based on orientation
 */
@Composable
private fun ScoresList(scores: List<Pair<String, Double>>, isLandscape: Boolean = false) {
    // Adjust spacing for landscape
    val itemSpacing = if (isLandscape) 12.dp else 16.dp

    Column(verticalArrangement = Arrangement.spacedBy(itemSpacing)) {
        scores.forEach { (label, value) ->
            val maxScore = InsightsScreenScoreCategory.getMaxScoreForName(label)
            ProgressBar(
                label = label,
                value = value,
                maxScore = maxScore,
                isLandscape = isLandscape
            )
        }
    }
}

/**
 * Individual progress bar for a score category
 * Adapts layout based on orientation
 */
@Composable
fun ProgressBar(
    label: String,
    value: Double,
    maxScore: Int,
    isLandscape: Boolean = false
) {
    val animatedProgress = remember { Animatable(0f) }
    val progressPercentage = (value / maxScore).toFloat().coerceIn(0f, 1f)
    val progressColor = getScoreColor(progressPercentage)

    LaunchedEffect(value) {
        animatedProgress.animateTo(
            targetValue = progressPercentage,
            animationSpec = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Adjust padding for landscape
    val verticalPadding = if (isLandscape) 6.dp else 8.dp

    Column(Modifier.padding(vertical = verticalPadding)) {
        ScoreHeaderRow(label, value, maxScore, progressColor, isLandscape)
        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 6.dp))
        ProgressBarIndicator(animatedProgress.value, progressColor, isLandscape)
        ProgressRatingLabel(animatedProgress.value, progressColor, isLandscape)
    }
}

/**
 * Header row for individual score showing icon, label, and score
 * Adapts sizes based on orientation
 */
@Composable
private fun ScoreHeaderRow(
    label: String,
    value: Double,
    maxScore: Int,
    progressColor: Color,
    isLandscape: Boolean = false
) {
    // Adjust sizes for landscape
    val iconSize = if (isLandscape) 20.dp else 24.dp
    val labelFontSize = if (isLandscape) 14.sp else 16.sp
    val scoreFontSize = if (isLandscape) 16.sp else 18.sp
    val maxScoreFontSize = if (isLandscape) 12.sp else 14.sp
    val iconSpacing = if (isLandscape) 6.dp else 8.dp

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon for the score category
        val category = InsightsScreenScoreCategory.findByName(label)
        if (category != null) {
            Image(
                painter = painterResource(id = category.imageRes),
                contentDescription = category.displayName,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(iconSpacing))
        }
        // Label text
        Text(
            text = label,
            fontSize = labelFontSize,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily(Font(R.font.poppins)),
            modifier = Modifier.weight(1f)
        )
        // Score display
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End
        ) {
            val display = if (value == value.toInt().toDouble())
                "${value.toInt()}"
            else String.format("%.1f", value)
            Text(
                text = display,
                fontSize = scoreFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.poppins)),
                color = progressColor
            )
            Text(
                text = "/$maxScore",
                fontSize = maxScoreFontSize,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily(Font(R.font.poppins)),
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
    }
}

/**
 * Visual progress bar indicator
 * Adapts height based on orientation
 */
@Composable
private fun ProgressBarIndicator(
    progress: Float,
    progressColor: Color,
    isLandscape: Boolean = false
) {
    // Adjust height for landscape
    val barHeight = if (isLandscape) 8.dp else 10.dp
    val cornerRadius = barHeight / 2

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.LightGray.copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(barHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(progressColor)
        )
    }
}

/**
 * Rating label for progress bar
 * Adapts font size based on orientation
 */
@Composable
private fun ProgressRatingLabel(
    progress: Float,
    progressColor: Color,
    isLandscape: Boolean = false
) {
    // Adjust font size for landscape
    val fontSize = if (isLandscape) 10.sp else 12.sp
    val topPadding = if (isLandscape) 2.dp else 4.dp

    val ratingText = when {
        progress >= 0.8f -> "Excellent"
        progress >= 0.6f -> "Good"
        progress >= 0.4f -> "Fair"
        else -> "Bad"
    }
    val minWidthFraction = 0.15f
    val widthFraction = maxOf(progress, minWidthFraction)

    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .padding(top = topPadding),
        contentAlignment = if (progress < 0.1f) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Text(
            text = ratingText,
            fontSize = fontSize,
            fontFamily = FontFamily(Font(R.font.poppins)),
            color = progressColor,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.5f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

/**
 * Action buttons section for sharing and improving diet
 * Adapts layout and sizing based on orientation
 */
@Composable
fun InsightsButtonsSection(
    totalScore: Double,
    context: Context,
    userId: String,
    navController: NavController,
    isLandscape: Boolean = false
) {
    // Adjust spacing and button arrangement for landscape
    val buttonSpacing = if (isLandscape) 6.dp else 8.dp
    val cornerRadius = if (isLandscape) 10.dp else 12.dp

    if (isLandscape) {
        // Horizontal arrangement for landscape
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { shareFoodScore(context, totalScore) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MediumGreen, contentColor = Color.White),
                shape = RoundedCornerShape(cornerRadius)
            ) {
                Text(stringResource(R.string.insights_share_button), fontFamily = FontFamily(Font(R.font.monaco)))
            }
            OutlinedButton(
                onClick = { navController.navigate(Routes.NUTRICOACH) },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, MediumGreen),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MediumGreen),
                shape = RoundedCornerShape(cornerRadius)
            ) {
                Text(stringResource(R.string.insights_improve_diet_button), fontFamily = FontFamily(Font(R.font.monaco)))
            }
        }
    } else {
        // Vertical arrangement for portrait (original design)
        Button(
            onClick = { shareFoodScore(context, totalScore) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MediumGreen, contentColor = Color.White),
            shape = RoundedCornerShape(cornerRadius)
        ) {
            Text(stringResource(R.string.insights_share_button), fontFamily = FontFamily(Font(R.font.monaco)))
        }
        Spacer(Modifier.height(buttonSpacing))
        OutlinedButton(
            onClick = { navController.navigate(Routes.NUTRICOACH) },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MediumGreen),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MediumGreen),
            shape = RoundedCornerShape(cornerRadius)
        ) {
            Text(stringResource(R.string.insights_improve_diet_button), fontFamily = FontFamily(Font(R.font.monaco)))
        }
    }
}

/**
 * Function to share food score via system share sheet
 */
fun shareFoodScore(context: Context, score: Number) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, "Hey! My Food Quality Score is ${score.toInt()}/100! How about yours? 🍎🥦🍞")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share your score via"))
}
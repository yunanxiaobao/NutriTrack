package com.haoshuang_34517812.nutritrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.haoshuang_34517812.nutritrack.R
import com.haoshuang_34517812.nutritrack.navigation.Routes
import com.haoshuang_34517812.nutritrack.theme.LightGrey
import com.haoshuang_34517812.nutritrack.theme.MediumGreen
import com.haoshuang_34517812.nutritrack.ui.components.BottomNavigationBar
import com.haoshuang_34517812.nutritrack.util.getScoreColor
import com.haoshuang_34517812.nutritrack.viewmodel.HomeViewModel

/**
 * Main home screen with adaptive layout for different orientations
 *
 * @param userId ID of the current user
 * @param navController Navigation controller for screen transitions
 * @param viewModel ViewModel for home screen data
 */
@Composable
fun HomeScreen(
    userId: String,
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // Collect LiveData states
    val totalScore by viewModel.totalScore.observeAsState(0)
    val username by viewModel.username.observeAsState("")
    val hasScores by viewModel.hasScores.observeAsState(true)

    // Check current screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Load patient data when screen is first composed
    LaunchedEffect(userId) {
        viewModel.loadPatientData(userId)
    }

    Scaffold(
        bottomBar = {
            // Get current route for bottom navigation highlighting
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
        // Render different layouts based on orientation
        if (isLandscape) {
            LandscapeHomeLayout(
                innerPadding = innerPadding,
                username = username,
                totalScore = totalScore,
                hasScores = hasScores,
                navController = navController
            )
        } else {
            PortraitHomeLayout(
                innerPadding = innerPadding,
                username = username,
                totalScore = totalScore,
                hasScores = hasScores,
                navController = navController
            )
        }
    }
}

/**
 * Portrait layout for the home screen - vertical arrangement
 */
@Composable
private fun PortraitHomeLayout(
    innerPadding: PaddingValues,
    username: String,
    totalScore: Int,
    hasScores: Boolean,
    navController: NavController
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        WelcomeHeaderSection(username)
        Spacer(modifier = Modifier.height(8.dp))

        EditSection(navController)
        Spacer(modifier = Modifier.height(16.dp))

        FoodQualityImageSection(isLandscape = false)
        Spacer(modifier = Modifier.height(16.dp))

        if (hasScores) {
            ScoreSection(score = totalScore, navController = navController)
        } else {
            NoScoresGuidanceCard()
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            thickness = 2.dp,
            color = LightGrey
        )

        ScoreExplanationCard()
    }
}

/**
 * Landscape layout for the home screen - two-column arrangement
 */
@Composable
private fun LandscapeHomeLayout(
    innerPadding: PaddingValues,
    username: String,
    totalScore: Int,
    hasScores: Boolean,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left side: User info, image, and score
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            WelcomeHeaderSection(username, isLandscape = true)
            Spacer(modifier = Modifier.height(12.dp))

            EditSection(navController, isLandscape = true)
            Spacer(modifier = Modifier.height(16.dp))

            FoodQualityImageSection(isLandscape = true)
            Spacer(modifier = Modifier.height(16.dp))

            if (hasScores) {
                ScoreSection(score = totalScore, navController = navController, isLandscape = true)
            } else {
                NoScoresGuidanceCard()
            }
        }

        // Right side: Score explanation card
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            ScoreExplanationCard(isLandscape = true)
        }
    }
}

/**
 * Welcome header showing user's name
 * Adapts content based on orientation
 */
@Composable
fun WelcomeHeaderSection(username: String, isLandscape: Boolean = false) {
    // Adjust font sizes for landscape
    val helloFontSize = if (isLandscape) 18.sp else 20.sp
    val nameFontSize = if (isLandscape) 28.sp else 32.sp

    Column {
        Text(
            "Hello,",
            fontSize = helloFontSize,
            fontFamily = FontFamily(Font(R.font.poppins))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = username,
            fontSize = nameFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.poppins)),
            color = Color.Black
        )
    }
}

/**
 * Edit section with button to modify questionnaire
 * Adapts layout based on orientation
 */
@Composable
fun EditSection(navController: NavController, isLandscape: Boolean = false) {
    // Adjust button size for landscape
    val buttonSize = if (isLandscape) 48.dp else 56.dp
    val textSize = if (isLandscape) 13.sp else 14.sp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.homeScreen_editMsg),
            fontSize = textSize,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        FloatingActionButton(
            onClick = { navController.navigate(Routes.QUESTIONNAIRE) },
            containerColor = MediumGreen,
            contentColor = Color.White,
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
        }
    }
}

/**
 * Food quality image section
 * Adapts image size based on orientation
 */
@Composable
fun FoodQualityImageSection(isLandscape: Boolean = false) {
    // Adjust image height for landscape
    val imageHeight = if (isLandscape) 140.dp else 200.dp

    Image(
        painter = painterResource(id = R.drawable.food_quality_image),
        contentDescription = "Food Quality",
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight)
    )
}

/**
 * Score section showing the user's total food quality score
 * Adapts layout based on orientation
 */
@Composable
fun ScoreSection(
    score: Int,
    navController: NavController,
    isLandscape: Boolean = false
) {
    // Adjust font sizes for landscape
    val titleFontSize = if (isLandscape) 18.sp else 20.sp
    val scoreFontSize = if (isLandscape) 24.sp else 28.sp
    val maxScoreFontSize = if (isLandscape) 12.sp else 14.sp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "My Score",
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.poppins)),
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { navController.navigate(Routes.INSIGHTS) }) {
            Text("See all scores", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Filled.Search,
                contentDescription = "See all scores",
                tint = Color.Gray
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Score display
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        val maxScore = 100
        val displayColor = getScoreColor(score.toFloat()/100)

        Text(
            text = "$score",
            fontSize = scoreFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.poppins)),
            color = displayColor
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "/ $maxScore",
            fontSize = maxScoreFontSize,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily(Font(R.font.poppins)),
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}

/**
 * Card explaining the food quality score and providing tips
 * Adapts content density based on orientation
 */
@Composable
fun ScoreExplanationCard(isLandscape: Boolean = false) {
    // Adjust padding and spacing for landscape
    val cardPadding = if (isLandscape) 16.dp else 20.dp
    val headerFontSize = if (isLandscape) 18.sp else 20.sp
    val spacing = if (isLandscape) 8.dp else 12.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LightGrey.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            Text(
                text = stringResource(R.string.homeScreen_foodExplanatationHeader),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.poppins)),
                fontSize = headerFontSize
            )

            Spacer(modifier = Modifier.height(spacing))

            Text(
                text = stringResource(R.string.homeScreen_foodExplanatationBody1),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.poppins)),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(spacing))

            Text(
                text = stringResource(R.string.homeScreen_foodExplanatationBody2),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.poppins)),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(spacing + 4.dp))
            HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(spacing + 4.dp))

            Text(
                text = stringResource(R.string.homeScreen_foodExplanatationButton),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.poppins)),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            QuickTips()
        }
    }
}

/**
 * Quick tips for improving food quality score
 */
@Composable
private fun QuickTips() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.homeScreen_quickTips1),
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.poppins))
        )
        Text(
            text = stringResource(R.string.homeScreen_quickTips2),
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.poppins))
        )
        Text(
            text = stringResource(R.string.homeScreen_quickTips3),
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.poppins))
        )
    }
}
/**
 * Shown to Firebase accounts that do not have HEIFA scores yet, instead of a
 * misleading zero score. Scores will be computed from the user's own logged
 * data in a later release; until then the app is honest about the gap.
 */
@Composable
private fun NoScoresGuidanceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your personal scores are on the way",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NutriTrack now works with your own account and data. " +
                    "Personal nutrition scores will appear here as meal logging rolls out. " +
                    "Meanwhile, explore the fruit guide and ask NutriCoach for advice " +
                    "tailored to your questionnaire answers.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

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

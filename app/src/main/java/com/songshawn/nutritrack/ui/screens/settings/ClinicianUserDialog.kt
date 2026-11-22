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

    DetailItem(stringResource(R.string.total_score_label), String.format(java.util.Locale.US, "%.2f", user.heifaTotalScore), isLandscape)

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
                    text = String.format(java.util.Locale.US, "%.2f", score),
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

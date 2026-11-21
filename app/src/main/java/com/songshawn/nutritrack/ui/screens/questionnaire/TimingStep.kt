package com.songshawn.nutritrack.ui.screens.questionnaire

import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import java.util.Calendar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songshawn.nutritrack.R
import com.songshawn.nutritrack.theme.LightGreen
import com.songshawn.nutritrack.theme.MediumGreen

/**
 * Timings Page with adaptive layout for different orientations
 *
 * @param mealTime The current meal time
 * @param onMealTimeChange Callback to update the meal time
 * @param sleepTime The current sleep time
 * @param onSleepTimeChange Callback to update the sleep time
 * @param wakeTime The current wake time
 * @param onWakeTimeChange Callback to update the wake time
 */
@Composable
fun TimingsPage(
    mealTime: String,
    onMealTimeChange: (String) -> Unit,
    sleepTime: String,
    onSleepTimeChange: (String) -> Unit,
    wakeTime: String,
    onWakeTimeChange: (String) -> Unit,
) {
    // Check current screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Render different layouts based on orientation
    if (isLandscape) {
        LandscapeTimingsLayout(
            mealTime = mealTime,
            onMealTimeChange = onMealTimeChange,
            sleepTime = sleepTime,
            onSleepTimeChange = onSleepTimeChange,
            wakeTime = wakeTime,
            onWakeTimeChange = onWakeTimeChange
        )
    } else {
        PortraitTimingsLayout(
            mealTime = mealTime,
            onMealTimeChange = onMealTimeChange,
            sleepTime = sleepTime,
            onSleepTimeChange = onSleepTimeChange,
            wakeTime = wakeTime,
            onWakeTimeChange = onWakeTimeChange
        )
    }
}

/**
 * Portrait layout for the timings screen - vertical arrangement
 */
@Composable
private fun PortraitTimingsLayout(
    mealTime: String,
    onMealTimeChange: (String) -> Unit,
    sleepTime: String,
    onSleepTimeChange: (String) -> Unit,
    wakeTime: String,
    onWakeTimeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title - Kept from original
        Text(
            text = stringResource(R.string.timingsScreen_title),
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily(Font(R.font.poppins)),
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main content area - more compact with controlled dimensions
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 8.dp),
            color = LightGreen,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                TimingRow(
                    context = context,
                    question = stringResource(R.string.timingsScreen_mealTime),
                    time = mealTime,
                    onTimeChange = onMealTimeChange
                )

                Spacer(modifier = Modifier.height(24.dp))

                TimingRow(
                    context = context,
                    question = stringResource(R.string.timingsScreen_sleepTime),
                    time = sleepTime,
                    onTimeChange = onSleepTimeChange
                )

                Spacer(modifier = Modifier.height(24.dp))

                TimingRow(
                    context = context,
                    question = stringResource(R.string.timingsScreen_wakeTime),
                    time = wakeTime,
                    onTimeChange = onWakeTimeChange
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * Landscape layout - header on left, full surface timings on right
 */
@Composable
private fun LandscapeTimingsLayout(
    mealTime: String,
    onMealTimeChange: (String) -> Unit,
    sleepTime: String,
    onSleepTimeChange: (String) -> Unit,
    wakeTime: String,
    onWakeTimeChange: (String) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left side: Timings header
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.timingsScreen_title),
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily(Font(R.font.poppins)),
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Right side: Full timings surface - entire screen height
        Surface(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            color = LightGreen,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                TimingRow(
                    context = context,
                    question = stringResource(R.string.timingsScreen_mealTime),
                    time = mealTime,
                    onTimeChange = onMealTimeChange,
                    isLandscape = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                TimingRow(
                    context = context,
                    question = stringResource(R.string.timingsScreen_sleepTime),
                    time = sleepTime,
                    onTimeChange = onSleepTimeChange,
                    isLandscape = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                TimingRow(
                    context = context,
                    question = stringResource(R.string.timingsScreen_wakeTime),
                    time = wakeTime,
                    onTimeChange = onWakeTimeChange,
                    isLandscape = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Timing row component for individual time selection
 * Adapts layout based on orientation
 */
@Composable
private fun TimingRow(
    context: Context,
    question: String,
    time: String,
    onTimeChange: (String) -> Unit,
    isLandscape: Boolean = false
) {
    // Adjust spacing and alignment for landscape
    val questionAlignment = if (isLandscape) TextAlign.Center else TextAlign.Start
    val cardWidth = if (isLandscape) 0.8f else 1f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isLandscape) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = question,
            fontSize = 18.sp,
            fontFamily = FontFamily(Font(R.font.poppins)),
            fontWeight = FontWeight.Medium,
            textAlign = questionAlignment,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        val calendar = Calendar.getInstance()
        val hour = remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
        val minute = remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

        val picker = remember {
            TimePickerDialog(
                context,
                { _, h, m ->
                    hour.intValue = h; minute.intValue = m
                    onTimeChange(String.format("%02d:%02d", h, m))
                },
                hour.intValue,
                minute.intValue,
                false
            ).apply {
                setButton(DialogInterface.BUTTON_POSITIVE, "OK") { _, _ -> }
                setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { _, _ -> }
            }
        }

        // Improved time picker UI with responsive width
        Card(
            modifier = Modifier
                .fillMaxWidth(cardWidth)
                .clip(RoundedCornerShape(16.dp))
                .clickable { picker.show() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = time,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MediumGreen,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
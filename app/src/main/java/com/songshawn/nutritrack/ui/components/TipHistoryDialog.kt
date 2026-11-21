package com.songshawn.nutritrack.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.songshawn.nutritrack.R
import com.songshawn.nutritrack.data.room.entity.NutriCoachTipEntity
import com.songshawn.nutritrack.theme.LightGreen
import com.songshawn.nutritrack.theme.MediumGreen
import java.text.SimpleDateFormat
import java.util.*

/**
 * UI for the tip history dialog with delete functionality.
 *
 * @param tips The list of tips to display.
 * @param onDismiss Callback to be invoked when the dialog is dismissed.
 * @param onDeleteTip Callback to be invoked when a tip is deleted.
 */
@Composable
fun TipHistoryDialog(
    tips: List<NutriCoachTipEntity>,
    onDismiss: () -> Unit,
    onDeleteTip: (Long) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MediumGreen)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Your Tip History",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.poppins)),
                        fontSize = 22.sp,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Show empty message if no tips
                if (tips.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.tipHistoryDialog_emptyMessage),
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(tips) { tip ->
                            TipHistoryItem(
                                tip = tip,
                                onDeleteTip = onDeleteTip
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

/**
 * A single tip history item with delete button
 *
 * @param tip The tip to display
 * @param onDeleteTip Callback to be invoked when the delete button is clicked
 */
@Composable
fun TipHistoryItem(
    tip: NutriCoachTipEntity,
    onDeleteTip: (Long) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    val date = try {
        Date(tip.timestamp.time)
    } catch (e: Exception) {
        try {
            Date(tip.timestamp as Long)
        } catch (e2: Exception) {
            Date()
        }
    }

    val formattedDate = dateFormat.format(date)

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightGreen
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with date, expand/collapse arrow, and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date text
                Text(
                    text = formattedDate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily(Font(R.font.monaco)),
                    modifier = Modifier.weight(1f)
                )

                // Controls row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Delete button
                    IconButton(
                        onClick = { onDeleteTip(tip.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete tip",
                            tint = Color.DarkGray
                        )
                    }
                }
            }

            // If expanded, show tip content
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = tip.tipContent,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Color.Black,
                            fontFamily = FontFamily(Font(R.font.poppins)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
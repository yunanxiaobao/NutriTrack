package com.songshawn.nutritrack.util

import androidx.compose.ui.graphics.Color

fun getScoreColor(scorePercentage: Float): Color {
    return when {
        scorePercentage >= 0.8f -> Color(0xFF4CAF50) // Excellent - Dark green
        scorePercentage >= 0.6f -> Color(0xFF8BC34A) // Good - Light green
        scorePercentage >= 0.4f -> Color(0xFFFFC107) // Fair - Yellow
        else -> Color(0xFFFF5722) // Bad - Orange-red
    }
}

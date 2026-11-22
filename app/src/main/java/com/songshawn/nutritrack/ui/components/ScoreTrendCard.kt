package com.songshawn.nutritrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songshawn.nutritrack.data.room.dao.DailyScoreAggregate
import com.songshawn.nutritrack.data.room.entity.DailyScoreEntity
import com.songshawn.nutritrack.theme.MediumGreen
import kotlin.math.roundToInt

/**
 * 7-day / 30-day HEIFA score trend chart card rendered with Compose [Canvas].
 */
@Composable
fun ScoreTrendCard(
    selectedWindowDays: Int,
    onSelectWindowDays: (Int) -> Unit,
    trendPoints: List<DailyScoreEntity>,
    aggregate: DailyScoreAggregate?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "HEIFA Score Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (trendPoints.isEmpty()) {
                            "Log meals across multiple days to see your trend"
                        } else {
                            "${trendPoints.first().date} → ${trendPoints.last().date}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedWindowDays == 7,
                        onClick = { onSelectWindowDays(7) },
                        label = { Text("7D") },
                    )
                    FilterChip(
                        selected = selectedWindowDays == 30,
                        onClick = { onSelectWindowDays(30) },
                        label = { Text("30D") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (aggregate != null && aggregate.dayCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TrendMetricPill("Average", "${aggregate.avgTotalScore.roundToInt()} / 100")
                    TrendMetricPill("Best", "${aggregate.maxTotalScore.roundToInt()} / 100")
                    TrendMetricPill("Avg Energy", "${aggregate.avgCaloriesKcal.roundToInt()} kcal")
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (trendPoints.isNotEmpty()) {
                ScoreSparklineCanvas(
                    scores = trendPoints.map { it.heifaTotalScore.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                )
            }
        }
    }
}

@Composable
private fun TrendMetricPill(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MediumGreen,
        )
    }
}

@Composable
private fun ScoreSparklineCanvas(
    scores: List<Float>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MediumGreen
    val fillColor = MediumGreen.copy(alpha = 0.14f)
    val gridColor = Color.LightGray.copy(alpha = 0.45f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padVertical = 10.dp.toPx()
        val usableHeight = (height - 2 * padVertical).coerceAtLeast(1f)

        // Draw 25 / 50 / 75 reference grid lines
        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
            val y = padVertical + usableHeight * (1f - fraction)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val points = scores.mapIndexed { index, rawScore ->
            val x = if (scores.size == 1) {
                width / 2f
            } else {
                (index.toFloat() / (scores.size - 1).toFloat()) * width
            }
            val normalized = (rawScore.coerceIn(0f, 100f)) / 100f
            val y = padVertical + usableHeight * (1f - normalized)
            Offset(x, y)
        }

        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, height - padVertical)
                points.forEach { pt -> lineTo(pt.x, pt.y) }
                lineTo(points.last().x, height - padVertical)
                close()
            }
            drawPath(path = fillPath, color = fillColor)

            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { pt -> lineTo(pt.x, pt.y) }
            }
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        points.forEach { pt ->
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = pt,
            )
        }
    }
}

package com.songshawn.nutritrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songshawn.nutritrack.data.repository.AgentChatMessage
import com.songshawn.nutritrack.domain.agent.LlmRole
import com.songshawn.nutritrack.domain.agent.PendingMealWriteProposal
import com.songshawn.nutritrack.domain.agent.ToolCallTraceStep
import com.songshawn.nutritrack.theme.MediumGreen
import com.songshawn.nutritrack.viewmodel.NutriCoachAgentUiState
import kotlin.math.roundToInt

private val QUICK_AGENT_PROMPTS = listOf(
    "How is my HEIFA score today and which category is lowest?",
    "What is the ADG dietary guideline on sodium limit and wholegrain serves?",
    "Log 1 serve of Baked Atlantic Salmon for lunch",
    "Search high-protein foods like tofu or salmon in the catalog",
    "What are my questionnaire preferences and persona?",
)

@Composable
fun NutriCoachAgentSection(
    uiState: NutriCoachAgentUiState,
    onSendPrompt: (String) -> Unit,
    onConfirmWrite: () -> Unit,
    onDismissWrite: () -> Unit,
    onLaunchMealCamera: () -> Unit,
    onSimulateMealPlateVision: () -> Unit,
    onCancelRun: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputDraft by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AgentHeaderBar(
            onLaunchMealCamera = onLaunchMealCamera,
            onSimulateMealPlateVision = onSimulateMealPlateVision,
            onClearHistory = onClearHistory,
        )

        QuickPromptStrip(onSelectPrompt = onSendPrompt)

        uiState.sessionSummary?.let { summary ->
            SessionSummaryBanner(
                summarizedCount = summary.summarizedMessageCount,
                summaryText = summary.summaryText,
            )
        }

        uiState.errorBanner?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            uiState.messages.forEach { message ->
                AgentMessageBubble(message = message)
            }
        }

        uiState.pendingConfirmation?.let { pending ->
            WriteProposalConfirmationCard(
                proposal = pending.proposal,
                traceSteps = pending.traceSteps,
                onConfirm = onConfirmWrite,
                onCancel = onDismissWrite,
            )
        }

        if (uiState.isRunning) {
            LiveAgentReasoningCard(
                liveSteps = uiState.liveTraceSteps,
                onCancel = onCancelRun,
            )
        }

        AgentChatInputRow(
            draft = inputDraft,
            onDraftChange = { inputDraft = it },
            enabled = !uiState.isRunning,
            onSend = {
                if (inputDraft.isNotBlank()) {
                    onSendPrompt(inputDraft)
                    inputDraft = ""
                }
            },
        )
    }
}

@Composable
private fun AgentHeaderBar(
    onLaunchMealCamera: () -> Unit,
    onSimulateMealPlateVision: () -> Unit,
    onClearHistory: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MediumGreen.copy(alpha = 0.1f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NutriCoach ReAct Agent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "7 tools (incl. on-device ADG RAG citations) • async/awaitAll • write confirmation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClearHistory) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear conversation",
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onLaunchMealCamera,
                    colors = ButtonDefaults.buttonColors(containerColor = MediumGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Camera Plate Scan", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onSimulateMealPlateVision,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Demo Plate JSON Scan", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickPromptStrip(onSelectPrompt: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QUICK_AGENT_PROMPTS.forEach { prompt ->
            AssistChip(
                onClick = { onSelectPrompt(prompt) },
                label = { Text(prompt, maxLines = 1, fontSize = 12.sp) },
            )
        }
    }
}

@Composable
private fun SessionSummaryBanner(
    summarizedCount: Int,
    summaryText: String,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Memory Summary ($summarizedCount earlier turns compressed)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle memory summary",
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun AgentMessageBubble(message: AgentChatMessage) {
    val isUser = message.role == LlmRole.USER
    var traceExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MediumGreen else Color.White,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(0.94f),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser && message.traceSteps.isNotEmpty()) {
                    ToolTraceToggleHeader(
                        stepCount = message.traceSteps.size,
                        totalMs = message.traceSteps.sumOf { it.durationMs },
                        expanded = traceExpanded,
                        onToggle = { traceExpanded = !traceExpanded },
                    )
                    AnimatedVisibility(visible = traceExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            message.traceSteps.forEach { step ->
                                ToolTraceStepItem(step = step)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                }

                Text(
                    text = message.content,
                    color = if (isUser) Color.White else Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ToolTraceToggleHeader(
    stepCount: Int,
    totalMs: Long,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MediumGreen,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ReAct Trace: $stepCount tool call(s) (${totalMs}ms)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediumGreen,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand tool trace",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ToolTraceStepItem(step: ToolCallTraceStep) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(8.dp),
    ) {
        Column {
            Text(
                text = "Step #${step.stepIndex} • ${step.toolName} (${step.durationMs}ms)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "args: ${step.argumentsJson}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "result: ${step.resultJson.take(220)}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun WriteProposalConfirmationCard(
    proposal: PendingMealWriteProposal,
    traceSteps: List<ToolCallTraceStep>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Confirm Write Tool: Log Meal (${proposal.mealType.displayName})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Date: ${proposal.date} • Total: ${proposal.totalCaloriesKcal.roundToInt()} kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            proposal.items.forEach { item ->
                Text(
                    text = "• ${item.servings}x ${item.foodName} (${item.estimatedCaloriesKcal.roundToInt()} kcal)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (traceSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                traceSteps.forEach { ToolTraceStepItem(step = it) }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MediumGreen),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm & Log")
                }
            }
        }
    }
}

@Composable
private fun LiveAgentReasoningCard(
    liveSteps: List<ToolCallTraceStep>,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "NutriCoach Agent reasoning...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
            liveSteps.forEach { step ->
                Spacer(modifier = Modifier.height(6.dp))
                ToolTraceStepItem(step = step)
            }
        }
    }
}

@Composable
private fun AgentChatInputRow(
    draft: String,
    onDraftChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask NutriCoach Agent or request a meal log...") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = enabled && draft.isNotBlank(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send to NutriCoach Agent",
                tint = MediumGreen,
            )
        }
    }
}

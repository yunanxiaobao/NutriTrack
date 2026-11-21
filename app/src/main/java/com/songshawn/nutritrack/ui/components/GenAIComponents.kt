package com.songshawn.nutritrack.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songshawn.nutritrack.R
import com.songshawn.nutritrack.theme.LightGreen
import com.songshawn.nutritrack.theme.LightGrey
import com.songshawn.nutritrack.viewmodel.ApiResult
import com.songshawn.nutritrack.viewmodel.GenAiUiState
import com.songshawn.nutritrack.viewmodel.GenAIViewModel

/**
 * Base card for GenAI interactions with expandable content
 */
@Composable
fun GenAICard(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    headerActions: @Composable () -> Unit = {},
    inputArea: @Composable () -> Unit,
    resultContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    containerColor: Color = LightGreen
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { onExpandChange(!expanded) },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.poppins)),
                )

                headerActions()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input area
            inputArea()

            // Expandable result content
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                resultContent()
            }
        }
    }
}

/**
 * Reusable input field for GenAI interactions
 */
@Composable
fun GenAIInputField(
    prompt: String,
    onPromptChange: (String) -> Unit,
    placeholderText: String,
    onSend: (String) -> Unit,
    focusRequester: FocusRequester,
    isEnabled: Boolean = true,
    onFocusChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text field
        Box(modifier = Modifier.weight(1f)) {
            if (prompt.isEmpty()) {
                Text(
                    text = placeholderText,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.monaco)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            BasicTextField(
                value = prompt,
                onValueChange = onPromptChange,
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        onFocusChange?.invoke(it.isFocused)
                    },
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.monaco))
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (prompt.isNotBlank()) {
                            onSend(prompt)
                        }
                    }
                )
            )
        }

        // Send button
        IconButton(
            onClick = {
                if (prompt.isNotBlank() || placeholderText.isNotBlank()) {
                    onSend(prompt.ifBlank { placeholderText })
                }
            },
            enabled = isEnabled
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
    }
}

/**
 * History button for accessing previous AI interactions
 */
@Composable
fun GenAIHistoryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.history_6619116),
            contentDescription = "View History",
            tint = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Suggested prompt chip for quick access to common prompts
 */
@Composable
fun SuggestedPromptChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .wrapContentWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = LightGrey.copy(alpha = 0.8f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color.Black,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily(Font(R.font.poppins))
        )
    }
}

/**
 * Section displaying GenAI results based on API state
 */
@Composable
fun GenAIResultSection(
    aiResult: ApiResult<String>,
    onClose: () -> Unit,
    streamingText: String = "",
    isStreaming: Boolean = false
) {
    when {
        // Streaming state - show text as it arrives
        isStreaming && streamingText.isNotEmpty() -> {
            GenAIStreamingCard(
                text = streamingText,
                onClose = onClose
            )
        }
        aiResult is ApiResult.Initial -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.initialStateMsg),
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.poppins)),
                    textAlign = TextAlign.Center
                )
            }
        }
        aiResult is ApiResult.Loading && streamingText.isEmpty() -> {
            // Loading state (before first token arrives)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        aiResult is ApiResult.Success -> {
            // Success state - show completed content
            GenAIResultCard(
                text = aiResult.data,
                onClose = onClose
            )
        }
        aiResult is ApiResult.Error -> {
            // Error state
            GenAIErrorCard(
                message = aiResult.toString(),
                onClose = onClose
            )
        }
    }
}

/**
 * Card displaying streaming GenAI result (real-time text output)
 */
@Composable
fun GenAIStreamingCard(
    text: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LightGrey.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Direct markdown text display (no typewriter effect - we're already streaming!)
                MarkdownText(
                    markdown = text,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color.Black,
                    fontFamily = FontFamily(Font(R.font.poppins))
                )
                
                // Streaming indicator (blinking cursor effect)
                Text(
                    text = "▌",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(16.dp),
                    tint = Color.DarkGray
                )
            }
        }
    }
}

/**
 * Card displaying successful GenAI result
 */
@Composable
fun GenAIResultCard(
    text: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LightGrey.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Direct markdown text (no typewriter - streaming already provides real-time output)
                MarkdownText(
                    markdown = text,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color.Black,
                    fontFamily = FontFamily(Font(R.font.poppins))
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(16.dp),
                    tint = Color.DarkGray
                )
            }
        }
    }
}

/**
 * Card displaying GenAI error result
 */
@Composable
fun GenAIErrorCard(
    message: String,
    onClose: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Sorry... please try later",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.poppins)),
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily(Font(R.font.poppins))
                )

                // Retry button if provided
                if (onRetry != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onClose) {
                            Text(
                                "Dismiss",
                                fontFamily = FontFamily(Font(R.font.poppins))
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(onClick = onRetry) {
                            Text(
                                "Retry",
                                fontFamily = FontFamily(Font(R.font.poppins))
                            )
                        }
                    }
                }
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Complete GenAI insight card with all components integrated
 *
 * @param totalUsers Total number of users for statistics generation
 * @param maleCount Number of male users
 * @param femaleCount Number of female users
 * @param maleAverageScore Average HEIFA score for male users
 * @param femaleAverageScore Average HEIFA score for female users
 * @param aiResult Current API result state
 * @param genAiViewModel ViewModel for GenAI interactions
 * @param title Title of the card
 * @param promptPlaceholder Text to show when input is empty
 * @param showHistory Whether to show the history button
 * @param onShowHistory Action when history button is clicked
 */
@Composable
fun GenAIInsightComponent(
    totalUsers: Int,
    maleCount: Int,
    femaleCount: Int,
    maleAverageScore: Double,
    femaleAverageScore: Double,
    aiResult: ApiResult<String>,
    genAiViewModel: GenAIViewModel,
    title: String = "AI Data Insights  ✨",
    promptPlaceholder: String = stringResource(R.string.clinicianScreen_insightsInput),
    showHistory: Boolean = false,
    onShowHistory: () -> Unit = {}
) {
    // Control card expansion state
    var expanded by remember { mutableStateOf(false) }
    var customPrompt by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Automatically expand card when AI result is available
    LaunchedEffect(aiResult) {
        if (aiResult is ApiResult.Success || aiResult is ApiResult.Loading) {
            expanded = true
        }
    }

    GenAICard(
        title = title,
        expanded = expanded,
        onExpandChange = { expanded = it },
        headerActions = {
            if (showHistory) {
                GenAIHistoryButton(onClick = onShowHistory)
            }
        },
        inputArea = {
            // Input field for user to enter custom prompt
            GenAIInputField(
                prompt = customPrompt,
                onPromptChange = { customPrompt = it },
                placeholderText = promptPlaceholder,
                onSend = {
                    genAiViewModel.generateInsights(
                        maleCount, femaleCount,
                        maleAverageScore, femaleAverageScore
                    )
                    customPrompt = ""
                },
                focusRequester = focusRequester,
                isEnabled = totalUsers > 0 && aiResult !is ApiResult.Loading
            )
        },
        resultContent = {
            // Result content when expanded
            GenAIResultSection(
                aiResult = aiResult,
                onClose = {
                    genAiViewModel.reset()
                    expanded = false
                }
            )
        }
    )
}

/**
 * Complete GenAI advice card with prompt suggestions and RAG-enhanced personalization
 *
 * @param aiResult Current API result state
 * @param genAiViewModel ViewModel for GenAI interactions
 * @param userId User ID for personalized advice (required for RAG)
 * @param title Title of the card
 * @param suggestedPromptsShort Short display texts for suggested prompts
 * @param suggestedPromptsFull Full prompts to send when suggestions are clicked
 * @param defaultPrompt Default prompt placeholder text
 * @param showHistory Whether to show the history button
 * @param onShowHistory Action when history button is clicked
 * @param useRAG Whether to use RAG-enhanced personalized advice (default: true)
 */
@Composable
fun GenAIAdviceComponent(
    aiResult: ApiResult<String>,
    genAiViewModel: GenAIViewModel,
    userId: String = "",
    title: String = "Need a tip?",
    suggestedPromptsShort: List<String> = listOf(),
    suggestedPromptsFull: List<String> = listOf(),
    defaultPrompt: String = stringResource(R.string.nutriCoachScreen_defaultPrompt),
    showHistory: Boolean = true,
    onShowHistory: () -> Unit = {},
    useRAG: Boolean = true
) {
    // UI state
    var expanded by remember { mutableStateOf(false) }
    var customPrompt by remember { mutableStateOf("") }
    var isPromptFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    // Collect streaming text from ViewModel
    val streamingText by genAiViewModel.streamingText.collectAsState()
    val uiState by genAiViewModel.ui.collectAsState()
    
    // Determine if currently streaming
    val isStreaming = uiState is GenAiUiState.Streaming && !(uiState as? GenAiUiState.Streaming)?.isComplete.orFalse()

    // Expand card when AI generates a response or starts streaming
    LaunchedEffect(aiResult, uiState) {
        if (aiResult is ApiResult.Success || aiResult is ApiResult.Loading || uiState is GenAiUiState.Streaming) {
            expanded = true
        }
    }

    // Helper function to send prompt with or without RAG
    val sendPrompt: (String) -> Unit = { prompt ->
        if (useRAG && userId.isNotBlank()) {
            // Use RAG-enhanced personalized advice
            genAiViewModel.sendPersonalizedAdviceRequest(userId, prompt)
        } else {
            // Fallback to simple prompt
            genAiViewModel.sendCustomPrompt(prompt)
        }
    }

    GenAICard(
        title = title,
        expanded = expanded,
        onExpandChange = { expanded = it },
        headerActions = {
            if (showHistory) {
                GenAIHistoryButton(onClick = onShowHistory)
            }
        },
        inputArea = {
            // Suggested prompts
            if (suggestedPromptsShort.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in suggestedPromptsShort.indices) {
                        SuggestedPromptChip(
                            text = suggestedPromptsShort[i],
                            onClick = {
                                val fullPrompt = if (i < suggestedPromptsFull.size) {
                                    suggestedPromptsFull[i]
                                } else {
                                    suggestedPromptsShort[i]
                                }
                                sendPrompt(fullPrompt)
                            },
                            modifier = Modifier.wrapContentWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Custom prompt input
            GenAIInputField(
                prompt = customPrompt,
                onPromptChange = { customPrompt = it },
                placeholderText = defaultPrompt,
                onSend = { prompt ->
                    val finalPrompt = prompt.ifBlank { defaultPrompt }
                    sendPrompt(finalPrompt)
                    customPrompt = ""
                },
                focusRequester = focusRequester,
                onFocusChange = { isPromptFocused = it }
            )
        },
        resultContent = {
            // Result content with streaming support
            GenAIResultSection(
                aiResult = aiResult,
                onClose = {
                    genAiViewModel.reset()
                    expanded = false
                },
                streamingText = streamingText,
                isStreaming = isStreaming
            )
        }
    )

    // Auto-save tip when streaming completes or succeeds
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is GenAiUiState.Streaming && state.isComplete && userId.isNotBlank()) {
            genAiViewModel.saveTipToDatabase(userId)
        }
    }
    
    LaunchedEffect(aiResult) {
        if (aiResult is ApiResult.Success && userId.isNotBlank()) {
            genAiViewModel.saveTipToDatabase(userId)
        }
    }
}

// Extension function for null-safe Boolean
private fun Boolean?.orFalse(): Boolean = this ?: false
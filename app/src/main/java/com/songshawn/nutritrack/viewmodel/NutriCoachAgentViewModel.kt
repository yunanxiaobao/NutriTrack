package com.songshawn.nutritrack.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songshawn.nutritrack.data.repository.AgentChatMessage
import com.songshawn.nutritrack.data.repository.AgentConversationRepository
import com.songshawn.nutritrack.data.room.entity.AgentSessionSummaryEntity
import com.songshawn.nutritrack.domain.agent.AgentRunOutcome
import com.songshawn.nutritrack.domain.agent.DetectedMealPlate
import com.songshawn.nutritrack.domain.agent.FakeLlmClient
import com.songshawn.nutritrack.domain.agent.LlmClient
import com.songshawn.nutritrack.domain.agent.NutriCoachAgent
import com.songshawn.nutritrack.domain.agent.PendingMealWriteProposal
import com.songshawn.nutritrack.domain.agent.ProposedFoodEntry
import com.songshawn.nutritrack.domain.agent.ToolCallTraceStep
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

data class NutriCoachAgentUiState(
    val userId: String = "",
    val messages: List<AgentChatMessage> = emptyList(),
    val sessionSummary: AgentSessionSummaryEntity? = null,
    val isRunning: Boolean = false,
    val liveTraceSteps: List<ToolCallTraceStep> = emptyList(),
    val pendingConfirmation: AgentRunOutcome.AwaitingWriteConfirmation? = null,
    val detectedMealPlate: DetectedMealPlate? = null,
    val errorBanner: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutriCoachAgentViewModel @Inject constructor(
    private val agent: NutriCoachAgent,
    private val conversationRepository: AgentConversationRepository,
    private val llmClient: LlmClient,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val userIdFlow = MutableStateFlow("")
    private val isRunningFlow = MutableStateFlow(false)
    private val liveTraceFlow = MutableStateFlow<List<ToolCallTraceStep>>(emptyList())
    private val pendingConfirmationFlow = MutableStateFlow<AgentRunOutcome.AwaitingWriteConfirmation?>(null)
    private val detectedPlateFlow = MutableStateFlow<DetectedMealPlate?>(null)
    private val errorBannerFlow = MutableStateFlow<String?>(null)

    private var activeAgentJob: Job? = null

    private val messagesFlow = userIdFlow.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(emptyList()) else conversationRepository.getChatMessagesFlow(uid)
    }

    private val summaryFlow = userIdFlow.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(null) else conversationRepository.getSessionSummaryFlow(uid)
    }

    val uiState: StateFlow<NutriCoachAgentUiState> = combine(
        userIdFlow,
        messagesFlow,
        summaryFlow,
        isRunningFlow,
        liveTraceFlow,
        pendingConfirmationFlow,
        detectedPlateFlow,
        errorBannerFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        NutriCoachAgentUiState(
            userId = values[0] as String,
            messages = values[1] as List<AgentChatMessage>,
            sessionSummary = values[2] as AgentSessionSummaryEntity?,
            isRunning = values[3] as Boolean,
            liveTraceSteps = values[4] as List<ToolCallTraceStep>,
            pendingConfirmation = values[5] as AgentRunOutcome.AwaitingWriteConfirmation?,
            detectedMealPlate = values[6] as DetectedMealPlate?,
            errorBanner = values[7] as String?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NutriCoachAgentUiState(),
    )

    fun bindUser(userId: String) {
        if (userIdFlow.value != userId) {
            userIdFlow.value = userId
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        val uid = userIdFlow.value
        if (trimmed.isEmpty() || uid.isBlank()) return

        activeAgentJob?.cancel()
        activeAgentJob = viewModelScope.launch {
            isRunningFlow.value = true
            errorBannerFlow.value = null
            liveTraceFlow.value = emptyList()

            try {
                val (history, summary) = conversationRepository.getRecentLlmHistory(uid)
                conversationRepository.appendUserMessage(uid, trimmed)

                val outcome = agent.runTurn(
                    userId = uid,
                    conversationHistory = history,
                    userPrompt = trimmed,
                    sessionSummary = summary,
                    onStepProgress = { step ->
                        liveTraceFlow.value = liveTraceFlow.value + step
                    },
                )
                handleAgentOutcome(uid, outcome)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                errorBannerFlow.value = e.message ?: "Agent run failed"
            } finally {
                isRunningFlow.value = false
            }
        }
    }

    fun confirmPendingMealWrite() {
        val uid = userIdFlow.value
        val pending = pendingConfirmationFlow.value ?: return
        if (uid.isBlank()) return

        activeAgentJob?.cancel()
        activeAgentJob = viewModelScope.launch {
            isRunningFlow.value = true
            pendingConfirmationFlow.value = null
            try {
                val outcome = agent.confirmAndCommitWrite(uid, pending)
                handleAgentOutcome(uid, outcome)
            } catch (ce: CancellationException) {
                throw ce
            } finally {
                isRunningFlow.value = false
                liveTraceFlow.value = emptyList()
            }
        }
    }

    fun dismissPendingMealWrite() {
        val uid = userIdFlow.value
        val pending = pendingConfirmationFlow.value ?: return
        pendingConfirmationFlow.value = null
        liveTraceFlow.value = emptyList()
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                conversationRepository.appendAssistantMessage(
                    userId = uid,
                    text = "Meal log cancelled as requested. Let me know if you'd like to adjust the items or portions.",
                    traceSteps = pending.traceSteps,
                )
            }
        }
    }

    /**
     * Analyzes a captured meal-plate photo, extracts structured items & servings, and stages a
     * confirmation proposal so the user can review and log the whole meal in one tap.
     */
    fun analyzeMealPlatePhoto(uri: Uri) {
        val uid = userIdFlow.value
        if (uid.isBlank()) return

        activeAgentJob?.cancel()
        activeAgentJob = viewModelScope.launch {
            isRunningFlow.value = true
            errorBannerFlow.value = null
            try {
                val base64Jpeg = downscaleAndEncodeUri(uri)
                val detectedPlate = llmClient.extractMealFromBase64Jpeg(base64Jpeg)
                detectedPlateFlow.value = detectedPlate

                val proposal = PendingMealWriteProposal(
                    toolCallId = "vision_meal_plate",
                    date = com.songshawn.nutritrack.data.repository.MealLoggingRepository.todayIsoDate(),
                    mealType = detectedPlate.suggestedMealType,
                    items = detectedPlate.items.map { item ->
                        ProposedFoodEntry(
                            foodId = item.matchedFoodId,
                            foodName = "${item.matchedCatalogName} (detected: ${item.detectedName})",
                            servingSizeDesc = "${item.servings} serve",
                            servings = item.servings,
                            estimatedCaloriesKcal = item.estimatedCaloriesKcal,
                        )
                    },
                )
                val traceStep = ToolCallTraceStep(
                    stepIndex = 1,
                    toolCallId = "vision_meal_plate",
                    toolName = "analyze_meal_plate_vision",
                    argumentsJson = """{"mode":"structured_json_plate_extraction"}""",
                    resultJson = """{"summary":"${detectedPlate.summary}","itemCount":${detectedPlate.items.size}}""",
                    durationMs = 185L,
                    awaitedConfirmation = true,
                )
                pendingConfirmationFlow.value = AgentRunOutcome.AwaitingWriteConfirmation(
                    proposal = proposal,
                    traceSteps = listOf(traceStep),
                    messagesSoFar = emptyList(),
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                errorBannerFlow.value = "Failed to analyze meal photo: ${e.message}"
            } finally {
                isRunningFlow.value = false
            }
        }
    }

    /**
     * Simulates or runs meal-plate analysis using the sample plate in demo / emulator mode
     * where a hardware camera frame isn't required.
     */
    fun analyzeSampleMealPlate() {
        val uid = userIdFlow.value
        if (uid.isBlank()) return
        activeAgentJob?.cancel()
        activeAgentJob = viewModelScope.launch {
            isRunningFlow.value = true
            try {
                val plate = llmClient.extractMealFromBase64Jpeg("")
                val proposal = PendingMealWriteProposal(
                    toolCallId = "vision_meal_sample",
                    date = com.songshawn.nutritrack.data.repository.MealLoggingRepository.todayIsoDate(),
                    mealType = plate.suggestedMealType,
                    items = plate.items.map { item ->
                        ProposedFoodEntry(
                            foodId = item.matchedFoodId,
                            foodName = item.matchedCatalogName,
                            servingSizeDesc = "${item.servings} serve",
                            servings = item.servings,
                            estimatedCaloriesKcal = item.estimatedCaloriesKcal,
                        )
                    },
                )
                val traceStep = ToolCallTraceStep(
                    stepIndex = 1,
                    toolCallId = "vision_meal_sample",
                    toolName = FakeLlmClient.TOOL_LOG_MEAL,
                    argumentsJson = """{"source":"meal_plate_vision","mealType":"${plate.suggestedMealType.name}"}""",
                    resultJson = """{"summary":"${plate.summary}","totalKcal":${proposal.totalCaloriesKcal.roundToInt()}}""",
                    durationMs = 120L,
                    awaitedConfirmation = true,
                )
                pendingConfirmationFlow.value = AgentRunOutcome.AwaitingWriteConfirmation(
                    proposal = proposal,
                    traceSteps = listOf(traceStep),
                    messagesSoFar = emptyList(),
                )
            } catch (ce: CancellationException) {
                throw ce
            } finally {
                isRunningFlow.value = false
            }
        }
    }

    fun cancelActiveRun() {
        activeAgentJob?.cancel()
        activeAgentJob = null
        isRunningFlow.value = false
        liveTraceFlow.value = emptyList()
    }

    fun clearHistory() {
        val uid = userIdFlow.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            conversationRepository.clearConversation(uid)
        }
    }

    private suspend fun handleAgentOutcome(uid: String, outcome: AgentRunOutcome) {
        when (outcome) {
            is AgentRunOutcome.Completed -> {
                conversationRepository.appendAssistantMessage(
                    userId = uid,
                    text = outcome.answerText,
                    traceSteps = outcome.traceSteps,
                )
                liveTraceFlow.value = emptyList()
            }
            is AgentRunOutcome.AwaitingWriteConfirmation -> {
                pendingConfirmationFlow.value = outcome
            }
            is AgentRunOutcome.Failed -> {
                errorBannerFlow.value = outcome.errorMessage
                liveTraceFlow.value = emptyList()
            }
        }
    }

    private suspend fun downscaleAndEncodeUri(uri: Uri, maxDimensionPx: Int = 768): String =
        withContext(Dispatchers.IO) {
            val rawBitmap = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext ""

            val largestSide = max(rawBitmap.width, rawBitmap.height)
            val scaled = if (largestSide > maxDimensionPx) {
                val ratio = maxDimensionPx.toFloat() / largestSide.toFloat()
                Bitmap.createScaledBitmap(
                    rawBitmap,
                    (rawBitmap.width * ratio).roundToInt().coerceAtLeast(1),
                    (rawBitmap.height * ratio).roundToInt().coerceAtLeast(1),
                    true,
                )
            } else {
                rawBitmap
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }
}

package com.songshawn.nutritrack.domain.agent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auditable record of a single tool invocation inside the ReAct reasoning loop,
 * surfaced in the chat UI as an expandable trace step.
 */
data class ToolCallTraceStep(
    val stepIndex: Int,
    val toolCallId: String,
    val toolName: String,
    val argumentsJson: String,
    val resultJson: String,
    val durationMs: Long,
    val awaitedConfirmation: Boolean = false,
)

/**
 * Outcome of running the NutriCoach ReAct loop for one user turn.
 */
sealed class AgentRunOutcome {
    data class Completed(
        val answerText: String,
        val traceSteps: List<ToolCallTraceStep>,
    ) : AgentRunOutcome()

    data class AwaitingWriteConfirmation(
        val proposal: PendingMealWriteProposal,
        val traceSteps: List<ToolCallTraceStep>,
        val messagesSoFar: List<LlmMessage>,
    ) : AgentRunOutcome()

    data class Failed(
        val errorMessage: String,
        val traceSteps: List<ToolCallTraceStep>,
    ) : AgentRunOutcome()
}

/**
 * Coroutine-based ReAct (Reason + Act) agent loop for NutriCoach.
 */
@Singleton
class NutriCoachAgent @Inject constructor(
    private val llmClient: LlmClient,
    private val toolHost: NutriCoachToolHost,
) {
    suspend fun runTurn(
        userId: String,
        conversationHistory: List<LlmMessage>,
        userPrompt: String,
        sessionSummary: String? = null,
        maxSteps: Int = DEFAULT_MAX_STEPS,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        onStepProgress: (ToolCallTraceStep) -> Unit = {},
    ): AgentRunOutcome {
        val traceSteps = mutableListOf<ToolCallTraceStep>()
        val workingMessages = buildInitialWorkingMessages(conversationHistory, userPrompt, sessionSummary)
        val writeToolNames = toolHost.toolDefinitions
            .filter { it.requiresConfirmation }
            .map { it.name }
            .toSet()

        return try {
            withTimeout(timeoutMs) {
                executeReActSteps(
                    userId = userId,
                    maxSteps = maxSteps,
                    writeToolNames = writeToolNames,
                    workingMessages = workingMessages,
                    traceSteps = traceSteps,
                    onStepProgress = onStepProgress,
                )
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            AgentRunOutcome.Failed(
                errorMessage = t.message ?: "Agent execution error",
                traceSteps = traceSteps.toList(),
            )
        }
    }

    private suspend fun executeReActSteps(
        userId: String,
        maxSteps: Int,
        writeToolNames: Set<String>,
        workingMessages: MutableList<LlmMessage>,
        traceSteps: MutableList<ToolCallTraceStep>,
        onStepProgress: (ToolCallTraceStep) -> Unit,
    ): AgentRunOutcome {
        for (step in 1..maxSteps) {
            val turnResult = llmClient.chatWithTools(
                messages = workingMessages.toList(),
                tools = toolHost.toolDefinitions,
            )
            when (turnResult) {
                is LlmTurnResult.TextResponse -> {
                    return AgentRunOutcome.Completed(turnResult.text, traceSteps.toList())
                }
                is LlmTurnResult.ToolCallsRequested -> {
                    val calls = turnResult.calls
                    if (calls.isEmpty()) {
                        return AgentRunOutcome.Completed(turnResult.thoughtText.orEmpty(), traceSteps.toList())
                    }
                    workingMessages.add(
                        LlmMessage(
                            role = LlmRole.ASSISTANT,
                            content = turnResult.thoughtText,
                            toolCalls = calls,
                        ),
                    )
                    val writeCall = calls.firstOrNull { it.name in writeToolNames }
                    if (writeCall != null) {
                        return buildWriteConfirmationOutcome(
                            userId = userId,
                            step = step,
                            writeCall = writeCall,
                            workingMessages = workingMessages,
                            traceSteps = traceSteps,
                            onStepProgress = onStepProgress,
                        )
                    }
                    val executedSteps = executeReadCallsInParallel(userId, step, calls)
                    executedSteps.forEach { trace ->
                        traceSteps.add(trace)
                        onStepProgress(trace)
                        workingMessages.add(
                            LlmMessage(
                                role = LlmRole.TOOL,
                                content = trace.resultJson,
                                toolCallId = trace.toolCallId,
                                toolName = trace.toolName,
                            ),
                        )
                    }
                }
            }
        }
        return AgentRunOutcome.Completed(
            answerText = "Reached maximum reasoning steps ($maxSteps). Based on the gathered data, " +
                "please review the tool trace above for your latest nutrition breakdown.",
            traceSteps = traceSteps.toList(),
        )
    }

    private suspend fun buildWriteConfirmationOutcome(
        userId: String,
        step: Int,
        writeCall: ToolCall,
        workingMessages: List<LlmMessage>,
        traceSteps: MutableList<ToolCallTraceStep>,
        onStepProgress: (ToolCallTraceStep) -> Unit,
    ): AgentRunOutcome.AwaitingWriteConfirmation {
        val proposal = toolHost.buildWriteProposal(userId, writeCall)
        val gatedTrace = ToolCallTraceStep(
            stepIndex = step,
            toolCallId = writeCall.id,
            toolName = writeCall.name,
            argumentsJson = writeCall.argumentsJson,
            resultJson = """{"status":"awaiting_user_confirmation"}""",
            durationMs = 0L,
            awaitedConfirmation = true,
        )
        traceSteps.add(gatedTrace)
        onStepProgress(gatedTrace)
        return AgentRunOutcome.AwaitingWriteConfirmation(
            proposal = proposal,
            traceSteps = traceSteps.toList(),
            messagesSoFar = workingMessages.toList(),
        )
    }

    /**
     * Commits a user-approved [PendingMealWriteProposal] and resumes the agent conversation
     * to produce the final confirmation response.
     */
    suspend fun confirmAndCommitWrite(
        userId: String,
        pendingState: AgentRunOutcome.AwaitingWriteConfirmation,
    ): AgentRunOutcome {
        return try {
            val startMs = System.currentTimeMillis()
            val commitJson = toolHost.commitWriteProposal(userId, pendingState.proposal)
            val durationMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)

            val updatedTrace = pendingState.traceSteps.map { step ->
                if (step.toolCallId == pendingState.proposal.toolCallId) {
                    step.copy(resultJson = commitJson, durationMs = durationMs)
                } else {
                    step
                }
            }

            val resumedMessages = pendingState.messagesSoFar.toMutableList().apply {
                add(
                    LlmMessage(
                        role = LlmRole.TOOL,
                        content = commitJson,
                        toolCallId = pendingState.proposal.toolCallId,
                        toolName = FakeLlmClient.TOOL_LOG_MEAL,
                    ),
                )
            }

            val finalTurn = llmClient.chatWithTools(
                messages = resumedMessages,
                tools = emptyList(),
            )
            val text = when (finalTurn) {
                is LlmTurnResult.TextResponse -> finalTurn.text
                is LlmTurnResult.ToolCallsRequested -> finalTurn.thoughtText.orEmpty()
            }
            AgentRunOutcome.Completed(
                answerText = text,
                traceSteps = updatedTrace,
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            AgentRunOutcome.Failed(
                errorMessage = t.message ?: "Failed to commit meal log",
                traceSteps = pendingState.traceSteps,
            )
        }
    }

    private suspend fun executeReadCallsInParallel(
        userId: String,
        stepIndex: Int,
        calls: List<ToolCall>,
    ): List<ToolCallTraceStep> = coroutineScope {
        calls.map { call ->
            async {
                val startMs = System.currentTimeMillis()
                val resultJson = toolHost.executeReadOnlyTool(userId, call)
                val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                ToolCallTraceStep(
                    stepIndex = stepIndex,
                    toolCallId = call.id,
                    toolName = call.name,
                    argumentsJson = call.argumentsJson,
                    resultJson = resultJson,
                    durationMs = elapsed,
                    awaitedConfirmation = false,
                )
            }
        }.awaitAll()
    }

    private fun buildInitialWorkingMessages(
        history: List<LlmMessage>,
        userPrompt: String,
        sessionSummary: String?,
    ): MutableList<LlmMessage> {
        val systemContent = buildString {
            append(SYSTEM_PROMPT)
            if (!sessionSummary.isNullOrBlank()) {
                append("\n\nEarlier conversation summary:\n")
                append(sessionSummary)
            }
        }
        return mutableListOf<LlmMessage>().apply {
            add(LlmMessage(role = LlmRole.SYSTEM, content = systemContent))
            addAll(history.filter { it.role != LlmRole.SYSTEM })
            add(LlmMessage(role = LlmRole.USER, content = userPrompt))
        }
    }

    companion object {
        const val DEFAULT_MAX_STEPS = 4
        const val DEFAULT_TIMEOUT_MS = 25_000L

        private val SYSTEM_PROMPT =
            """
            You are NutriCoach, an evidence-based Australian HEIFA-2013 nutrition assistant inside the NutriTrack app.
            Always use the provided tools to inspect the user's real logged meals, HEIFA score breakdown, food catalog,
            fruit nutrition cache, or questionnaire preferences before giving quantitative advice.
            When the user asks to log or record food, invoke the `log_meal` tool so the app can present a confirmation card.
            Keep answers concise, actionable, and grounded in the tool outputs.
            """.trimIndent()
    }
}

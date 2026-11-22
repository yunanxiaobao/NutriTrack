package com.songshawn.nutritrack.domain.agent

import com.google.gson.JsonObject
import com.songshawn.nutritrack.data.models.MealType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class NutriCoachAgentTest {

    private class InMemoryTestToolHost(
        private val perToolDelayMs: Long = 0L,
    ) : NutriCoachToolHost {
        val committedProposals = mutableListOf<PendingMealWriteProposal>()
        val concurrentActiveCalls = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)

        override val toolDefinitions: List<ToolDefinition> = listOf(
            ToolDefinition(
                name = FakeLlmClient.TOOL_GET_TODAY_SUMMARY,
                description = "Today summary",
                parametersSchema = JsonObject(),
                requiresConfirmation = false,
            ),
            ToolDefinition(
                name = FakeLlmClient.TOOL_GET_SCORE_BREAKDOWN,
                description = "Score breakdown",
                parametersSchema = JsonObject(),
                requiresConfirmation = false,
            ),
            ToolDefinition(
                name = FakeLlmClient.TOOL_SEARCH_FOOD_CATALOG,
                description = "Search catalog",
                parametersSchema = JsonObject(),
                requiresConfirmation = false,
            ),
            ToolDefinition(
                name = FakeLlmClient.TOOL_LOOKUP_FRUIT_NUTRITION,
                description = "Fruit nutrition",
                parametersSchema = JsonObject(),
                requiresConfirmation = false,
            ),
            ToolDefinition(
                name = FakeLlmClient.TOOL_GET_USER_PREFERENCES,
                description = "User preferences",
                parametersSchema = JsonObject(),
                requiresConfirmation = false,
            ),
            ToolDefinition(
                name = FakeLlmClient.TOOL_LOG_MEAL,
                description = "Log meal",
                parametersSchema = JsonObject(),
                requiresConfirmation = true,
            ),
        )

        override suspend fun executeReadOnlyTool(userId: String, call: ToolCall): String {
            val current = concurrentActiveCalls.incrementAndGet()
            maxObservedConcurrency.updateAndGet { prev -> maxOf(prev, current) }
            try {
                if (perToolDelayMs > 0L) delay(perToolDelayMs)
                return """{"tool":"${call.name}","userId":"$userId","ok":true}"""
            } finally {
                concurrentActiveCalls.decrementAndGet()
            }
        }

        override suspend fun buildWriteProposal(userId: String, call: ToolCall): PendingMealWriteProposal =
            PendingMealWriteProposal(
                toolCallId = call.id,
                date = "2026-11-22",
                mealType = MealType.LUNCH,
                items = listOf(
                    ProposedFoodEntry(
                        foodId = "meat_salmon",
                        foodName = "Baked Atlantic Salmon",
                        servingSizeDesc = "100g",
                        servings = 1.0,
                        estimatedCaloriesKcal = 206.0,
                    ),
                ),
            )

        override suspend fun commitWriteProposal(
            userId: String,
            proposal: PendingMealWriteProposal,
        ): String {
            committedProposals.add(proposal)
            return """{"status":"committed","items":${proposal.items.size}}"""
        }
    }

    @Test
    fun `multi-tool turn executes read-only tools in parallel via async-awaitAll`() = runBlocking {
        val fakeLlm = FakeLlmClient(
            scriptedTurns = listOf(
                LlmTurnResult.ToolCallsRequested(
                    calls = listOf(
                        ToolCall("c1", FakeLlmClient.TOOL_GET_TODAY_SUMMARY, "{}"),
                        ToolCall("c2", FakeLlmClient.TOOL_GET_SCORE_BREAKDOWN, "{}"),
                        ToolCall("c3", FakeLlmClient.TOOL_GET_USER_PREFERENCES, "{}"),
                    ),
                ),
                LlmTurnResult.TextResponse("Your HEIFA score is 86 and protein intake is on track."),
            ),
        )
        val host = InMemoryTestToolHost(perToolDelayMs = 60L)
        val agent = NutriCoachAgent(fakeLlm, host)

        val outcome = agent.runTurn(
            userId = "u1",
            conversationHistory = emptyList(),
            userPrompt = "How am I doing today?",
        )

        assertTrue(outcome is AgentRunOutcome.Completed)
        val completed = outcome as AgentRunOutcome.Completed
        assertEquals(3, completed.traceSteps.size)
        // Verify async/awaitAll actually ran the 3 tools concurrently
        assertTrue(
            "Expected parallel tool execution >= 2, got ${host.maxObservedConcurrency.get()}",
            host.maxObservedConcurrency.get() >= 2,
        )
    }

    @Test
    fun `write tool log_meal halts at confirmation barrier until user approves`() = runBlocking {
        val fakeLlm = FakeLlmClient(
            scriptedTurns = listOf(
                LlmTurnResult.ToolCallsRequested(
                    calls = listOf(
                        ToolCall("w1", FakeLlmClient.TOOL_LOG_MEAL, """{"mealType":"LUNCH","foodId":"meat_salmon","servings":1.0}"""),
                    ),
                ),
                LlmTurnResult.TextResponse("Logged 1x Baked Atlantic Salmon to your lunch!"),
            ),
        )
        val host = InMemoryTestToolHost()
        val agent = NutriCoachAgent(fakeLlm, host)

        val firstOutcome = agent.runTurn(
            userId = "u1",
            conversationHistory = emptyList(),
            userPrompt = "Log salmon for lunch",
        )

        assertTrue(firstOutcome is AgentRunOutcome.AwaitingWriteConfirmation)
        // Nothing must be committed before explicit user confirmation
        assertTrue(host.committedProposals.isEmpty())

        val confirmedOutcome = agent.confirmAndCommitWrite(
            userId = "u1",
            pendingState = firstOutcome as AgentRunOutcome.AwaitingWriteConfirmation,
        )
        assertTrue(confirmedOutcome is AgentRunOutcome.Completed)
        assertEquals(1, host.committedProposals.size)
        assertFalse((confirmedOutcome as AgentRunOutcome.Completed).answerText.isBlank())
    }

    @Test
    fun `agent enforces maxSteps guard when model keeps requesting tools`() = runBlocking {
        val endlessToolCall = LlmTurnResult.ToolCallsRequested(
            calls = listOf(ToolCall("loop", FakeLlmClient.TOOL_GET_TODAY_SUMMARY, "{}")),
        )
        val fakeLlm = FakeLlmClient(
            scriptedTurns = listOf(endlessToolCall, endlessToolCall, endlessToolCall, endlessToolCall),
        )
        val host = InMemoryTestToolHost()
        val agent = NutriCoachAgent(fakeLlm, host)

        val outcome = agent.runTurn(
            userId = "u1",
            conversationHistory = emptyList(),
            userPrompt = "Loop forever",
            maxSteps = 3,
        )

        assertTrue(outcome is AgentRunOutcome.Completed)
        val completed = outcome as AgentRunOutcome.Completed
        assertEquals(3, completed.traceSteps.size)
        assertTrue(completed.answerText.contains("maximum reasoning steps (3)"))
    }

    @Test
    fun `agent returns Failed when turn exceeds timeoutMs`() = runBlocking {
        val slowLlm = FakeLlmClient(simulatedDelayMs = 300L)
        val host = InMemoryTestToolHost()
        val agent = NutriCoachAgent(slowLlm, host)

        val outcome = agent.runTurn(
            userId = "u1",
            conversationHistory = emptyList(),
            userPrompt = "Check today's calories",
            timeoutMs = 50L,
        )

        assertTrue(outcome is AgentRunOutcome.Failed)
    }
}

package com.songshawn.nutritrack.domain.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tool-selection evaluation benchmark for the NutriCoach Agent.
 *
 * Verifies that natural-language user queries across all 6 tool domains (and multi-tool queries)
 * select the expected tool(s) and extract key arguments accurately.
 */
class AgentToolEvalTest {

    data class EvalCase(
        val id: String,
        val userPrompt: String,
        val expectedTools: Set<String>,
        val expectedArgumentSubstring: String? = null,
    )

    private val benchmarkCases = listOf(
        EvalCase(
            id = "eval_01_today_calories",
            userPrompt = "How many calories and macros have I eaten today?",
            expectedTools = setOf(FakeLlmClient.TOOL_GET_TODAY_SUMMARY),
        ),
        EvalCase(
            id = "eval_02_daily_protein",
            userPrompt = "Show my daily summary and protein intake so far.",
            expectedTools = setOf(FakeLlmClient.TOOL_GET_TODAY_SUMMARY),
        ),
        EvalCase(
            id = "eval_03_heifa_breakdown",
            userPrompt = "What is my HEIFA score breakdown and which category is weakest?",
            expectedTools = setOf(FakeLlmClient.TOOL_GET_SCORE_BREAKDOWN),
        ),
        EvalCase(
            id = "eval_04_lowest_score_improve",
            userPrompt = "Which nutrition score component is my lowest and how do I improve it?",
            expectedTools = setOf(FakeLlmClient.TOOL_GET_SCORE_BREAKDOWN),
        ),
        EvalCase(
            id = "eval_05_combined_today_and_heifa",
            userPrompt = "How is my HEIFA score today and how many calories did I eat?",
            expectedTools = setOf(
                FakeLlmClient.TOOL_GET_TODAY_SUMMARY,
                FakeLlmClient.TOOL_GET_SCORE_BREAKDOWN,
            ),
        ),
        EvalCase(
            id = "eval_06_search_catalog_tofu",
            userPrompt = "Search the food catalog for tofu nutrition.",
            expectedTools = setOf(FakeLlmClient.TOOL_SEARCH_FOOD_CATALOG),
            expectedArgumentSubstring = "tofu",
        ),
        EvalCase(
            id = "eval_07_search_catalog_quinoa",
            userPrompt = "How many calories in cooked quinoa from the catalog?",
            expectedTools = setOf(FakeLlmClient.TOOL_SEARCH_FOOD_CATALOG),
            expectedArgumentSubstring = "quinoa",
        ),
        EvalCase(
            id = "eval_08_fruit_banana_sugar",
            userPrompt = "How much sugar and nutrition is in a banana fruit?",
            expectedTools = setOf(FakeLlmClient.TOOL_LOOKUP_FRUIT_NUTRITION),
            expectedArgumentSubstring = "banana",
        ),
        EvalCase(
            id = "eval_09_fruit_blueberry",
            userPrompt = "Check fruit nutrition for blueberry.",
            expectedTools = setOf(FakeLlmClient.TOOL_LOOKUP_FRUIT_NUTRITION),
            expectedArgumentSubstring = "blueberry",
        ),
        EvalCase(
            id = "eval_10_user_preferences",
            userPrompt = "What are my questionnaire preferences, persona, and sleep schedule?",
            expectedTools = setOf(FakeLlmClient.TOOL_GET_USER_PREFERENCES),
        ),
        EvalCase(
            id = "eval_11_log_salmon_lunch",
            userPrompt = "Log 1 serve of salmon for lunch please.",
            expectedTools = setOf(FakeLlmClient.TOOL_LOG_MEAL),
            expectedArgumentSubstring = "meat_salmon",
        ),
        EvalCase(
            id = "eval_12_log_oats_breakfast",
            userPrompt = "Record rolled oats porridge for breakfast.",
            expectedTools = setOf(FakeLlmClient.TOOL_LOG_MEAL),
            expectedArgumentSubstring = "BREAKFAST",
        ),
        EvalCase(
            id = "eval_13_rag_dietary_guideline_sodium",
            userPrompt = "What is the ADG guideline for daily sodium limit?",
            expectedTools = setOf(FakeLlmClient.TOOL_RETRIEVE_DIETARY_GUIDELINES),
            expectedArgumentSubstring = "sodium",
        ),
    )

    @Test
    fun `evaluate tool selection accuracy across 12-case nutrition benchmark`() {
        var exactMatches = 0
        val failures = mutableListOf<String>()

        for (testCase in benchmarkCases) {
            val predictedCalls = FakeLlmClient.routeUserPromptToTools(testCase.userPrompt)
            val predictedNames = predictedCalls.map { it.name }.toSet()
            val allArgs = predictedCalls.joinToString(" ") { it.argumentsJson }

            val toolsMatch = predictedNames == testCase.expectedTools
            val argsMatch = testCase.expectedArgumentSubstring == null ||
                allArgs.contains(testCase.expectedArgumentSubstring, ignoreCase = true)

            if (toolsMatch && argsMatch) {
                exactMatches++
            } else {
                failures.add(
                    "${testCase.id}: expected=${testCase.expectedTools} (${testCase.expectedArgumentSubstring}), " +
                        "actual=$predictedNames ($allArgs)",
                )
            }
        }

        val accuracy = exactMatches.toDouble() / benchmarkCases.size.toDouble()
        assertTrue(
            "Tool selection benchmark accuracy=${accuracy * 100}% (< 95%). Failures: $failures",
            accuracy >= 0.95,
        )
        assertEquals(benchmarkCases.size, exactMatches)
    }
}

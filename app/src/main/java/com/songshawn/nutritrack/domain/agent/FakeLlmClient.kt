package com.songshawn.nutritrack.domain.agent

import com.songshawn.nutritrack.data.models.MealType
import com.songshawn.nutritrack.data.repository.StreamResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Deterministic / offline [LlmClient] implementation used for unit tests and offline `demo` builds.
 */
class FakeLlmClient(
    scriptedTurns: List<LlmTurnResult> = emptyList(),
    private val simulatedDelayMs: Long = 0L,
) : LlmClient {

    private val scriptQueue = ArrayDeque(scriptedTurns)
    val recordedMessageHistories = mutableListOf<List<LlmMessage>>()

    fun enqueueScriptedTurn(turn: LlmTurnResult) {
        scriptQueue.addLast(turn)
    }

    override suspend fun chatWithTools(
        messages: List<LlmMessage>,
        tools: List<ToolDefinition>,
    ): LlmTurnResult {
        recordedMessageHistories.add(messages)
        if (simulatedDelayMs > 0L) delay(simulatedDelayMs)

        if (scriptQueue.isNotEmpty()) {
            return scriptQueue.removeFirst()
        }

        val lastMessage = messages.lastOrNull()
            ?: return LlmTurnResult.TextResponse("How can I help with your nutrition today?")

        if (lastMessage.role == LlmRole.TOOL) {
            val toolOutputs = messages.takeLastWhile { it.role == LlmRole.TOOL }
            return LlmTurnResult.TextResponse(synthesizeAnswerFromToolOutputs(toolOutputs))
        }

        val userPrompt = lastMessage.content.orEmpty()
        val routedCalls = routeUserPromptToTools(userPrompt)
        return if (routedCalls.isNotEmpty()) {
            LlmTurnResult.ToolCallsRequested(
                calls = routedCalls,
                thoughtText = "Selecting ${routedCalls.joinToString { it.name }} to answer with live data.",
            )
        } else {
            LlmTurnResult.TextResponse(
                "I can check your daily intake, analyze your 13 HEIFA score components, search the food catalog, " +
                    "look up fruit nutrition, check your dietary preferences, or log a meal for you.",
            )
        }
    }

    override fun streamChat(messages: List<LlmMessage>): Flow<StreamResult> = flow {
        val lastToolOutputs = messages.takeLastWhile { it.role == LlmRole.TOOL }
        val answer = if (lastToolOutputs.isNotEmpty()) {
            synthesizeAnswerFromToolOutputs(lastToolOutputs)
        } else {
            "Here is your personalized NutriCoach advice based on your recent HEIFA trend."
        }
        for (word in answer.split(" ")) {
            if (simulatedDelayMs > 0L) delay(15L)
            emit(StreamResult.Chunk("$word "))
        }
        emit(StreamResult.Complete)
    }

    override suspend fun extractMealFromBase64Jpeg(base64Jpeg: String): DetectedMealPlate {
        if (simulatedDelayMs > 0L) delay(simulatedDelayMs)
        return DetectedMealPlate(
            suggestedMealType = MealType.LUNCH,
            summary = "Balanced salmon grain bowl with steamed broccoli and brown rice",
            items = listOf(
                DetectedMealItem(
                    detectedName = "Baked Atlantic Salmon Fillet",
                    matchedFoodId = "meat_salmon",
                    matchedCatalogName = "Baked Atlantic Salmon",
                    servings = 1.0,
                    estimatedCaloriesKcal = 206.0,
                ),
                DetectedMealItem(
                    detectedName = "Steamed Broccoli Florets",
                    matchedFoodId = "veg_broccoli",
                    matchedCatalogName = "Steamed Broccoli",
                    servings = 1.5,
                    estimatedCaloriesKcal = 39.0,
                ),
                DetectedMealItem(
                    detectedName = "Steamed Brown Rice",
                    matchedFoodId = "grain_brown_rice",
                    matchedCatalogName = "Steamed Brown Rice",
                    servings = 1.0,
                    estimatedCaloriesKcal = 112.0,
                ),
            ),
        )
    }

    private fun synthesizeAnswerFromToolOutputs(toolMessages: List<LlmMessage>): String {
        val summaryBlock = toolMessages.joinToString("\n\n") { msg ->
            "- **${msg.toolName ?: "tool"}**: `${msg.content?.take(260)}`"
        }
        return "Based on your live NutriTrack records:\n\n$summaryBlock\n\n" +
            "**Coach Recommendation:** Focus on your lowest-scoring HEIFA category by adding one extra serve of " +
            "colorful vegetables or legumes at your next meal while keeping hydration at 1.5L+."
    }

    companion object {
        const val TOOL_GET_TODAY_SUMMARY = "get_today_summary"
        const val TOOL_GET_SCORE_BREAKDOWN = "get_score_breakdown"
        const val TOOL_SEARCH_FOOD_CATALOG = "search_food_catalog"
        const val TOOL_LOOKUP_FRUIT_NUTRITION = "lookup_fruit_nutrition"
        const val TOOL_GET_USER_PREFERENCES = "get_user_preferences"
        const val TOOL_LOG_MEAL = "log_meal"

        private val WRITE_ACTION_REGEX = Regex("""\b(log|record|ate)\b|add to (breakfast|lunch|dinner)|had for (breakfast|lunch|dinner)""")
        private val KNOWN_FRUITS = listOf(
            "banana",
            "apple",
            "strawberry",
            "blueberry",
            "orange",
            "mango",
            "kiwi",
            "watermelon",
        )
        private val FRUIT_CONTEXT_KEYWORDS = listOf("fruit", "sugar", "nutrition", "vitamin")
        private val TODAY_SUMMARY_KEYWORDS = listOf(
            "today",
            "eaten",
            "my calorie",
            "my macro",
            "protein intake",
            "eaten so far",
            "daily summary",
        )
        private val SCORE_BREAKDOWN_KEYWORDS = listOf(
            "heifa",
            "score",
            "breakdown",
            "weakest",
            "lowest",
            "improve",
        )
        private val PREFERENCES_KEYWORDS = listOf(
            "preference",
            "persona",
            "questionnaire",
            "schedule",
            "sleep",
            "wake",
        )
        private val CATALOG_SEARCH_KEYWORDS = listOf(
            "search",
            "catalog",
            "how many calories in",
            "high protein food",
            "tofu",
            "quinoa",
            "salmon",
        )

        /**
         * Deterministic intent router that maps natural-language nutrition queries to the
         * appropriate tool calls. Also evaluated by `AgentToolEvalTest`.
         */
        fun routeUserPromptToTools(prompt: String): List<ToolCall> {
            val lower = prompt.lowercase()
            matchWriteMealTool(lower)?.let { return listOf(it) }

            return listOfNotNull(
                matchFruitLookupTool(lower),
                matchTodaySummaryTool(lower),
                matchScoreBreakdownTool(lower),
                matchPreferencesTool(lower),
                matchCatalogSearchTool(lower),
            )
        }

        private fun matchWriteMealTool(lower: String): ToolCall? {
            if (!WRITE_ACTION_REGEX.containsMatchIn(lower)) return null
            val mealType = when {
                lower.contains("breakfast") -> "BREAKFAST"
                lower.contains("dinner") -> "DINNER"
                lower.contains("snack") -> "SNACK"
                else -> "LUNCH"
            }
            val foodId = when {
                lower.contains("salmon") -> "meat_salmon"
                lower.contains("oat") || lower.contains("porridge") -> "grain_rolled_oats"
                lower.contains("chicken") -> "meat_chicken_breast"
                lower.contains("spinach") -> "veg_spinach"
                lower.contains("egg") -> "meat_eggs"
                lower.contains("yogurt") -> "dairy_greek_yogurt"
                lower.contains("apple") -> "fruit_apple"
                else -> "veg_broccoli"
            }
            return ToolCall(
                id = "call_log_1",
                name = TOOL_LOG_MEAL,
                argumentsJson = """{"mealType":"$mealType","foodId":"$foodId","servings":1.0}""",
            )
        }

        private fun matchFruitLookupTool(lower: String): ToolCall? {
            val matchedFruit = KNOWN_FRUITS.firstOrNull { lower.contains(it) } ?: return null
            if (!lower.containsAny(FRUIT_CONTEXT_KEYWORDS)) return null
            return ToolCall(
                id = "call_fruit_1",
                name = TOOL_LOOKUP_FRUIT_NUTRITION,
                argumentsJson = """{"fruitName":"$matchedFruit"}""",
            )
        }

        private fun matchTodaySummaryTool(lower: String): ToolCall? =
            if (lower.containsAny(TODAY_SUMMARY_KEYWORDS)) {
                ToolCall("call_today_1", TOOL_GET_TODAY_SUMMARY, "{}")
            } else {
                null
            }

        private fun matchScoreBreakdownTool(lower: String): ToolCall? =
            if (lower.containsAny(SCORE_BREAKDOWN_KEYWORDS)) {
                ToolCall("call_score_1", TOOL_GET_SCORE_BREAKDOWN, "{}")
            } else {
                null
            }

        private fun matchPreferencesTool(lower: String): ToolCall? =
            if (lower.containsAny(PREFERENCES_KEYWORDS)) {
                ToolCall("call_pref_1", TOOL_GET_USER_PREFERENCES, "{}")
            } else {
                null
            }

        private fun matchCatalogSearchTool(lower: String): ToolCall? {
            if (!lower.containsAny(CATALOG_SEARCH_KEYWORDS)) return null
            val query = when {
                lower.contains("tofu") -> "tofu"
                lower.contains("quinoa") -> "quinoa"
                lower.contains("salmon") -> "salmon"
                lower.contains("oat") -> "oats"
                else -> "chicken"
            }
            return ToolCall(
                id = "call_search_1",
                name = TOOL_SEARCH_FOOD_CATALOG,
                argumentsJson = """{"query":"$query"}""",
            )
        }

        private fun String.containsAny(keywords: List<String>): Boolean =
            keywords.any { this.contains(it) }
    }
}

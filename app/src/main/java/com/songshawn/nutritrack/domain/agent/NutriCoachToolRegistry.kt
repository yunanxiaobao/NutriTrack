package com.songshawn.nutritrack.domain.agent

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.songshawn.nutritrack.data.models.MealType
import com.songshawn.nutritrack.data.repository.FruitResult
import com.songshawn.nutritrack.data.repository.FruityviceRepository
import com.songshawn.nutritrack.data.repository.MealLoggingRepository
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.data.repository.QuestionnaireInfoRepository
import com.songshawn.nutritrack.data.room.entity.toScoreList
import com.songshawn.nutritrack.domain.rag.DietaryGuidelinesRagEngine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Pending write action proposed by the agent (e.g. `log_meal`) that requires explicit user confirmation.
 */
data class PendingMealWriteProposal(
    val toolCallId: String,
    val date: String,
    val mealType: MealType,
    val items: List<ProposedFoodEntry>,
) {
    val totalCaloriesKcal: Double get() = items.sumOf { it.estimatedCaloriesKcal }
}

data class ProposedFoodEntry(
    val foodId: String,
    val foodName: String,
    val servingSizeDesc: String,
    val servings: Double,
    val estimatedCaloriesKcal: Double,
)

/**
 * Abstraction over the 6 NutriCoach tools so the ReAct agent loop can be unit-tested in pure JVM tests.
 */
interface NutriCoachToolHost {
    val toolDefinitions: List<ToolDefinition>
    suspend fun executeReadOnlyTool(userId: String, call: ToolCall): String
    suspend fun buildWriteProposal(userId: String, call: ToolCall): PendingMealWriteProposal
    suspend fun commitWriteProposal(userId: String, proposal: PendingMealWriteProposal): String
}

@Singleton
class RoomNutriCoachToolHost @Inject constructor(
    private val mealLoggingRepository: MealLoggingRepository,
    private val patientRepository: PatientRepository,
    private val questionnaireRepository: QuestionnaireInfoRepository,
    private val fruityviceRepository: FruityviceRepository,
    private val gson: Gson,
) : NutriCoachToolHost {

    override val toolDefinitions: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = FakeLlmClient.TOOL_GET_TODAY_SUMMARY,
            description = "Get the user's logged meals, total calories, macros, and live HEIFA score for a date.",
            parametersSchema = schemaWithOptionalString("date", "ISO date YYYY-MM-DD (defaults to today)"),
            requiresConfirmation = false,
        ),
        ToolDefinition(
            name = FakeLlmClient.TOOL_GET_SCORE_BREAKDOWN,
            description = "Get all 13 HEIFA-2013 nutrition component scores and identify the weakest categories.",
            parametersSchema = emptyObjectSchema(),
            requiresConfirmation = false,
        ),
        ToolDefinition(
            name = FakeLlmClient.TOOL_SEARCH_FOOD_CATALOG,
            description = "Full-text search (FTS4) the on-device food catalog for nutrient profiles and food IDs.",
            parametersSchema = schemaWithRequiredString("query", "Search keyword such as salmon, oats, spinach, tofu"),
            requiresConfirmation = false,
        ),
        ToolDefinition(
            name = FakeLlmClient.TOOL_LOOKUP_FRUIT_NUTRITION,
            description = "Look up fruit nutrition per 100g (calories, sugar, carbs, protein, fat) from Fruityvice cache/API.",
            parametersSchema = schemaWithRequiredString("fruitName", "English fruit name such as banana, apple, blueberry"),
            requiresConfirmation = false,
        ),
        ToolDefinition(
            name = FakeLlmClient.TOOL_GET_USER_PREFERENCES,
            description = "Read the user's onboarding questionnaire profile (persona, preferred food groups, meal/sleep times).",
            parametersSchema = emptyObjectSchema(),
            requiresConfirmation = false,
        ),
        ToolDefinition(
            name = FakeLlmClient.TOOL_RETRIEVE_DIETARY_GUIDELINES,
            description = "Semantic vector RAG retrieval over the Australian Dietary Guidelines (ADG 2013) with citations.",
            parametersSchema = schemaWithRequiredString("query", "Nutrition guideline topic or question"),
            requiresConfirmation = false,
        ),
        ToolDefinition(
            name = FakeLlmClient.TOOL_LOG_MEAL,
            description = "Propose logging a food item into the user's meal log (requires user confirmation before writing).",
            parametersSchema = logMealSchema(),
            requiresConfirmation = true,
        ),
    )

    override suspend fun executeReadOnlyTool(userId: String, call: ToolCall): String {
        val args = parseJsonArgs(call.argumentsJson)
        return when (call.name) {
            FakeLlmClient.TOOL_GET_TODAY_SUMMARY -> executeGetTodaySummary(userId, args)
            FakeLlmClient.TOOL_GET_SCORE_BREAKDOWN -> executeGetScoreBreakdown(userId)
            FakeLlmClient.TOOL_SEARCH_FOOD_CATALOG -> executeSearchFoodCatalog(args)
            FakeLlmClient.TOOL_LOOKUP_FRUIT_NUTRITION -> executeLookupFruit(args)
            FakeLlmClient.TOOL_GET_USER_PREFERENCES -> executeGetUserPreferences(userId)
            FakeLlmClient.TOOL_RETRIEVE_DIETARY_GUIDELINES -> executeRetrieveDietaryGuidelines(args)
            else -> """{"error":"Unknown read-only tool '${call.name}'"}"""
        }
    }

    override suspend fun buildWriteProposal(userId: String, call: ToolCall): PendingMealWriteProposal {
        mealLoggingRepository.ensureCatalogSeeded()
        val args = parseJsonArgs(call.argumentsJson)
        val date = args.get("date")?.asString?.takeIf { it.isNotBlank() }
            ?: MealLoggingRepository.todayIsoDate()
        val mealTypeStr = args.get("mealType")?.asString?.uppercase() ?: "LUNCH"
        val mealType = runCatching { MealType.valueOf(mealTypeStr) }.getOrDefault(MealType.LUNCH)
        val foodQueryOrId = args.get("foodId")?.asString
            ?: args.get("foodName")?.asString
            ?: "veg_broccoli"
        val servings = args.get("servings")?.asDouble?.coerceIn(0.25, 10.0) ?: 1.0

        val allMatches = mealLoggingRepository.searchFoods("")
        val resolvedFood = allMatches.firstOrNull { it.id.equals(foodQueryOrId, ignoreCase = true) }
            ?: mealLoggingRepository.searchFoods(foodQueryOrId).firstOrNull()
            ?: allMatches.first()

        return PendingMealWriteProposal(
            toolCallId = call.id,
            date = date,
            mealType = mealType,
            items = listOf(
                ProposedFoodEntry(
                    foodId = resolvedFood.id,
                    foodName = resolvedFood.name,
                    servingSizeDesc = resolvedFood.servingSizeDesc,
                    servings = servings,
                    estimatedCaloriesKcal = resolvedFood.caloriesKcal * servings,
                ),
            ),
        )
    }

    override suspend fun commitWriteProposal(
        userId: String,
        proposal: PendingMealWriteProposal,
    ): String {
        var latestScore = mealLoggingRepository.getLatestDailyScore(userId)
        for (entry in proposal.items) {
            latestScore = mealLoggingRepository.logFood(
                userId = userId,
                date = proposal.date,
                mealType = proposal.mealType,
                foodId = entry.foodId,
                servings = entry.servings,
            )
        }
        return gson.toJson(
            mapOf(
                "status" to "confirmed_and_logged",
                "date" to proposal.date,
                "mealType" to proposal.mealType.name,
                "loggedItems" to proposal.items.map { "${it.servings}x ${it.foodName}" },
                "updatedHeifaTotalScore" to (latestScore?.heifaTotalScore ?: 0.0),
                "updatedDailyCaloriesKcal" to (latestScore?.totalCaloriesKcal ?: 0.0),
            ),
        )
    }

    private suspend fun executeGetTodaySummary(userId: String, args: JsonObject): String {
        val date = args.get("date")?.asString?.takeIf { it.isNotBlank() }
            ?: MealLoggingRepository.todayIsoDate()
        val meals = mealLoggingRepository.getMealsForDateFlow(userId, date).first()
        val score = mealLoggingRepository.getDailyScoreFlow(userId, date).first()
            ?: mealLoggingRepository.getLatestDailyScore(userId)

        val mealSummaries = meals.filter { it.items.isNotEmpty() }.map { m ->
            mapOf(
                "mealType" to m.meal.mealType.displayName,
                "caloriesKcal" to m.totalCaloriesKcal.roundToInt(),
                "items" to m.items.map { "${it.item.servings}x ${it.food.name}" },
            )
        }
        return gson.toJson(
            mapOf(
                "date" to (score?.date ?: date),
                "heifaTotalScore" to (score?.heifaTotalScore ?: 0.0),
                "totalCaloriesKcal" to (score?.totalCaloriesKcal ?: 0.0),
                "proteinG" to (score?.totalProteinG ?: 0.0),
                "carbsG" to (score?.totalCarbsG ?: 0.0),
                "fatG" to (score?.totalFatG ?: 0.0),
                "meals" to mealSummaries,
            ),
        )
    }

    private suspend fun executeGetScoreBreakdown(userId: String): String {
        val daily = mealLoggingRepository.getLatestDailyScore(userId)
        val patient = patientRepository.getPatientById(userId)
        val pairs = daily?.toScoreList() ?: patient?.toScoreList().orEmpty()
        val total = daily?.heifaTotalScore ?: patient?.heifaTotalScore ?: 0.0
        val weakest = pairs.sortedBy { it.second }.take(3).map { "${it.first} (${it.second})" }

        return gson.toJson(
            mapOf(
                "heifaTotalScore" to total,
                "lowestComponents" to weakest,
                "components" to pairs.toMap(),
            ),
        )
    }

    private suspend fun executeSearchFoodCatalog(args: JsonObject): String {
        val query = args.get("query")?.asString.orEmpty()
        val matches = mealLoggingRepository.searchFoods(query).take(5).map { food ->
            mapOf(
                "id" to food.id,
                "name" to food.name,
                "category" to food.category.displayName,
                "servingSize" to food.servingSizeDesc,
                "caloriesKcal" to food.caloriesKcal,
                "proteinG" to food.proteinG,
                "carbsG" to food.carbsG,
                "fatG" to food.fatG,
            )
        }
        return gson.toJson(mapOf("query" to query, "results" to matches))
    }

    private suspend fun executeLookupFruit(args: JsonObject): String {
        val fruitName = args.get("fruitName")?.asString ?: "banana"
        return when (val res = fruityviceRepository.getFruit(fruitName)) {
            is FruitResult.Success -> gson.toJson(
                mapOf(
                    "name" to res.data.name,
                    "source" to res.source.name,
                    "caloriesPer100g" to res.data.nutritions.calories,
                    "sugarG" to res.data.nutritions.sugar,
                    "carbsG" to res.data.nutritions.carbohydrates,
                    "proteinG" to res.data.nutritions.protein,
                    "fatG" to res.data.nutritions.fat,
                ),
            )
            else -> gson.toJson(
                mapOf(
                    "name" to fruitName,
                    "source" to "FALLBACK_ESTIMATE",
                    "caloriesPer100g" to 62.0,
                    "sugarG" to 11.5,
                    "carbsG" to 15.0,
                ),
            )
        }
    }

    private suspend fun executeGetUserPreferences(userId: String): String {
        val info = questionnaireRepository.getInfoForUser(userId)
        return if (info == null) {
            """{"questionnaireCompleted":false}"""
        } else {
            gson.toJson(
                mapOf(
                    "questionnaireCompleted" to true,
                    "persona" to info.persona.displayName,
                    "preferredCategories" to info.selectedCategories.map { it.displayName },
                    "biggestMealTime" to info.biggestMealTime,
                    "sleepTime" to info.sleepTime,
                    "wakeTime" to info.wakeTime,
                ),
            )
        }
    }

    private fun executeRetrieveDietaryGuidelines(args: JsonObject): String {
        val query = args.get("query")?.asString.orEmpty()
        val retrieved = DietaryGuidelinesRagEngine.retrieve(query, topK = 3).map { hit ->
            mapOf(
                "citation" to hit.chunk.citationTag,
                "section" to hit.chunk.sectionTitle,
                "similarityScore" to hit.score,
                "guideline" to hit.chunk.content,
            )
        }
        return gson.toJson(mapOf("query" to query, "citations" to retrieved))
    }

    private fun parseJsonArgs(raw: String): JsonObject = try {
        if (raw.isBlank()) JsonObject() else JsonParser.parseString(raw).asJsonObject
    } catch (_: Exception) {
        JsonObject()
    }

    private fun emptyObjectSchema(): JsonObject = JsonObject().apply {
        addProperty("type", "object")
        add("properties", JsonObject())
    }

    private fun schemaWithOptionalString(field: String, description: String): JsonObject = JsonObject().apply {
        addProperty("type", "object")
        add(
            "properties",
            JsonObject().apply {
                add(
                    field,
                    JsonObject().apply {
                        addProperty("type", "string")
                        addProperty("description", description)
                    },
                )
            },
        )
    }

    private fun schemaWithRequiredString(field: String, description: String): JsonObject =
        schemaWithOptionalString(field, description)

    private fun logMealSchema(): JsonObject = JsonObject().apply {
        addProperty("type", "object")
        add(
            "properties",
            JsonObject().apply {
                add("mealType", JsonObject().apply { addProperty("type", "string") })
                add("foodId", JsonObject().apply { addProperty("type", "string") })
                add("servings", JsonObject().apply { addProperty("type", "number") })
            },
        )
    }
}

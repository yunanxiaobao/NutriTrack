package com.songshawn.nutritrack.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.songshawn.nutritrack.data.models.Gender
import com.songshawn.nutritrack.data.models.HeifaFoodGroup
import com.songshawn.nutritrack.data.models.MealType
import com.songshawn.nutritrack.data.room.dao.DailyScoreAggregate
import com.songshawn.nutritrack.data.room.dao.DailyScoreDao
import com.songshawn.nutritrack.data.room.dao.FoodCatalogDao
import com.songshawn.nutritrack.data.room.dao.MealLogDao
import com.songshawn.nutritrack.data.room.dao.PatientDao
import com.songshawn.nutritrack.data.room.entity.DailyScoreEntity
import com.songshawn.nutritrack.data.room.entity.FoodItemEntity
import com.songshawn.nutritrack.data.room.entity.MealItemWithFood
import com.songshawn.nutritrack.data.room.entity.MealWithItems
import com.songshawn.nutritrack.domain.scoring.HeifaScoreEngine
import com.songshawn.nutritrack.domain.scoring.LoggedFoodPortion
import com.songshawn.nutritrack.util.AuthenticationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the on-device food catalog (FTS4 search), meal logging, and automatic
 * recalculation of daily HEIFA scores via [HeifaScoreEngine].
 */
@Singleton
class MealLoggingRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val foodCatalogDao: FoodCatalogDao,
    private val mealLogDao: MealLogDao,
    private val dailyScoreDao: DailyScoreDao,
    private val patientDao: PatientDao,
    private val gson: Gson,
) {
    private val seedMutex = Mutex()

    /**
     * Idempotently populates `food_items` from `assets/food_catalog.json` and rebuilds the FTS4 index.
     */
    suspend fun ensureCatalogSeeded() = withContext(Dispatchers.IO) {
        if (foodCatalogDao.count() > 0) return@withContext
        seedMutex.withLock {
            if (foodCatalogDao.count() > 0) return@withLock
            val json = appContext.assets.open(FOOD_CATALOG_ASSET)
                .bufferedReader()
                .use { it.readText() }
            val listType = object : TypeToken<List<FoodItemEntity>>() {}.type
            val items: List<FoodItemEntity> = gson.fromJson(json, listType)
            foodCatalogDao.insertAll(items)
            foodCatalogDao.rebuildFtsIndex()
        }
    }

    /**
     * Searches the food catalog using FTS4 prefix matching when [rawQuery] is non-blank,
     * optionally filtered by [category].
     */
    suspend fun searchFoods(
        rawQuery: String,
        category: HeifaFoodGroup? = null,
    ): List<FoodItemEntity> = withContext(Dispatchers.IO) {
        ensureCatalogSeeded()
        val ftsQuery = buildFtsPrefixQuery(rawQuery)
        val baseResults = if (ftsQuery.isEmpty()) {
            foodCatalogDao.getAllFoods()
        } else {
            try {
                foodCatalogDao.searchFoodsFts(ftsQuery)
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                foodCatalogDao.getAllFoods().filter {
                    it.name.contains(rawQuery.trim(), ignoreCase = true)
                }
            }
        }
        if (category == null) baseResults else baseResults.filter { it.category == category }
    }

    fun getMealsForDateFlow(userId: String, date: String): Flow<List<MealWithItems>> =
        mealLogDao.getMealsWithItemsForDateFlow(userId, date)

    fun getDailyScoreFlow(userId: String, date: String): Flow<DailyScoreEntity?> =
        dailyScoreDao.getDailyScoreFlow(userId, date)

    fun getLatestDailyScoreFlow(userId: String): Flow<DailyScoreEntity?> =
        dailyScoreDao.getLatestDailyScoreFlow(userId)

    suspend fun getLatestDailyScore(userId: String): DailyScoreEntity? =
        dailyScoreDao.getLatestDailyScore(userId)

    fun getRecentDailyScoresFlow(userId: String, days: Int): Flow<List<DailyScoreEntity>> =
        dailyScoreDao.getRecentDailyScoresFlow(userId, days)

    fun getRecentScoreAggregateFlow(userId: String, days: Int): Flow<DailyScoreAggregate> =
        dailyScoreDao.getRecentScoreAggregateFlow(userId, days)

    /**
     * Logs [servings] of [foodId] under [mealType] on [date] and recomputes that day's HEIFA score.
     */
    suspend fun logFood(
        userId: String,
        date: String = todayIsoDate(),
        mealType: MealType,
        foodId: String,
        servings: Double = 1.0,
    ): DailyScoreEntity = withContext(Dispatchers.IO) {
        ensureCatalogSeeded()
        mealLogDao.addFoodToMeal(
            userId = userId,
            date = date,
            mealType = mealType,
            foodId = foodId,
            servings = servings.coerceIn(0.25, 20.0),
        )
        recalculateAndPersistDailyScore(userId = userId, date = date)
    }

    /**
     * Removes a logged food line item and recomputes the day's HEIFA score.
     */
    suspend fun removeLoggedItem(
        userId: String,
        date: String,
        itemId: Long,
    ): DailyScoreEntity = withContext(Dispatchers.IO) {
        mealLogDao.deleteMealLogItem(itemId)
        recalculateAndPersistDailyScore(userId = userId, date = date)
    }

    /**
     * Updates the serving count of a logged item and recomputes the day's HEIFA score.
     */
    suspend fun updateItemServings(
        userId: String,
        date: String,
        itemId: Long,
        servings: Double,
    ): DailyScoreEntity = withContext(Dispatchers.IO) {
        if (servings <= 0.0) {
            mealLogDao.deleteMealLogItem(itemId)
        } else {
            mealLogDao.updateMealLogItemServings(itemId, servings.coerceIn(0.25, 20.0))
        }
        recalculateAndPersistDailyScore(userId = userId, date = date)
    }

    /**
     * Recomputes the HEIFA score for ([userId], [date]) from all logged meals on that date,
     * writes the [DailyScoreEntity], and mirrors the latest score into [PatientDao] so legacy
     * screens (Home, Insights, NutriCoach, Clinician) stay in sync.
     */
    suspend fun recalculateAndPersistDailyScore(
        userId: String,
        date: String,
        syncToPatientSnapshot: Boolean = true,
    ): DailyScoreEntity = withContext(Dispatchers.IO) {
        val meals = mealLogDao.getMealsWithItemsForDate(userId, date)
        val allItems = meals.flatMap { it.items }
        val existingPatient = patientDao.getPatientById(userId)
        val gender = existingPatient?.sex ?: Gender.FEMALE

        val breakdown = HeifaScoreEngine.compute(
            portions = allItems.map { it.toLoggedFoodPortion() },
            gender = gender,
        )
        val dailyScore = DailyScoreEntity(
            userId = userId,
            date = date,
            heifaTotalScore = breakdown.totalScore,
            vegetableScore = breakdown.vegetableScore,
            fruitScore = breakdown.fruitScore,
            grainsScore = breakdown.grainsScore,
            wholegrainsScore = breakdown.wholegrainsScore,
            meatAndAlternativeScore = breakdown.meatAndAlternativeScore,
            dairyScore = breakdown.dairyScore,
            waterScore = breakdown.waterScore,
            saturatedFatScore = breakdown.saturatedFatScore,
            unsaturatedFatScore = breakdown.unsaturatedFatScore,
            sodiumScore = breakdown.sodiumScore,
            sugarScore = breakdown.sugarScore,
            alcoholScore = breakdown.alcoholScore,
            discretionaryScore = breakdown.discretionaryScore,
            totalCaloriesKcal = breakdown.totalCaloriesKcal,
            totalProteinG = breakdown.totalProteinG,
            totalCarbsG = breakdown.totalCarbsG,
            totalFatG = breakdown.totalFatG,
        )
        dailyScoreDao.upsertDailyScore(dailyScore)

        if (syncToPatientSnapshot && allItems.isNotEmpty()) {
            val displayName = existingPatient?.name?.takeIf { it.isNotBlank() }
                ?: AuthenticationManager.getCurrentDisplayName()
                ?: "NutriTrack User"
            val updatedPatient = dailyScore.toPatientEntity(
                displayName = displayName,
                gender = gender,
                phoneNumber = existingPatient?.phoneNumber ?: "",
            )
            patientDao.insertPatient(updatedPatient)
        }
        dailyScore
    }

    companion object {
        private const val FOOD_CATALOG_ASSET = "food_catalog.json"

        fun todayIsoDate(): String = LocalDate.now().toString()

        /**
         * Sanitizes arbitrary user input into a safe SQLite FTS4 prefix query (`"oat* milk*"`).
         */
        fun buildFtsPrefixQuery(raw: String): String =
            raw.trim()
                .lowercase()
                .split(Regex("[^a-z0-9]+"))
                .filter { it.isNotBlank() }
                .joinToString(" ") { "$it*" }
    }
}

private fun MealItemWithFood.toLoggedFoodPortion(): LoggedFoodPortion = LoggedFoodPortion(
    foodId = food.id,
    category = food.category,
    subCategory = food.subCategory,
    servings = item.servings,
    caloriesKcalPerServe = food.caloriesKcal,
    proteinGPerServe = food.proteinG,
    fatGPerServe = food.fatG,
    saturatedFatGPerServe = food.saturatedFatG,
    unsaturatedFatGPerServe = food.unsaturatedFatG,
    carbsGPerServe = food.carbsG,
    sugarGPerServe = food.sugarG,
    addedSugarGPerServe = food.addedSugarG,
    fiberGPerServe = food.fiberG,
    sodiumMgPerServe = food.sodiumMg,
    waterMlPerServe = food.waterMl,
    alcoholStandardDrinksPerServe = food.alcoholStandardDrinks,
)

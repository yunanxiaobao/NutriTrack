package com.songshawn.nutritrack.util

import com.songshawn.nutritrack.data.models.FoodCategory
import com.songshawn.nutritrack.data.models.Gender
import com.songshawn.nutritrack.data.models.MealType
import com.songshawn.nutritrack.data.models.Persona
import com.songshawn.nutritrack.data.repository.MealLoggingRepository
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.data.room.dao.MealLogDao
import com.songshawn.nutritrack.data.room.dao.NutriCoachTipDao
import com.songshawn.nutritrack.data.room.dao.QuestionnaireInfoDao
import com.songshawn.nutritrack.data.room.entity.NutriCoachTipEntity
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import com.songshawn.nutritrack.data.room.entity.QuestionnaireInfoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds a 30-day meal history, HEIFA score trend, questionnaire profile, and AI coaching
 * history for [DemoDataConfig.DEMO_USER_ID] in `demo` builds.
 */
@Singleton
class DemoPersonaSeeder @Inject constructor(
    private val mealLoggingRepository: MealLoggingRepository,
    private val patientRepository: PatientRepository,
    private val mealLogDao: MealLogDao,
    private val questionnaireInfoDao: QuestionnaireInfoDao,
    private val nutriCoachTipDao: NutriCoachTipDao,
) {
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        mealLoggingRepository.ensureCatalogSeeded()
        if (!DemoDataConfig.isEnabled) return@withContext

        val demoUserId = DemoDataConfig.DEMO_USER_ID
        if (mealLoggingRepository.getLatestDailyScore(demoUserId) != null) return@withContext

        seedDemoProfileAndQuestionnaire(demoUserId)
        seedThirtyDaysOfMeals(demoUserId)
        seedDemoTips(demoUserId)
    }

    private suspend fun seedDemoProfileAndQuestionnaire(userId: String) {
        patientRepository.insertPatient(
            PatientEntity(
                userId = userId,
                phoneNumber = "0412345678",
                name = DemoDataConfig.DEMO_DISPLAY_NAME,
                passwordHash = "",
                sex = Gender.FEMALE,
                heifaTotalScore = 85.0,
                vegetableScore = 9.0,
                fruitScore = 10.0,
                grainsScore = 4.5,
                wholegrainsScore = 5.0,
                meatAndAlternativeScore = 9.5,
                dairyScore = 8.5,
                waterScore = 5.0,
                saturatedFatScore = 4.5,
                unsaturatedFatScore = 5.0,
                sodiumScore = 9.5,
                sugarScore = 9.5,
                alcoholScore = 5.0,
                discretionaryScore = 9.0,
                vegetablesVariantionsScore = 4.5,
                fruitVariantionsScore = 5.0,
                fruitServeSize = 2.0,
            ),
        )
        questionnaireInfoDao.insertInfo(
            QuestionnaireInfoEntity(
                patientId = userId,
                biggestMealTime = "18:30",
                sleepTime = "22:45",
                wakeTime = "06:45",
                persona = Persona.HEALTH_DEVOTEE,
                selectedCategories = listOf(
                    FoodCategory.VEGETABLES,
                    FoodCategory.FRUITS,
                    FoodCategory.GRAINS,
                    FoodCategory.SEAFOOD,
                    FoodCategory.POULTRY,
                    FoodCategory.EGGS,
                    FoodCategory.NUTS_SEEDS,
                ),
            ),
        )
    }

    private suspend fun seedThirtyDaysOfMeals(userId: String) {
        val today = LocalDate.now()
        for (daysAgo in 29 downTo 0) {
            val dateStr = today.minusDays(daysAgo.toLong()).toString()
            val progressIndex = 29 - daysAgo // 0 (oldest) .. 29 (today)
            seedMealsForSingleDay(userId, dateStr, progressIndex)
            mealLoggingRepository.recalculateAndPersistDailyScore(
                userId = userId,
                date = dateStr,
                syncToPatientSnapshot = (daysAgo == 0),
            )
        }
    }

    private suspend fun seedMealsForSingleDay(userId: String, date: String, progressIndex: Int) {
        // Breakfast evolves from simple toast to oats + berries + Greek yogurt
        if (progressIndex < 10) {
            mealLogDao.addFoodToMeal(userId, date, MealType.BREAKFAST, "grain_wholemeal_bread", 1.5)
            mealLogDao.addFoodToMeal(userId, date, MealType.BREAKFAST, "meat_eggs", 1.0)
        } else {
            mealLogDao.addFoodToMeal(userId, date, MealType.BREAKFAST, "grain_rolled_oats", 1.5)
            mealLogDao.addFoodToMeal(userId, date, MealType.BREAKFAST, "fruit_blueberries", 1.0)
            mealLogDao.addFoodToMeal(userId, date, MealType.BREAKFAST, "dairy_greek_yogurt", 1.0)
        }

        // Lunch adds more vegetable diversity and whole grains over time
        val vegServes = 1.0 + (progressIndex / 15.0)
        mealLogDao.addFoodToMeal(userId, date, MealType.LUNCH, "meat_chicken_breast", 1.0)
        mealLogDao.addFoodToMeal(userId, date, MealType.LUNCH, "veg_spinach", vegServes)
        mealLogDao.addFoodToMeal(userId, date, MealType.LUNCH, "veg_tomato", 1.0)
        if (progressIndex >= 8) {
            mealLogDao.addFoodToMeal(userId, date, MealType.LUNCH, "grain_quinoa", 1.5)
            mealLogDao.addFoodToMeal(userId, date, MealType.LUNCH, "fat_avocado", 1.0)
        } else {
            mealLogDao.addFoodToMeal(userId, date, MealType.LUNCH, "grain_white_rice", 1.5)
            mealLogDao.addFoodToMeal(userId, date, MealType.LUNCH, "disc_cola", 1.0)
        }

        // Dinner shifts from occasional discretionary items to salmon + colorful vegetables
        val dinnerProtein = if (progressIndex % 2 == 0) "meat_salmon" else "meat_tofu"
        mealLogDao.addFoodToMeal(userId, date, MealType.DINNER, dinnerProtein, 1.2)
        mealLogDao.addFoodToMeal(userId, date, MealType.DINNER, "veg_broccoli", 1.5)
        mealLogDao.addFoodToMeal(userId, date, MealType.DINNER, "grain_brown_rice", 1.5)
        if (progressIndex >= 14) {
            mealLogDao.addFoodToMeal(userId, date, MealType.DINNER, "veg_carrot", 1.0)
            mealLogDao.addFoodToMeal(userId, date, MealType.DINNER, "veg_lentils", 1.0)
            mealLogDao.addFoodToMeal(userId, date, MealType.DINNER, "fat_olive_oil", 1.0)
        } else if (progressIndex % 3 == 0) {
            mealLogDao.addFoodToMeal(userId, date, MealType.DINNER, "disc_fries", 1.0)
        }

        // Snacks & hydration improve across the 30-day window
        val waterGlasses = if (progressIndex >= 12) 3.0 else 1.5
        mealLogDao.addFoodToMeal(userId, date, MealType.SNACK, "water_glass", waterGlasses)
        if (progressIndex >= 6) {
            val secondFruit = if (progressIndex % 2 == 0) "fruit_orange" else "fruit_apple"
            mealLogDao.addFoodToMeal(userId, date, MealType.SNACK, secondFruit, 1.0)
            mealLogDao.addFoodToMeal(userId, date, MealType.SNACK, "dairy_soy_milk", 1.0)
        }
    }

    private suspend fun seedDemoTips(userId: String) {
        val now = System.currentTimeMillis()
        val tips = listOf(
            NutriCoachTipEntity(
                userId = userId,
                prompt = "Daily coaching review",
                tipContent = "**Great momentum on vegetable variety!** Adding lentils and carrots alongside " +
                    "dark greens pushed your Vegetable HEIFA score to 10/10. Keep pairing wholegrain oats " +
                    "with berries at breakfast for sustained fiber.",
                timestamp = Date(now - 86_400_000L * 2),
            ),
            NutriCoachTipEntity(
                userId = userId,
                prompt = "Omega-3 & healthy fats check",
                tipContent = "**Healthy fat balance looks strong.** Rotating Atlantic salmon, extra-virgin " +
                    "olive oil, and avocado keeps your unsaturated fat score at 5/5 while keeping saturated " +
                    "fat under 8% of total energy.",
                timestamp = Date(now - 86_400_000L),
            ),
        )
        tips.forEach { nutriCoachTipDao.insertTip(it) }
    }
}

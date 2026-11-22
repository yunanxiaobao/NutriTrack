package com.songshawn.nutritrack.domain.scoring

import com.songshawn.nutritrack.data.models.Gender
import com.songshawn.nutritrack.data.models.HeifaFoodGroup
import kotlin.math.round

/**
 * Pure-Kotlin input item for [HeifaScoreEngine], detached from Room or Android APIs.
 */
data class LoggedFoodPortion(
    val foodId: String = "item",
    val category: HeifaFoodGroup,
    val subCategory: String = "default",
    val servings: Double = 1.0,
    val caloriesKcalPerServe: Double = 100.0,
    val proteinGPerServe: Double = 5.0,
    val fatGPerServe: Double = 3.0,
    val saturatedFatGPerServe: Double = 0.5,
    val unsaturatedFatGPerServe: Double = 2.0,
    val carbsGPerServe: Double = 15.0,
    val sugarGPerServe: Double = 2.0,
    val addedSugarGPerServe: Double = 0.0,
    val fiberGPerServe: Double = 2.0,
    val sodiumMgPerServe: Double = 50.0,
    val waterMlPerServe: Double = 50.0,
    val alcoholStandardDrinksPerServe: Double = 0.0,
)

/**
 * Output of [HeifaScoreEngine.compute]: 13 HEIFA-2013 component scores (summing to 0..100)
 * plus daily macronutrient totals.
 */
data class DailyHeifaBreakdown(
    val totalScore: Double = 0.0,
    val vegetableScore: Double = 0.0,
    val fruitScore: Double = 0.0,
    val grainsScore: Double = 0.0,
    val wholegrainsScore: Double = 0.0,
    val meatAndAlternativeScore: Double = 0.0,
    val dairyScore: Double = 0.0,
    val waterScore: Double = 0.0,
    val saturatedFatScore: Double = 0.0,
    val unsaturatedFatScore: Double = 0.0,
    val sodiumScore: Double = 0.0,
    val sugarScore: Double = 0.0,
    val alcoholScore: Double = 0.0,
    val discretionaryScore: Double = 0.0,
    val totalCaloriesKcal: Double = 0.0,
    val totalProteinG: Double = 0.0,
    val totalCarbsG: Double = 0.0,
    val totalFatG: Double = 0.0,
)

/**
 * Pure-Kotlin implementation of the Australian HEIFA-2013 (Healthy Eating Index for Australian Adults)
 * scoring algorithm.
 */
object HeifaScoreEngine {

    private const val VEG_VARIETY_TARGET = 4.0
    private const val FRUIT_VARIETY_TARGET = 2.0
    private const val FRUIT_SERVE_TARGET = 2.0
    private const val GRAIN_SERVE_TARGET = 6.0
    private const val WHOLE_GRAIN_SERVE_TARGET = 3.0
    private const val DAIRY_SERVE_TARGET = 2.5
    private const val WATER_ML_TARGET = 1500.0

    private const val SAT_FAT_KCAL_PER_GRAM = 9.0
    private const val SUGAR_KCAL_PER_GRAM = 4.0
    private const val SAT_FAT_LOW_PCT = 0.10
    private const val SAT_FAT_HIGH_PCT = 0.15
    private const val ADDED_SUGAR_LOW_PCT = 0.05
    private const val ADDED_SUGAR_HIGH_PCT = 0.15
    private const val SODIUM_LOW_MG = 2000.0
    private const val SODIUM_HIGH_MG = 3500.0
    private const val ALCOHOL_SAFE_DRINKS = 2.0
    private const val ALCOHOL_MAX_DRINKS = 4.0

    fun compute(
        portions: List<LoggedFoodPortion>,
        gender: Gender = Gender.FEMALE,
    ): DailyHeifaBreakdown {
        val valid = portions.filter { it.servings > 0.0 }
        if (valid.isEmpty()) return DailyHeifaBreakdown()

        val totalCalories = valid.sumOf { it.caloriesKcalPerServe * it.servings }
        val energyBase = totalCalories.coerceAtLeast(1200.0)

        val vegetableScore = computeGroupWithVarietyScore(
            portions = valid,
            group = HeifaFoodGroup.VEGETABLES,
            serveTarget = if (gender == Gender.MALE) 6.0 else 5.0,
            varietyTarget = VEG_VARIETY_TARGET,
        )
        val fruitScore = computeGroupWithVarietyScore(
            portions = valid,
            group = HeifaFoodGroup.FRUITS,
            serveTarget = FRUIT_SERVE_TARGET,
            varietyTarget = FRUIT_VARIETY_TARGET,
        )
        val grainsScore = round1(
            linearUp(
                valid.servesOf(HeifaFoodGroup.GRAINS, HeifaFoodGroup.WHOLE_GRAINS),
                GRAIN_SERVE_TARGET,
                maxScore = 5.0,
            ),
        )
        val wholegrainsScore = round1(
            linearUp(valid.servesOf(HeifaFoodGroup.WHOLE_GRAINS), WHOLE_GRAIN_SERVE_TARGET, maxScore = 5.0),
        )
        val meatScore = round1(
            linearUp(
                valid.servesOf(HeifaFoodGroup.MEAT_AND_ALTERNATIVES),
                if (gender == Gender.MALE) 3.0 else 2.5,
                maxScore = 10.0,
            ),
        )
        val dairyScore = round1(
            linearUp(valid.servesOf(HeifaFoodGroup.DAIRY_AND_ALTERNATIVES), DAIRY_SERVE_TARGET, maxScore = 10.0),
        )
        val waterScore = round1(
            linearUp(valid.sumOf { it.waterMlPerServe * it.servings }, WATER_ML_TARGET, maxScore = 5.0),
        )
        val unsatFatServes = valid.servesOf(HeifaFoodGroup.UNSATURATED_FATS) +
            valid.sumOf { (it.unsaturatedFatGPerServe * it.servings) / 10.0 }
        val unsaturatedFatScore = round1(
            linearUp(unsatFatServes, if (gender == Gender.MALE) 4.0 else 2.0, maxScore = 5.0),
        )

        val moderation = computeModerationScores(valid, gender, energyBase)
        val totalScore = round1(
            (
                vegetableScore + fruitScore + grainsScore + wholegrainsScore +
                    meatScore + dairyScore + waterScore + unsaturatedFatScore +
                    moderation.saturatedFatScore + moderation.sodiumScore +
                    moderation.sugarScore + moderation.alcoholScore + moderation.discretionaryScore
                ).coerceIn(0.0, 100.0),
        )

        return DailyHeifaBreakdown(
            totalScore = totalScore,
            vegetableScore = vegetableScore,
            fruitScore = fruitScore,
            grainsScore = grainsScore,
            wholegrainsScore = wholegrainsScore,
            meatAndAlternativeScore = meatScore,
            dairyScore = dairyScore,
            waterScore = waterScore,
            saturatedFatScore = moderation.saturatedFatScore,
            unsaturatedFatScore = unsaturatedFatScore,
            sodiumScore = moderation.sodiumScore,
            sugarScore = moderation.sugarScore,
            alcoholScore = moderation.alcoholScore,
            discretionaryScore = moderation.discretionaryScore,
            totalCaloriesKcal = round1(totalCalories),
            totalProteinG = round1(valid.sumOf { it.proteinGPerServe * it.servings }),
            totalCarbsG = round1(valid.sumOf { it.carbsGPerServe * it.servings }),
            totalFatG = round1(valid.sumOf { it.fatGPerServe * it.servings }),
        )
    }

    private data class ModerationScores(
        val saturatedFatScore: Double,
        val sodiumScore: Double,
        val sugarScore: Double,
        val alcoholScore: Double,
        val discretionaryScore: Double,
    )

    private fun computeGroupWithVarietyScore(
        portions: List<LoggedFoodPortion>,
        group: HeifaFoodGroup,
        serveTarget: Double,
        varietyTarget: Double,
    ): Double {
        val serves = portions.servesOf(group)
        val varietyCount = portions
            .filter { it.category == group && it.subCategory.isNotBlank() }
            .map { it.subCategory.lowercase() }
            .distinct()
            .size
        val quantityPts = linearUp(serves, serveTarget, maxScore = 5.0)
        val varietyPts = linearUp(varietyCount.toDouble(), varietyTarget, maxScore = 5.0)
        return round1(quantityPts + varietyPts)
    }

    private fun computeModerationScores(
        valid: List<LoggedFoodPortion>,
        gender: Gender,
        energyBase: Double,
    ): ModerationScores {
        val totalSatFatG = valid.sumOf { it.saturatedFatGPerServe * it.servings }
        val totalAddedSugarG = valid.sumOf { it.addedSugarGPerServe * it.servings }
        val totalSodiumMg = valid.sumOf { it.sodiumMgPerServe * it.servings }
        val totalAlcoholDrinks = valid.sumOf { it.alcoholStandardDrinksPerServe * it.servings }
        val discretionaryServes = valid.servesOf(HeifaFoodGroup.DISCRETIONARY)

        val satFatPct = (totalSatFatG * SAT_FAT_KCAL_PER_GRAM) / energyBase
        val addedSugarPct = (totalAddedSugarG * SUGAR_KCAL_PER_GRAM) / energyBase
        val discZero = if (gender == Gender.MALE) 4.0 else 3.5

        return ModerationScores(
            saturatedFatScore = round1(linearDown(satFatPct, SAT_FAT_LOW_PCT, SAT_FAT_HIGH_PCT, 5.0)),
            sodiumScore = round1(linearDown(totalSodiumMg, SODIUM_LOW_MG, SODIUM_HIGH_MG, 10.0)),
            sugarScore = round1(linearDown(addedSugarPct, ADDED_SUGAR_LOW_PCT, ADDED_SUGAR_HIGH_PCT, 10.0)),
            alcoholScore = round1(linearDown(totalAlcoholDrinks, ALCOHOL_SAFE_DRINKS, ALCOHOL_MAX_DRINKS, 5.0)),
            discretionaryScore = round1(linearDown(discretionaryServes, 1.0, discZero, 10.0)),
        )
    }

    private fun List<LoggedFoodPortion>.servesOf(vararg groups: HeifaFoodGroup): Double =
        filter { it.category in groups }.sumOf { it.servings }

    private fun linearUp(value: Double, target: Double, maxScore: Double): Double {
        if (target <= 0.0) return 0.0
        return ((value / target).coerceIn(0.0, 1.0)) * maxScore
    }

    private fun linearDown(
        value: Double,
        bestAtOrBelow: Double,
        zeroAtOrAbove: Double,
        maxScore: Double,
    ): Double {
        if (value <= bestAtOrBelow) return maxScore
        if (value >= zeroAtOrAbove) return 0.0
        val ratio = 1.0 - ((value - bestAtOrBelow) / (zeroAtOrAbove - bestAtOrBelow))
        return ratio.coerceIn(0.0, 1.0) * maxScore
    }

    private fun round1(value: Double): Double = round(value * 10.0) / 10.0
}

package com.songshawn.nutritrack.domain.scoring

import com.songshawn.nutritrack.data.models.Gender
import com.songshawn.nutritrack.data.models.HeifaFoodGroup
import com.songshawn.nutritrack.data.repository.MealLoggingRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeifaScoreEngineTest {

    @Test
    fun `empty portions produce all-zero score breakdown`() {
        val result = HeifaScoreEngine.compute(emptyList(), Gender.FEMALE)
        assertEquals(0.0, result.totalScore, 0.001)
        assertEquals(0.0, result.vegetableScore, 0.001)
        assertEquals(0.0, result.fruitScore, 0.001)
        assertEquals(0.0, result.totalCaloriesKcal, 0.001)
    }

    @Test
    fun `zero or negative servings are ignored as empty day`() {
        val result = HeifaScoreEngine.compute(
            listOf(LoggedFoodPortion(category = HeifaFoodGroup.FRUITS, servings = 0.0)),
            Gender.MALE,
        )
        assertEquals(0.0, result.totalScore, 0.001)
    }

    @Test
    fun `vegetable score rewards both quantity and distinct sub-category variety`() {
        // Female target is 5 serves (5 pts) + 4 distinct sub-categories (5 pts) = 10 pts
        val diverseVeg = listOf(
            LoggedFoodPortion(foodId = "v1", category = HeifaFoodGroup.VEGETABLES, subCategory = "dark_green", servings = 1.5),
            LoggedFoodPortion(foodId = "v2", category = HeifaFoodGroup.VEGETABLES, subCategory = "cruciferous", servings = 1.5),
            LoggedFoodPortion(foodId = "v3", category = HeifaFoodGroup.VEGETABLES, subCategory = "orange_red", servings = 1.0),
            LoggedFoodPortion(foodId = "v4", category = HeifaFoodGroup.VEGETABLES, subCategory = "legumes", servings = 1.0),
        )
        val monoVeg = listOf(
            LoggedFoodPortion(foodId = "v1", category = HeifaFoodGroup.VEGETABLES, subCategory = "dark_green", servings = 5.0),
        )

        val diverseBreakdown = HeifaScoreEngine.compute(diverseVeg, Gender.FEMALE)
        val monoBreakdown = HeifaScoreEngine.compute(monoVeg, Gender.FEMALE)

        assertEquals(10.0, diverseBreakdown.vegetableScore, 0.01)
        // Mono veg hits full 5 pts quantity, but only 1/4 of 5 pts variety = 6.3 pts
        assertEquals(6.3, monoBreakdown.vegetableScore, 0.1)
    }

    @Test
    fun `male vegetable target requires 6 serves instead of 5`() {
        val fiveServes = listOf(
            LoggedFoodPortion(category = HeifaFoodGroup.VEGETABLES, subCategory = "dark_green", servings = 5.0),
        )
        val female = HeifaScoreEngine.compute(fiveServes, Gender.FEMALE)
        val male = HeifaScoreEngine.compute(fiveServes, Gender.MALE)
        assertTrue(female.vegetableScore > male.vegetableScore)
    }

    @Test
    fun `fruit score reaches 10 with 2 serves across 2 distinct sub-categories`() {
        val fruits = listOf(
            LoggedFoodPortion(foodId = "f1", category = HeifaFoodGroup.FRUITS, subCategory = "citrus", servings = 1.0),
            LoggedFoodPortion(foodId = "f2", category = HeifaFoodGroup.FRUITS, subCategory = "berries", servings = 1.0),
        )
        val breakdown = HeifaScoreEngine.compute(fruits, Gender.FEMALE)
        assertEquals(10.0, breakdown.fruitScore, 0.01)
    }

    @Test
    fun `whole grains contribute to both total grains and wholegrains scores`() {
        val threeWholeGrains = listOf(
            LoggedFoodPortion(category = HeifaFoodGroup.WHOLE_GRAINS, servings = 3.0),
        )
        val breakdown = HeifaScoreEngine.compute(threeWholeGrains, Gender.FEMALE)
        assertEquals(2.5, breakdown.grainsScore, 0.01)
        assertEquals(5.0, breakdown.wholegrainsScore, 0.01)
    }

    @Test
    fun `excess sodium linearly reduces sodium score to zero at 3500mg`() {
        val lowSodium = listOf(LoggedFoodPortion(category = HeifaFoodGroup.VEGETABLES, sodiumMgPerServe = 1500.0))
        val midSodium = listOf(LoggedFoodPortion(category = HeifaFoodGroup.VEGETABLES, sodiumMgPerServe = 2750.0))
        val highSodium = listOf(LoggedFoodPortion(category = HeifaFoodGroup.VEGETABLES, sodiumMgPerServe = 3800.0))

        assertEquals(10.0, HeifaScoreEngine.compute(lowSodium).sodiumScore, 0.01)
        assertEquals(5.0, HeifaScoreEngine.compute(midSodium).sodiumScore, 0.01)
        assertEquals(0.0, HeifaScoreEngine.compute(highSodium).sodiumScore, 0.01)
    }

    @Test
    fun `added sugar and saturated fat moderation scores penalize high intake`() {
        val sugaryFattyMeal = listOf(
            LoggedFoodPortion(
                category = HeifaFoodGroup.DISCRETIONARY,
                servings = 4.0,
                caloriesKcalPerServe = 500.0,
                saturatedFatGPerServe = 10.0,
                addedSugarGPerServe = 15.0,
            ),
        )
        val breakdown = HeifaScoreEngine.compute(sugaryFattyMeal, Gender.FEMALE)
        assertEquals(0.0, breakdown.saturatedFatScore, 0.01)
        assertEquals(3.0, breakdown.sugarScore, 0.01)
        assertEquals(0.0, breakdown.discretionaryScore, 0.01)
    }

    @Test
    fun `balanced whole-food daily diet achieves near-100 HEIFA total score`() {
        val balancedDay = listOf(
            LoggedFoodPortion("v1", HeifaFoodGroup.VEGETABLES, "dark_green", servings = 1.5, caloriesKcalPerServe = 30.0),
            LoggedFoodPortion("v2", HeifaFoodGroup.VEGETABLES, "cruciferous", servings = 1.5, caloriesKcalPerServe = 30.0),
            LoggedFoodPortion("v3", HeifaFoodGroup.VEGETABLES, "orange_red", servings = 1.0, caloriesKcalPerServe = 35.0),
            LoggedFoodPortion("v4", HeifaFoodGroup.VEGETABLES, "legumes", servings = 1.0, caloriesKcalPerServe = 85.0),
            LoggedFoodPortion("f1", HeifaFoodGroup.FRUITS, "berries", servings = 1.0, caloriesKcalPerServe = 70.0),
            LoggedFoodPortion("f2", HeifaFoodGroup.FRUITS, "citrus", servings = 1.0, caloriesKcalPerServe = 70.0),
            LoggedFoodPortion("g1", HeifaFoodGroup.WHOLE_GRAINS, "oats", servings = 3.0, caloriesKcalPerServe = 115.0),
            LoggedFoodPortion("g2", HeifaFoodGroup.GRAINS, "rice", servings = 3.0, caloriesKcalPerServe = 120.0),
            LoggedFoodPortion("m1", HeifaFoodGroup.MEAT_AND_ALTERNATIVES, "seafood", servings = 2.5, caloriesKcalPerServe = 180.0),
            LoggedFoodPortion("d1", HeifaFoodGroup.DAIRY_AND_ALTERNATIVES, "yogurt", servings = 2.5, caloriesKcalPerServe = 130.0),
            LoggedFoodPortion("w1", HeifaFoodGroup.WATER, "water", servings = 3.0, caloriesKcalPerServe = 0.0, waterMlPerServe = 500.0),
            LoggedFoodPortion("o1", HeifaFoodGroup.UNSATURATED_FATS, "olive_oil", servings = 2.0, caloriesKcalPerServe = 88.0),
        )

        val breakdown = HeifaScoreEngine.compute(balancedDay, Gender.FEMALE)
        assertEquals(100.0, breakdown.totalScore, 0.1)
        assertTrue(breakdown.totalCaloriesKcal > 1500.0)
    }

    @Test
    fun `buildFtsPrefixQuery sanitizes punctuation into wildcard tokens`() {
        assertEquals("grilled* chicken*", MealLoggingRepository.buildFtsPrefixQuery("  Grilled, Chicken!! "))
        assertEquals("oat* 100*", MealLoggingRepository.buildFtsPrefixQuery("oat (100%)"))
        assertEquals("", MealLoggingRepository.buildFtsPrefixQuery("   ...  "))
    }
}

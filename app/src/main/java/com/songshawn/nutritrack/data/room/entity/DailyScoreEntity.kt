package com.songshawn.nutritrack.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import com.songshawn.nutritrack.data.models.Gender

/**
 * Computed daily HEIFA-style nutrition score and macronutrient summary for a user on an ISO date (`YYYY-MM-DD`).
 */
@Entity(
    tableName = "daily_scores",
    primaryKeys = ["userId", "date"],
    indices = [Index(value = ["date"])],
)
data class DailyScoreEntity(
    val userId: String,
    val date: String,
    val heifaTotalScore: Double,
    val vegetableScore: Double,
    val fruitScore: Double,
    val grainsScore: Double,
    val wholegrainsScore: Double,
    val meatAndAlternativeScore: Double,
    val dairyScore: Double,
    val waterScore: Double,
    val saturatedFatScore: Double,
    val unsaturatedFatScore: Double,
    val sodiumScore: Double,
    val sugarScore: Double,
    val alcoholScore: Double,
    val discretionaryScore: Double,
    val totalCaloriesKcal: Double,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun toScoreList(): List<Pair<String, Double>> = listOf(
        "Vegetables" to vegetableScore,
        "Fruits" to fruitScore,
        "Grains & Cereals" to grainsScore,
        "Whole Grains" to wholegrainsScore,
        "Meat & Alternatives" to meatAndAlternativeScore,
        "Dairy & Alternatives" to dairyScore,
        "Water" to waterScore,
        "Saturated Fats" to saturatedFatScore,
        "Unsaturated Fats" to unsaturatedFatScore,
        "Sodium" to sodiumScore,
        "Sugar" to sugarScore,
        "Alcohol" to alcoholScore,
        "Discretionary Foods" to discretionaryScore,
    )

    fun toPatientEntity(
        displayName: String,
        gender: Gender = Gender.FEMALE,
        phoneNumber: String = "",
    ): PatientEntity = PatientEntity(
        userId = userId,
        phoneNumber = phoneNumber,
        name = displayName,
        passwordHash = "",
        sex = gender,
        heifaTotalScore = heifaTotalScore,
        vegetableScore = vegetableScore,
        fruitScore = fruitScore,
        grainsScore = grainsScore,
        wholegrainsScore = wholegrainsScore,
        meatAndAlternativeScore = meatAndAlternativeScore,
        dairyScore = dairyScore,
        waterScore = waterScore,
        saturatedFatScore = saturatedFatScore,
        unsaturatedFatScore = unsaturatedFatScore,
        sodiumScore = sodiumScore,
        sugarScore = sugarScore,
        alcoholScore = alcoholScore,
        discretionaryScore = discretionaryScore,
        vegetablesVariantionsScore = (vegetableScore / 2.0).coerceIn(0.0, 5.0),
        fruitVariantionsScore = (fruitScore / 2.0).coerceIn(0.0, 5.0),
        fruitServeSize = (fruitScore / 5.0).coerceIn(0.0, 2.0),
    )
}

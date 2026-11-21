package com.songshawn.nutritrack.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.songshawn.nutritrack.data.models.Gender

@Entity(tableName = "patient")
data class PatientEntity(
    @PrimaryKey val userId: String,
    val phoneNumber: String,
    val name: String,
    val passwordHash: String = "",
    val sex: Gender? = null,

    // HEIFA score breakdowns
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
    val vegetablesVariantionsScore: Double,
    val fruitVariantionsScore: Double,
    val fruitServeSize: Double,

    )

fun PatientEntity.toScoreList(): List<Pair<String, Double>> {
    return listOf(
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
        "Discretionary Foods" to discretionaryScore
    )
}
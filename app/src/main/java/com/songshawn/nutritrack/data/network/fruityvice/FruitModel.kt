package com.songshawn.nutritrack.data.network.fruityvice

data class FruitDto(
    val name: String,
    val family: String,
    val order: String,
    val genus: String,
    val nutritions: Nutrition
)

data class Nutrition(
    val carbohydrates: Float,
    val protein: Float,
    val fat: Float,
    val calories: Float,
    val sugar: Float
)
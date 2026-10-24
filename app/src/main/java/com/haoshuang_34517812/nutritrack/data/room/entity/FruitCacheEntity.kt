package com.haoshuang_34517812.nutritrack.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.haoshuang_34517812.nutritrack.data.network.fruityvice.FruitDto
import com.haoshuang_34517812.nutritrack.data.network.fruityvice.Nutrition

/**
 * Room Entity for caching fruit data from Fruityvice API.
 * Implements cache-first strategy to reduce network requests and improve
 * user experience in weak network conditions.
 */
@Entity(tableName = "fruit_cache")
data class FruitCacheEntity(
    @PrimaryKey
    val name: String,           // Fruit name as primary key (lowercase for consistency)
    val family: String,
    val order: String,
    val genus: String,
    // Nutrition data (flattened for Room)
    val carbohydrates: Float,
    val protein: Float,
    val fat: Float,
    val calories: Float,
    val sugar: Float,
    // Cache metadata
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts this entity back to FruitDto for UI consumption
     */
    fun toFruitDto(): FruitDto = FruitDto(
        name = name,
        family = family,
        order = order,
        genus = genus,
        nutritions = Nutrition(
            carbohydrates = carbohydrates,
            protein = protein,
            fat = fat,
            calories = calories,
            sugar = sugar
        )
    )

    companion object {
        /**
         * Creates a FruitCacheEntity from a FruitDto
         */
        fun fromFruitDto(dto: FruitDto): FruitCacheEntity = FruitCacheEntity(
            name = dto.name.lowercase(),  // Normalize to lowercase for consistent lookups
            family = dto.family,
            order = dto.order,
            genus = dto.genus,
            carbohydrates = dto.nutritions.carbohydrates,
            protein = dto.nutritions.protein,
            fat = dto.nutritions.fat,
            calories = dto.nutritions.calories,
            sugar = dto.nutritions.sugar
        )

        // Cache expiration time (7 days in milliseconds)
        // Fruit nutrition data rarely changes, so a long cache time is appropriate
        const val CACHE_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000
    }

    /**
     * Checks if this cached entry has expired
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - cachedAt > CACHE_EXPIRATION_MS
    }
}

package com.songshawn.nutritrack.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import com.songshawn.nutritrack.data.models.HeifaFoodGroup

/**
 * A food or beverage entry in the on-device nutrition catalog.
 *
 * All nutrient values correspond to **one standard serve** ([servingSizeDesc], [servingGrams]).
 */
@Entity(
    tableName = "food_items",
    indices = [Index(value = ["category"])],
)
data class FoodItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: HeifaFoodGroup,
    /** Sub-group used for vegetable/fruit variety scoring (e.g. "dark_green", "citrus", "berry"). */
    val subCategory: String,
    val servingSizeDesc: String,
    val servingGrams: Double,
    val caloriesKcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val saturatedFatG: Double,
    val unsaturatedFatG: Double,
    val carbsG: Double,
    val sugarG: Double,
    val addedSugarG: Double,
    val fiberG: Double,
    val sodiumMg: Double,
    val waterMl: Double = 0.0,
    val alcoholStandardDrinks: Double = 0.0,
)

/**
 * FTS4 virtual table backing fast prefix and token search over [FoodItemEntity].
 */
@Fts4(contentEntity = FoodItemEntity::class)
@Entity(tableName = "food_items_fts")
data class FoodItemFtsEntity(
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "subCategory")
    val subCategory: String,
    @ColumnInfo(name = "servingSizeDesc")
    val servingSizeDesc: String,
)

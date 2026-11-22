package com.songshawn.nutritrack.data.room.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.songshawn.nutritrack.data.models.MealType

/**
 * Header record for a user's meal slot on a given ISO date (`YYYY-MM-DD`).
 */
@Entity(
    tableName = "meal_logs",
    indices = [
        Index(value = ["userId", "date"]),
        Index(value = ["userId", "date", "mealType"], unique = true),
    ],
)
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: String,
    val mealType: MealType,
    val loggedAt: Long = System.currentTimeMillis(),
)

/**
 * Individual food line item belonging to a [MealLogEntity].
 *
 * Linked to [MealLogEntity] with `ON DELETE CASCADE` so deleting a meal removes its items,
 * and to [FoodItemEntity] with `ON DELETE RESTRICT` so referenced catalog entries stay intact.
 */
@Entity(
    tableName = "meal_log_items",
    foreignKeys = [
        ForeignKey(
            entity = MealLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealLogId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["mealLogId"]),
        Index(value = ["foodId"]),
    ],
)
data class MealLogItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealLogId: Long,
    val foodId: String,
    val servings: Double,
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * Joined projection pairing a [MealLogItemEntity] with its catalog [FoodItemEntity].
 */
data class MealItemWithFood(
    @Embedded
    val item: MealLogItemEntity,
    @Relation(
        parentColumn = "foodId",
        entityColumn = "id",
    )
    val food: FoodItemEntity,
) {
    val totalCaloriesKcal: Double get() = food.caloriesKcal * item.servings
    val totalProteinG: Double get() = food.proteinG * item.servings
    val totalCarbsG: Double get() = food.carbsG * item.servings
    val totalFatG: Double get() = food.fatG * item.servings
}

/**
 * Relational graph for an entire meal slot and all of its food items.
 */
data class MealWithItems(
    @Embedded
    val meal: MealLogEntity,
    @Relation(
        entity = MealLogItemEntity::class,
        parentColumn = "id",
        entityColumn = "mealLogId",
    )
    val items: List<MealItemWithFood>,
) {
    val totalCaloriesKcal: Double get() = items.sumOf { it.totalCaloriesKcal }
}

package com.songshawn.nutritrack.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.songshawn.nutritrack.data.models.MealType
import com.songshawn.nutritrack.data.room.entity.MealLogEntity
import com.songshawn.nutritrack.data.room.entity.MealLogItemEntity
import com.songshawn.nutritrack.data.room.entity.MealWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {
    @Transaction
    @Query("SELECT * FROM meal_logs WHERE userId = :userId AND date = :date ORDER BY mealType ASC, id ASC")
    fun getMealsWithItemsForDateFlow(userId: String, date: String): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_logs WHERE userId = :userId AND date = :date ORDER BY mealType ASC, id ASC")
    suspend fun getMealsWithItemsForDate(userId: String, date: String): List<MealWithItems>

    @Query("SELECT * FROM meal_logs WHERE userId = :userId AND date = :date AND mealType = :mealType LIMIT 1")
    suspend fun getMealLogSlot(userId: String, date: String, mealType: MealType): MealLogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMealLog(mealLog: MealLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLogItem(item: MealLogItemEntity): Long

    @Query("UPDATE meal_log_items SET servings = :servings WHERE id = :itemId")
    suspend fun updateMealLogItemServings(itemId: Long, servings: Double)

    @Query("DELETE FROM meal_log_items WHERE id = :itemId")
    suspend fun deleteMealLogItem(itemId: Long)

    @Query("DELETE FROM meal_logs WHERE userId = :userId")
    suspend fun deleteAllMealsForUser(userId: String)

    @Transaction
    suspend fun addFoodToMeal(
        userId: String,
        date: String,
        mealType: MealType,
        foodId: String,
        servings: Double,
    ): Long {
        val existingSlot = getMealLogSlot(userId, date, mealType)
        val mealLogId = existingSlot?.id ?: insertMealLog(
            MealLogEntity(
                userId = userId,
                date = date,
                mealType = mealType,
            ),
        )
        return insertMealLogItem(
            MealLogItemEntity(
                mealLogId = mealLogId,
                foodId = foodId,
                servings = servings,
            ),
        )
    }
}

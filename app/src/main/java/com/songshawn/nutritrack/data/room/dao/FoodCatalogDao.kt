package com.songshawn.nutritrack.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.songshawn.nutritrack.data.models.HeifaFoodGroup
import com.songshawn.nutritrack.data.room.entity.FoodItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodCatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoodItemEntity>)

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun count(): Int

    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun getAllFoodsFlow(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items ORDER BY name ASC")
    suspend fun getAllFoods(): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE category = :category ORDER BY name ASC")
    fun getFoodsByCategoryFlow(category: HeifaFoodGroup): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun getFoodById(id: String): FoodItemEntity?

    /**
     * Full-text prefix search via the `food_items_fts` FTS4 virtual table.
     */
    @Query(
        """
        SELECT food_items.*
        FROM food_items
        JOIN food_items_fts ON food_items.rowid = food_items_fts.rowid
        WHERE food_items_fts MATCH :ftsQuery
        ORDER BY food_items.name ASC
        """,
    )
    suspend fun searchFoodsFts(ftsQuery: String): List<FoodItemEntity>

    @Query("INSERT INTO food_items_fts(food_items_fts) VALUES('rebuild')")
    suspend fun rebuildFtsIndex()
}

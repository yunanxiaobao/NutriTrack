package com.haoshuang_34517812.nutritrack.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.haoshuang_34517812.nutritrack.data.room.entity.FruitCacheEntity

/**
 * Data Access Object for fruit cache operations.
 * Supports cache-first strategy for Fruityvice API calls.
 */
@Dao
interface FruitCacheDao {

    /**
     * Gets a cached fruit by name (case-insensitive lookup)
     * @param name The fruit name to search for (should be lowercase)
     */
    @Query("SELECT * FROM fruit_cache WHERE name = :name LIMIT 1")
    suspend fun getFruitByName(name: String): FruitCacheEntity?

    /**
     * Inserts or updates a fruit cache entry
     * Uses REPLACE strategy to update existing entries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFruit(fruit: FruitCacheEntity)

    /**
     * Deletes expired cache entries
     * @param expirationTime Entries older than this timestamp will be deleted
     */
    @Query("DELETE FROM fruit_cache WHERE cachedAt < :expirationTime")
    suspend fun deleteExpiredEntries(expirationTime: Long)

    /**
     * Gets all cached fruits (useful for debugging or showing search history)
     */
    @Query("SELECT * FROM fruit_cache ORDER BY cachedAt DESC")
    suspend fun getAllCachedFruits(): List<FruitCacheEntity>

    /**
     * Deletes all cached fruits
     */
    @Query("DELETE FROM fruit_cache")
    suspend fun clearCache()

    /**
     * Gets the count of cached fruits
     */
    @Query("SELECT COUNT(*) FROM fruit_cache")
    suspend fun getCacheCount(): Int
}

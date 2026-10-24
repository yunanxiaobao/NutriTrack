package com.haoshuang_34517812.nutritrack.data.repository

import android.util.Log
import com.haoshuang_34517812.nutritrack.data.network.FruityviceApi
import com.haoshuang_34517812.nutritrack.data.network.fruityvice.FruitDto
import com.haoshuang_34517812.nutritrack.data.room.dao.FruitCacheDao
import com.haoshuang_34517812.nutritrack.data.room.entity.FruitCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository for fruit data with cache-first strategy.
 * 
 * Flow:
 * 1. Check Room cache for the fruit
 * 2. If cached and not expired, return cached data
 * 3. If not cached or expired, fetch from network
 * 4. On successful network response, cache the result
 * 5. Return the data
 */
class FruityviceRepository @Inject constructor(
    private val apiService: FruityviceApi,
    private val fruitCacheDao: FruitCacheDao
) {
    companion object {
        private const val TAG = "FruityviceRepository"
    }

    /**
     * Gets fruit information using cache-first strategy.
     * 
     * @param name The name of the fruit to search for
     * @return CacheResult containing the fruit data and source information
     */
    suspend fun getFruit(name: String): CacheResult<FruitDto> = withContext(Dispatchers.IO) {
        val normalizedName = name.trim().lowercase()
        
        // Step 1: Check cache first
        val cachedFruit = fruitCacheDao.getFruitByName(normalizedName)
        
        if (cachedFruit != null && !cachedFruit.isExpired()) {
            Log.d(TAG, "Cache HIT for fruit: $normalizedName")
            return@withContext CacheResult.Success(
                data = cachedFruit.toFruitDto(),
                source = DataSource.CACHE
            )
        }
        
        if (cachedFruit != null) {
            Log.d(TAG, "Cache EXPIRED for fruit: $normalizedName, fetching from network")
        } else {
            Log.d(TAG, "Cache MISS for fruit: $normalizedName, fetching from network")
        }
        
        // Step 2: Fetch from network
        try {
            val response = apiService.getFruitByName(name)
            
            if (response.isSuccessful && response.body() != null) {
                val fruitDto = response.body()!!
                
                // Step 3: Cache the result
                val cacheEntity = FruitCacheEntity.fromFruitDto(fruitDto)
                fruitCacheDao.insertFruit(cacheEntity)
                Log.d(TAG, "Cached fruit: ${fruitDto.name}")
                
                return@withContext CacheResult.Success(
                    data = fruitDto,
                    source = DataSource.NETWORK
                )
            } else {
                // Network request failed, try to use expired cache as fallback
                if (cachedFruit != null) {
                    Log.w(TAG, "Network failed, using expired cache for: $normalizedName")
                    return@withContext CacheResult.Success(
                        data = cachedFruit.toFruitDto(),
                        source = DataSource.CACHE_EXPIRED
                    )
                }
                
                return@withContext CacheResult.Error(
                    code = response.code(),
                    message = response.message() ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error for fruit: $normalizedName", e)
            
            // Network error, try to use expired cache as fallback
            if (cachedFruit != null) {
                Log.w(TAG, "Network error, using expired cache for: $normalizedName")
                return@withContext CacheResult.Success(
                    data = cachedFruit.toFruitDto(),
                    source = DataSource.CACHE_EXPIRED
                )
            }
            
            return@withContext CacheResult.NetworkError(e)
        }
    }

    /**
     * Gets all cached fruits (for showing search history or suggestions)
     */
    suspend fun getCachedFruits(): List<FruitDto> = withContext(Dispatchers.IO) {
        fruitCacheDao.getAllCachedFruits().map { it.toFruitDto() }
    }

    /**
     * Clears expired cache entries
     */
    suspend fun cleanupExpiredCache() = withContext(Dispatchers.IO) {
        val expirationTime = System.currentTimeMillis() - FruitCacheEntity.CACHE_EXPIRATION_MS
        fruitCacheDao.deleteExpiredEntries(expirationTime)
        Log.d(TAG, "Cleaned up expired cache entries")
    }

    /**
     * Clears all cached fruits
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        fruitCacheDao.clearCache()
        Log.d(TAG, "Cleared all fruit cache")
    }

    /**
     * Gets the number of cached fruits
     */
    suspend fun getCacheCount(): Int = withContext(Dispatchers.IO) {
        fruitCacheDao.getCacheCount()
    }
}

/**
 * Represents the result of a cache-aware data fetch operation
 */
sealed class CacheResult<out T> {
    data class Success<T>(
        val data: T,
        val source: DataSource
    ) : CacheResult<T>()
    
    data class Error(
        val code: Int,
        val message: String
    ) : CacheResult<Nothing>()
    
    data class NetworkError(
        val exception: Exception
    ) : CacheResult<Nothing>()
}

/**
 * Indicates where the data came from
 */
enum class DataSource {
    CACHE,          // Fresh cache hit
    CACHE_EXPIRED,  // Expired cache used as fallback
    NETWORK         // Fetched from network
}
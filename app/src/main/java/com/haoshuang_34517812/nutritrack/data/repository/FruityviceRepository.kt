package com.haoshuang_34517812.nutritrack.data.repository

import android.util.Log
import com.haoshuang_34517812.nutritrack.data.network.FruityviceApi
import com.haoshuang_34517812.nutritrack.data.network.fruityvice.FruitDto
import com.haoshuang_34517812.nutritrack.data.room.dao.FruitCacheDao
import com.haoshuang_34517812.nutritrack.data.room.entity.FruitCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository for fruit data with Stale-While-Revalidate strategy.
 * 
 * This strategy provides the best user experience:
 * 1. Return cached data immediately (even if stale) for fast response
 * 2. Fetch fresh data from network in background
 * 3. Emit updated data when network response arrives
 * 4. Update cache for future requests
 * 
 * Benefits:
 * - Fast initial response (from cache)
 * - Always eventually consistent with server
 * - Works offline with cached data
 * - No permanently stale data
 */
class FruityviceRepository @Inject constructor(
    private val apiService: FruityviceApi,
    private val fruitCacheDao: FruitCacheDao
) {
    companion object {
        private const val TAG = "FruityviceRepository"
    }

    /**
     * Gets fruit information using Stale-While-Revalidate strategy.
     * 
     * Emits up to 2 values:
     * 1. Cached data (if available) - immediate response
     * 2. Fresh network data (if different from cache) - background update
     * 
     * @param name The name of the fruit to search for
     * @return Flow of FruitResult that may emit cached then fresh data
     */
    fun getFruitFlow(name: String): Flow<FruitResult> = flow {
        val normalizedName = name.trim().lowercase()
        
        // Step 1: Check cache and emit immediately if available
        val cachedFruit = fruitCacheDao.getFruitByName(normalizedName)
        
        if (cachedFruit != null) {
            Log.d(TAG, "Emitting cached data for: $normalizedName (stale=${cachedFruit.isExpired()})")
            emit(FruitResult.Success(
                data = cachedFruit.toFruitDto(),
                source = if (cachedFruit.isExpired()) DataSource.CACHE_STALE else DataSource.CACHE_FRESH,
                isRevalidating = true  // Indicate we're fetching fresh data
            ))
        }
        
        // Step 2: Always fetch from network to revalidate (unless cache is fresh)
        val shouldRevalidate = cachedFruit == null || cachedFruit.isExpired()
        
        if (shouldRevalidate || cachedFruit == null) {
            try {
                Log.d(TAG, "Fetching fresh data for: $normalizedName")
                val response = apiService.getFruitByName(name)
                
                if (response.isSuccessful && response.body() != null) {
                    val fruitDto = response.body()!!
                    
                    // Update cache
                    val cacheEntity = FruitCacheEntity.fromFruitDto(fruitDto)
                    fruitCacheDao.insertFruit(cacheEntity)
                    Log.d(TAG, "Updated cache for: ${fruitDto.name}")
                    
                    // Emit fresh data (only if different from cache or no cache existed)
                    emit(FruitResult.Success(
                        data = fruitDto,
                        source = DataSource.NETWORK,
                        isRevalidating = false
                    ))
                } else {
                    // Network failed, but we already emitted cache if available
                    if (cachedFruit == null) {
                        emit(FruitResult.Error(
                            code = response.code(),
                            message = response.message() ?: "Fruit not found"
                        ))
                    } else {
                        // Mark revalidation complete (cache was already emitted)
                        emit(FruitResult.Success(
                            data = cachedFruit.toFruitDto(),
                            source = DataSource.CACHE_STALE,
                            isRevalidating = false,
                            revalidationFailed = true
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error for: $normalizedName", e)
                
                if (cachedFruit == null) {
                    emit(FruitResult.NetworkError(e))
                } else {
                    // Mark revalidation complete with failure
                    emit(FruitResult.Success(
                        data = cachedFruit.toFruitDto(),
                        source = DataSource.CACHE_STALE,
                        isRevalidating = false,
                        revalidationFailed = true
                    ))
                }
            }
        } else {
            // Cache is fresh, mark as complete
            emit(FruitResult.Success(
                data = cachedFruit!!.toFruitDto(),
                source = DataSource.CACHE_FRESH,
                isRevalidating = false
            ))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Simple single-shot API for cases where Flow is overkill.
     * Uses cache-first with network fallback.
     */
    suspend fun getFruit(name: String): FruitResult = withContext(Dispatchers.IO) {
        val normalizedName = name.trim().lowercase()
        val cachedFruit = fruitCacheDao.getFruitByName(normalizedName)
        
        // Return fresh cache immediately
        if (cachedFruit != null && !cachedFruit.isExpired()) {
            return@withContext FruitResult.Success(
                data = cachedFruit.toFruitDto(),
                source = DataSource.CACHE_FRESH,
                isRevalidating = false
            )
        }
        
        // Fetch from network
        try {
            val response = apiService.getFruitByName(name)
            
            if (response.isSuccessful && response.body() != null) {
                val fruitDto = response.body()!!
                fruitCacheDao.insertFruit(FruitCacheEntity.fromFruitDto(fruitDto))
                
                return@withContext FruitResult.Success(
                    data = fruitDto,
                    source = DataSource.NETWORK,
                    isRevalidating = false
                )
            } else {
                // Use stale cache as fallback
                if (cachedFruit != null) {
                    return@withContext FruitResult.Success(
                        data = cachedFruit.toFruitDto(),
                        source = DataSource.CACHE_STALE,
                        isRevalidating = false,
                        revalidationFailed = true
                    )
                }
                return@withContext FruitResult.Error(response.code(), response.message() ?: "Unknown error")
            }
        } catch (e: Exception) {
            if (cachedFruit != null) {
                return@withContext FruitResult.Success(
                    data = cachedFruit.toFruitDto(),
                    source = DataSource.CACHE_STALE,
                    isRevalidating = false,
                    revalidationFailed = true
                )
            }
            return@withContext FruitResult.NetworkError(e)
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
 * Represents the result of a fruit data fetch operation.
 * Supports Stale-While-Revalidate pattern with revalidation status.
 */
sealed class FruitResult {
    data class Success(
        val data: FruitDto,
        val source: DataSource,
        val isRevalidating: Boolean,      // True if background refresh is in progress
        val revalidationFailed: Boolean = false  // True if network refresh failed
    ) : FruitResult()
    
    data class Error(
        val code: Int,
        val message: String
    ) : FruitResult()
    
    data class NetworkError(
        val exception: Exception
    ) : FruitResult()
}

/**
 * Indicates where the data came from
 */
enum class DataSource {
    CACHE_FRESH,    // Cache hit, data is fresh
    CACHE_STALE,    // Cache hit, but data may be outdated
    NETWORK         // Fetched from network (most up-to-date)
}
package com.haoshuang_34517812.nutritrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshuang_34517812.nutritrack.data.repository.CacheResult
import com.haoshuang_34517812.nutritrack.data.repository.DataSource
import com.haoshuang_34517812.nutritrack.data.repository.FruityviceRepository
import com.haoshuang_34517812.nutritrack.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the NutriCoach screen handling fruit API data and patient information.
 * Implements cache-first strategy for fruit data to reduce network requests.
 */
@HiltViewModel
class NutriCoachViewModel @Inject constructor(
    private val repository: FruityviceRepository,
    private val patientRepo: PatientRepository
) : ViewModel() {

    // UI state for fruit data
    private val _fruitUiState = MutableStateFlow<FruitUiState>(FruitUiState.Initial)
    val fruitUiState: StateFlow<FruitUiState> = _fruitUiState.asStateFlow()

    // Current fruit search query
    private val _fruitQuery = MutableStateFlow("")
    val fruitQuery: StateFlow<String> = _fruitQuery

    // Indicates data source for the last successful fetch
    private val _dataSource = MutableStateFlow<DataSource?>(null)
    val dataSource: StateFlow<DataSource?> = _dataSource.asStateFlow()

    init {
        // Clean up expired cache entries on ViewModel creation
        viewModelScope.launch {
            repository.cleanupExpiredCache()
        }
    }

    /**
     * Updates the current fruit search query
     * @param query The new search query
     */
    fun updateQuery(query: String) {
        _fruitQuery.value = query
    }

    /**
     * Searches for fruit information using cache-first strategy.
     * 1. First checks Room cache for the fruit
     * 2. If cached and not expired, returns cached data immediately
     * 3. If not cached or expired, fetches from network
     * 4. Caches successful network responses for future use
     */
    fun searchFruit() {
        viewModelScope.launch {
            _fruitUiState.value = FruitUiState.Loading
            _dataSource.value = null

            when (val result = repository.getFruit(_fruitQuery.value)) {
                is CacheResult.Success -> {
                    _fruitUiState.value = FruitUiState.Success(result.data)
                    _dataSource.value = result.source
                }
                is CacheResult.Error -> {
                    _fruitUiState.value = FruitUiState.Error(
                        "HTTP ${result.code} - ${result.message}"
                    )
                }
                is CacheResult.NetworkError -> {
                    _fruitUiState.value = FruitUiState.Error(
                        result.exception.localizedMessage ?: "Network error"
                    )
                }
            }
        }
    }

    /**
     * Gets the fruit score for a specific user
     * @param userId ID of the user
     * @return Flow of the user's fruit score
     */
    fun getFruitScore(userId: String): Flow<Double> {
        return patientRepo.getPatientFlow(userId).map { it?.fruitScore ?: 0.0 }
    }

    /**
     * Gets the fruit variations score for a specific user
     * @param userId ID of the user
     * @return Flow of the user's fruit variations score
     */
    fun getFruitVariationsScore(userId: String): Flow<Double> {
        return patientRepo.getPatientFlow(userId).map { it?.fruitVariantionsScore ?: 0.0 }
    }

    /**
     * Gets the fruit serve size for a specific user
     * @param userId ID of the user
     * @return Flow of the user's fruit serve size
     */
    fun getFruitServeSize(userId: String): Flow<Double> {
        return patientRepo.getPatientFlow(userId).map { it?.fruitServeSize ?: 0.0 }
    }

    /**
     * Checks if the user's fruit metrics are below optimal thresholds
     * @param userId ID of the user
     * @return Flow of boolean indicating if fruit metrics are below optimal (true if improvement needed)
     */
    fun checkIfFruitScoreNeedsImprovement(userId: String): Flow<Boolean> {
        return combine(
            getFruitScore(userId),
            getFruitVariationsScore(userId),
            getFruitServeSize(userId)
        ) { fruitScore, fruitVariationsScore, fruitServeSize ->
            fruitScore < 5.0 && fruitVariationsScore < 2.5 && fruitServeSize < 1.0
        }
    }

    /**
     * Resets the fruit search state
     */
    fun resetState() {
        _fruitUiState.value = FruitUiState.Initial
        _dataSource.value = null
    }

    /**
     * Gets the count of cached fruits
     */
    suspend fun getCacheCount(): Int = repository.getCacheCount()
}
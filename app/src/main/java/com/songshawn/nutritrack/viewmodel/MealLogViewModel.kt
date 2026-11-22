package com.songshawn.nutritrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songshawn.nutritrack.data.models.HeifaFoodGroup
import com.songshawn.nutritrack.data.models.MealType
import com.songshawn.nutritrack.data.repository.MealLoggingRepository
import com.songshawn.nutritrack.data.room.entity.DailyScoreEntity
import com.songshawn.nutritrack.data.room.entity.FoodItemEntity
import com.songshawn.nutritrack.data.room.entity.MealWithItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MealLogUiState(
    val userId: String = "",
    val date: String = MealLoggingRepository.todayIsoDate(),
    val selectedMealType: MealType = MealType.BREAKFAST,
    val searchQuery: String = "",
    val selectedCategory: HeifaFoodGroup? = null,
    val catalogResults: List<FoodItemEntity> = emptyList(),
    val mealsForDay: List<MealWithItems> = emptyList(),
    val dailyScore: DailyScoreEntity? = null,
    val isSearching: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealLogViewModel @Inject constructor(
    private val mealLoggingRepository: MealLoggingRepository,
) : ViewModel() {

    private val userIdFlow = MutableStateFlow("")
    private val dateFlow = MutableStateFlow(MealLoggingRepository.todayIsoDate())
    private val selectedMealTypeFlow = MutableStateFlow(MealType.BREAKFAST)
    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow<HeifaFoodGroup?>(null)

    private val searchResultsFlow = combine(
        searchQueryFlow.debounce(150L),
        selectedCategoryFlow,
    ) { query, category -> query to category }
        .flatMapLatest { (query, category) ->
            flow {
                emit(mealLoggingRepository.searchFoods(query, category))
            }
        }

    private val dayMealsFlow = combine(userIdFlow, dateFlow) { uid, date -> uid to date }
        .flatMapLatest { (uid, date) ->
            if (uid.isBlank()) {
                flow { emit(emptyList()) }
            } else {
                mealLoggingRepository.getMealsForDateFlow(uid, date)
            }
        }

    private val dayScoreFlow = combine(userIdFlow, dateFlow) { uid, date -> uid to date }
        .flatMapLatest { (uid, date) ->
            if (uid.isBlank()) {
                flow { emit(null) }
            } else {
                mealLoggingRepository.getDailyScoreFlow(uid, date)
            }
        }

    val uiState: StateFlow<MealLogUiState> = combine(
        userIdFlow,
        dateFlow,
        selectedMealTypeFlow,
        searchQueryFlow,
        selectedCategoryFlow,
        searchResultsFlow,
        dayMealsFlow,
        dayScoreFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        MealLogUiState(
            userId = values[0] as String,
            date = values[1] as String,
            selectedMealType = values[2] as MealType,
            searchQuery = values[3] as String,
            selectedCategory = values[4] as HeifaFoodGroup?,
            catalogResults = values[5] as List<FoodItemEntity>,
            mealsForDay = values[6] as List<MealWithItems>,
            dailyScore = values[7] as DailyScoreEntity?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MealLogUiState(),
    )

    fun bindUser(userId: String) {
        if (userIdFlow.value != userId) {
            userIdFlow.value = userId
        }
        viewModelScope.launch {
            mealLoggingRepository.ensureCatalogSeeded()
        }
    }

    fun selectMealType(mealType: MealType) {
        selectedMealTypeFlow.value = mealType
    }

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun selectCategory(category: HeifaFoodGroup?) {
        selectedCategoryFlow.value = if (selectedCategoryFlow.value == category) null else category
    }

    fun shiftDateByDays(deltaDays: Long) {
        val current = runCatching { LocalDate.parse(dateFlow.value) }.getOrDefault(LocalDate.now())
        val next = current.plusDays(deltaDays)
        if (!next.isAfter(LocalDate.now())) {
            dateFlow.value = next.toString()
        }
    }

    fun addFoodToSelectedMeal(foodId: String, servings: Double = 1.0) {
        val uid = userIdFlow.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            mealLoggingRepository.logFood(
                userId = uid,
                date = dateFlow.value,
                mealType = selectedMealTypeFlow.value,
                foodId = foodId,
                servings = servings,
            )
        }
    }

    fun adjustItemServings(itemId: Long, newServings: Double) {
        val uid = userIdFlow.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            mealLoggingRepository.updateItemServings(
                userId = uid,
                date = dateFlow.value,
                itemId = itemId,
                servings = newServings,
            )
        }
    }

    fun removeItem(itemId: Long) {
        val uid = userIdFlow.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            mealLoggingRepository.removeLoggedItem(
                userId = uid,
                date = dateFlow.value,
                itemId = itemId,
            )
        }
    }
}

package com.songshawn.nutritrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songshawn.nutritrack.data.models.FoodCategory
import com.songshawn.nutritrack.data.models.Persona
import com.songshawn.nutritrack.data.repository.QuestionnaireInfoRepository
import com.songshawn.nutritrack.data.room.entity.QuestionnaireInfoEntity
import com.songshawn.nutritrack.util.AuthenticationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Updated ViewModel for the questionnaire screens with HorizontalPager
 * Manages the multi-step questionnaire flow and data collection process
 */
@HiltViewModel
class QuestionnaireViewModel @Inject constructor(
    private val infoRepo: QuestionnaireInfoRepository
) : ViewModel() {

    // Current step index - Using LiveData for simple UI state
    private val _currentStepIndex = MutableLiveData(0)
    val currentStepIndex: LiveData<Int> = _currentStepIndex

    // Selected food categories - Using StateFlow for collection data
    private val _selectedCategories = MutableStateFlow<List<FoodCategory>>(emptyList())
    val selectedCategories: StateFlow<List<FoodCategory>> = _selectedCategories.asStateFlow()

    // Selected persona - Using LiveData for simple UI state
    private val _selectedPersona = MutableLiveData<Persona?>(null)
    val selectedPersona: LiveData<Persona?> = _selectedPersona

    // Time values - Using LiveData for simple UI state
    private val _mealTime = MutableLiveData("12:00")
    val mealTime: LiveData<String> = _mealTime

    private val _sleepTime = MutableLiveData("22:00")
    val sleepTime: LiveData<String> = _sleepTime

    private val _wakeTime = MutableLiveData("07:00")
    val wakeTime: LiveData<String> = _wakeTime

    // Error messages - Using LiveData for temporary messages
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Sets the current step index when page changes
     * @param index The new step index
     */
    fun setCurrentStep(index: Int) {
        _currentStepIndex.value = index
    }

    /**
     * Validates all steps before final submission
     * @param onValid Callback executed if all validation passes
     */
    fun validateAllSteps(onValid: () -> Unit) {
        // Validate food categories
        if (_selectedCategories.value.isEmpty()) {
            _errorMessage.value = "Please select at least one food category"
            return
        }

        // Validate persona
        if (_selectedPersona.value == null) {
            _errorMessage.value = "Please select a persona"
            return
        }

        // Validate timings
        if (_mealTime.value == _sleepTime.value) {
            _errorMessage.value = "The meal time and sleep time cannot be the same"
            return
        }

        // All validations passed
        _errorMessage.value = null
        onValid()
    }

    /**
     * Loads existing questionnaire data for a user if available
     * @param userId The ID of the user to load data for
     */
    fun loadExisting(userId: String) {
        viewModelScope.launch {
            val existing: QuestionnaireInfoEntity? = infoRepo.getInfoForUser(userId)
            existing?.let {
                _selectedCategories.value = it.selectedCategories
                _selectedPersona.value = it.persona
                _mealTime.value = it.biggestMealTime
                _sleepTime.value = it.sleepTime
                _wakeTime.value = it.wakeTime
            }
        }
    }

    /**
     * Toggles selection status of a food category
     * @param category The category to toggle
     */
    fun toggleCategory(category: FoodCategory) {
        val list = _selectedCategories.value.toMutableList()
        if (list.contains(category)) list.remove(category) else list.add(category)
        _selectedCategories.value = list
    }

    /**
     * Sets the selected persona
     * @param persona The persona to select
     */
    fun setPersona(persona: Persona) {
        _selectedPersona.value = persona
    }

    /**
     * Sets the main meal time
     * @param time The time string in HH:MM format
     */
    fun setMealTime(time: String) {
        _mealTime.value = time
    }

    /**
     * Sets the sleep time
     * @param time The time string in HH:MM format
     */
    fun setSleepTime(time: String) {
        _sleepTime.value = time
    }

    /**
     * Sets the wake time
     * @param time The time string in HH:MM format
     */
    fun setWakeTime(time: String) {
        _wakeTime.value = time
    }

    /**
     * Clears the current error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Saves questionnaire information and marks questionnaire as completed
     * @param patientId The ID of the patient
     * @param onSuccess Callback to execute after successful save
     */
    fun onSaveRequested(patientId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val selectedPersona = _selectedPersona.value
                if (selectedPersona == null) {
                    _errorMessage.value = "Please select a persona before saving"
                    return@launch
                }

                val questionnaireInfo = QuestionnaireInfoEntity(
                    patientId = patientId,
                    biggestMealTime = _mealTime.value ?: "12:00",
                    sleepTime = _sleepTime.value ?: "22:00",
                    wakeTime = _wakeTime.value ?: "07:00",
                    persona = selectedPersona,
                    selectedCategories = _selectedCategories.value
                )

                infoRepo.saveInfo(questionnaireInfo)
                AuthenticationManager.setQuestionnaireCompleted(patientId)
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save questionnaire: ${e.localizedMessage}"
            }
        }
    }
}
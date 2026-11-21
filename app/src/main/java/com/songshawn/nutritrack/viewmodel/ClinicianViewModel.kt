package com.songshawn.nutritrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songshawn.nutritrack.data.models.Gender
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.data.repository.QuestionnaireInfoRepository
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import com.songshawn.nutritrack.data.store.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Clinician screen that displays statistics about patients
 * and handles admin authentication logic.
 */
@HiltViewModel
class ClinicianViewModel @Inject constructor(
    private val repository: PatientRepository,
    private val questionnaireRepository: QuestionnaireInfoRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    // Male users' average score
    private val _maleAverageScore = MutableLiveData(0.0)
    val maleAverageScore: LiveData<Double> = _maleAverageScore

    // Female users' average score
    private val _femaleAverageScore = MutableLiveData(0.0)
    val femaleAverageScore: LiveData<Double> = _femaleAverageScore

    // Count of male users
    private val _maleCount = MutableLiveData(0)
    val maleCount: LiveData<Int> = _maleCount

    // Count of female users
    private val _femaleCount = MutableLiveData(0)
    val femaleCount: LiveData<Int> = _femaleCount

    // Count of registered users
    private val _registeredUserCount = MutableLiveData(0)
    val registeredUserCount: LiveData<Int> = _registeredUserCount

    // List of all patients
    private val _patients = MutableLiveData<List<PatientEntity>>(emptyList())
    val patients: LiveData<List<PatientEntity>> = _patients

    // List of registered patients
    private val _registeredPatients = MutableLiveData<List<PatientEntity>>(emptyList())
    val registeredPatients: LiveData<List<PatientEntity>> = _registeredPatients

    // Loading state
    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    // Admin login related states
    private val _isLoginError = MutableLiveData(false)
    val isLoginError: LiveData<Boolean> = _isLoginError

    private val _loginErrorMessage = MutableLiveData("")
    val loginErrorMessage: LiveData<String> = _loginErrorMessage

    private val _isLoginSuccessful = MutableLiveData(false)
    val isLoginSuccessful: LiveData<Boolean> = _isLoginSuccessful

    init {
        loadPatientStatistics()
    }

    /**
     * Loads all patient statistics including gender distribution,
     * average HEIFA scores, and registered user counts.
     */
    private fun loadPatientStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Retrieve all patients
                val allPatients = repository.getAllPatients().first()

                // Retrieve registered patients
                val registered = repository.getRegisteredPatients().first()

                // Update patient lists
                _patients.value = allPatients
                _registeredPatients.value = registered

                // Update registered user count
                _registeredUserCount.value = repository.getRegisteredCount()

                // Filter male and female users
                val malePatients = allPatients.filter { it.sex == Gender.MALE }
                val femalePatients = allPatients.filter { it.sex == Gender.FEMALE }

                // Update counts
                _maleCount.value = malePatients.size
                _femaleCount.value = femalePatients.size

                // Calculate average scores
                _maleAverageScore.value = calculateAverageScore(malePatients)
                _femaleAverageScore.value = calculateAverageScore(femalePatients)
            } catch (e: Exception) {
                // Handle errors
                _maleAverageScore.value = 0.0
                _femaleAverageScore.value = 0.0
                _maleCount.value = 0
                _femaleCount.value = 0
                _registeredUserCount.value = 0
                _patients.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculates the average HEIFA score for a list of patients.
     *
     * @param patients List of patients
     * @return Average HEIFA score or 0.0 if list is empty
     */
    private fun calculateAverageScore(patients: List<PatientEntity>): Double {
        if (patients.isEmpty()) return 0.0
        return patients.map { it.heifaTotalScore }.average()
    }

    /**
     * Verifies admin password against the stored value in SharedPreferences.
     * Updates login state based on verification result.
     *
     * @param password Input password to verify
     */
    fun verifyAdminPassword(password: String) {
        // 使用 PreferencesManager 验证密码
        if (preferencesManager.verifyAdminPassword(password)) {
            _isLoginSuccessful.value = true
            _isLoginError.value = false
            _loginErrorMessage.value = ""
        } else {
            _isLoginSuccessful.value = false
            _isLoginError.value = true
            _loginErrorMessage.value = "Invalid password. Please try again."
        }
    }

    /**
     * Resets the login state.
     * Used when leaving the login screen or after logout.
     */
    fun resetLoginState() {
        _isLoginSuccessful.value = false
        _isLoginError.value = false
        _loginErrorMessage.value = ""
    }

    /**
     * Gets questionnaire information for a specific user.
     *
     * @param userId User ID to retrieve questionnaire for
     * @return Flow of questionnaire information for the user
     */
    fun getUserQuestionnaireInfo(userId: String) = questionnaireRepository.getInfoForUserFlow(userId)
}
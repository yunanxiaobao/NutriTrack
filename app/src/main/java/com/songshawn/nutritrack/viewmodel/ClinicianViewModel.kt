package com.songshawn.nutritrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songshawn.nutritrack.data.models.Gender
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.data.repository.QuestionnaireInfoRepository
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import com.songshawn.nutritrack.data.store.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Clinician screen exposing SQL cohort statistics via [StateFlow].
 */
@HiltViewModel
class ClinicianViewModel @Inject constructor(
    private val repository: PatientRepository,
    private val questionnaireRepository: QuestionnaireInfoRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _maleAverageScore = MutableStateFlow(0.0)
    val maleAverageScore: StateFlow<Double> = _maleAverageScore.asStateFlow()

    private val _femaleAverageScore = MutableStateFlow(0.0)
    val femaleAverageScore: StateFlow<Double> = _femaleAverageScore.asStateFlow()

    private val _maleCount = MutableStateFlow(0)
    val maleCount: StateFlow<Int> = _maleCount.asStateFlow()

    private val _femaleCount = MutableStateFlow(0)
    val femaleCount: StateFlow<Int> = _femaleCount.asStateFlow()

    private val _registeredUserCount = MutableStateFlow(0)
    val registeredUserCount: StateFlow<Int> = _registeredUserCount.asStateFlow()

    private val _patients = MutableStateFlow<List<PatientEntity>>(emptyList())
    val patients: StateFlow<List<PatientEntity>> = _patients.asStateFlow()

    private val _registeredPatients = MutableStateFlow<List<PatientEntity>>(emptyList())
    val registeredPatients: StateFlow<List<PatientEntity>> = _registeredPatients.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoginError = MutableStateFlow(false)
    val isLoginError: StateFlow<Boolean> = _isLoginError.asStateFlow()

    private val _loginErrorMessage = MutableStateFlow("")
    val loginErrorMessage: StateFlow<String> = _loginErrorMessage.asStateFlow()

    private val _isLoginSuccessful = MutableStateFlow(false)
    val isLoginSuccessful: StateFlow<Boolean> = _isLoginSuccessful.asStateFlow()

    init {
        loadPatientStatistics()
    }

    private fun loadPatientStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allPatients = repository.getAllPatients().first()
                val registered = repository.getRegisteredPatients().first()

                _patients.value = allPatients
                _registeredPatients.value = registered
                _registeredUserCount.value = repository.getRegisteredCount()

                // Compute gender counts and averages directly in SQLite (GROUP BY + AVG)
                val genderSummaries = repository.getGenderScoreSummaries().associateBy { it.sex }
                val maleSummary = genderSummaries[Gender.MALE]
                val femaleSummary = genderSummaries[Gender.FEMALE]

                _maleCount.value = maleSummary?.patientCount ?: 0
                _femaleCount.value = femaleSummary?.patientCount ?: 0
                _maleAverageScore.value = maleSummary?.avgTotalScore ?: 0.0
                _femaleAverageScore.value = femaleSummary?.avgTotalScore ?: 0.0
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
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

    fun verifyAdminPassword(password: String) {
        if (preferencesManager.verifyAdminPassword(password)) {
            _isLoginSuccessful.value = true
            _isLoginError.value = false
            _loginErrorMessage.value = ""
        } else {
            _isLoginSuccessful.value = false
            _isLoginError.value = true
            _loginErrorMessage.value = "Clinician demo mode is disabled in production builds."
        }
    }

    fun resetLoginState() {
        _isLoginSuccessful.value = false
        _isLoginError.value = false
        _loginErrorMessage.value = ""
    }

    fun getUserQuestionnaireInfo(userId: String) = questionnaireRepository.getInfoForUserFlow(userId)
}

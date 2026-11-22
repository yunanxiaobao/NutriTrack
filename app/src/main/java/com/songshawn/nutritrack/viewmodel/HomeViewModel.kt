package com.songshawn.nutritrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songshawn.nutritrack.data.repository.CloudSyncRepository
import com.songshawn.nutritrack.data.repository.MealLoggingRepository
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.util.AuthenticationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * ViewModel for the Home screen displaying user profile and live HEIFA scores.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PatientRepository,
    private val mealLoggingRepository: MealLoggingRepository,
    private val cloudSync: CloudSyncRepository,
) : ViewModel() {

    private val _userId = MutableLiveData<String>()
    val userId: LiveData<String> = _userId

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _totalScore = MutableLiveData(0)
    val totalScore: LiveData<Int> = _totalScore

    private val _hasScores = MutableLiveData(true)
    val hasScores: LiveData<Boolean> = _hasScores

    fun loadPatientData(userId: String) {
        _userId.value = userId
        viewModelScope.launch {
            mealLoggingRepository.ensureCatalogSeeded()
            cloudSync.syncUserData(userId)
            val latestDaily = mealLoggingRepository.getLatestDailyScore(userId)
            val patient = repository.getPatientById(userId)

            val resolvedScore = latestDaily?.heifaTotalScore ?: patient?.heifaTotalScore
            _hasScores.value = resolvedScore != null
            _username.value = patient?.name?.takeIf { it.isNotBlank() }
                ?: AuthenticationManager.getCurrentDisplayName()
                ?: "friend"
            _totalScore.value = (resolvedScore ?: 0.0).roundToInt()
        }
    }
}

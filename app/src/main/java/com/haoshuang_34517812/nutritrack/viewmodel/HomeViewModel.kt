package com.haoshuang_34517812.nutritrack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshuang_34517812.nutritrack.data.repository.CloudSyncRepository
import com.haoshuang_34517812.nutritrack.data.repository.PatientRepository
import com.haoshuang_34517812.nutritrack.util.AuthenticationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import javax.inject.Inject

/**
 * ViewModel for the Home screen displaying user profile and scores
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PatientRepository,
    private val cloudSync: CloudSyncRepository
) : ViewModel() {

    // User ID - Using LiveData for simple state
    private val _userId = MutableLiveData<String>()
    val userId: LiveData<String> = _userId

    // Username - Using LiveData for UI state
    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    // Total score - Using LiveData
    private val _totalScore = MutableLiveData(0)
    val totalScore: LiveData<Int> = _totalScore

    // Whether this user has real HEIFA scores (legacy dataset users) or is a
    // Firebase account without scores yet - the UI shows a guidance state
    // instead of a misleading zero score in the latter case.
    private val _hasScores = MutableLiveData(true)
    val hasScores: LiveData<Boolean> = _hasScores

    /**
     * Loads patient data from repository based on user ID
     * @param userId ID of the patient to load
     */
    fun loadPatientData(userId: String) {
        _userId.value = userId
        viewModelScope.launch {
            // Restore cloud data (questionnaire, tips) for signed-in users.
            cloudSync.syncUserData(userId)
            val patient = repository.getPatientById(userId)
            _hasScores.value = patient != null
            _username.value = patient?.name
                ?: AuthenticationManager.getCurrentDisplayName()
                ?: "friend"
            _totalScore.value = (patient?.heifaTotalScore ?: 0.0).roundToInt()
        }
    }
}
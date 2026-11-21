package com.songshawn.nutritrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ViewModel for accessing patient data from the database
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val patientRepository: PatientRepository
) : ViewModel() {

    /**
     * Gets a reactive flow of patient data for a specific user ID
     * Using Flow instead of LiveData for reactive database monitoring
     *
     * @param userId The ID of the patient to retrieve
     * @return Flow of PatientEntity that updates when data changes
     */
    fun getPatient(userId: String): Flow<PatientEntity?> {
        return patientRepository.getPatientFlow(userId)
    }
}
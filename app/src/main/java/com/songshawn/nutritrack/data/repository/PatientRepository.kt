package com.songshawn.nutritrack.data.repository

import com.songshawn.nutritrack.data.room.dao.PatientDao
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository layer: Encapsulates all operations on PatientDao.
 * Higher layers (ViewModel/UseCase) only interact with this repository,
 * facilitating testing and future extensions.
 */
class PatientRepository @Inject constructor(private val dao: PatientDao) {
    // Get all patients as a Flow
    fun getAllPatients(): Flow<List<PatientEntity>> = dao.getAllPatients()

    // Asynchronously get a single patient by ID
    suspend fun getPatientById(userId: String): PatientEntity? =
        dao.getPatientById(userId)

    // Get a patient as a Flow for reactive updates
    fun getPatientFlow(userId: String): Flow<PatientEntity?> =
        dao.getPatientFlow(userId)

    // Insert or replace a patient
    suspend fun insertPatient(patient: PatientEntity) =
        dao.insertPatient(patient)

    suspend fun insertPatients(patients: List<PatientEntity>) = dao.insertAll(patients)


    // Update patient data
    suspend fun updatePatient(patient: PatientEntity) =
        dao.updatePatient(patient)

    // Delete all patients
    suspend fun deleteAll() =
        dao.deleteAllPatients()

    // Get total count of patient records
    suspend fun getCount(): Int =
        dao.getCount()

    // Get all registered patients (with name and password)
    fun getRegisteredPatients(): Flow<List<PatientEntity>> =
        dao.getRegisteredPatients()

    // Get count of registered users
    suspend fun getRegisteredCount(): Int =
        dao.getRegisteredCount()
}
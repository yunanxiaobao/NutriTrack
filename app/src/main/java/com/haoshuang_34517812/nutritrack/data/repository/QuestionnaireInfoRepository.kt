package com.haoshuang_34517812.nutritrack.data.repository

import com.haoshuang_34517812.nutritrack.data.room.dao.QuestionnaireInfoDao
import com.haoshuang_34517812.nutritrack.data.room.entity.QuestionnaireInfoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QuestionnaireInfoRepository @Inject constructor(
    private val dao: QuestionnaireInfoDao,
    private val cloudSync: CloudSyncRepository
) {
    /**
     * Saves or replaces questionnaire information for a patient
     * @param info The questionnaire information to save
     */
    suspend fun saveInfo(info: QuestionnaireInfoEntity) {
        dao.insertInfo(info)
        // Best-effort mirror to Firestore for signed-in users.
        cloudSync.pushQuestionnaire(info)
    }

    /**
     * Gets questionnaire information for a specific user
     * @param userId The user ID
     * @return The questionnaire information or null if not found
     */
    suspend fun getInfoForUser(userId: String): QuestionnaireInfoEntity? {
        return dao.getInfoByUser(userId)
    }

    /**
     * Gets questionnaire information as a Flow for reactive updates
     * @param userId The user ID
     * @return Flow of questionnaire information that updates when data changes
     */
    fun getInfoForUserFlow(userId: String): Flow<QuestionnaireInfoEntity?> {
        return dao.getInfoByUserFlow(userId)
    }
}
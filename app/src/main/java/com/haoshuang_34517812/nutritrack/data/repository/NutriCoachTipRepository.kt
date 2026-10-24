package com.haoshuang_34517812.nutritrack.data.repository

import com.haoshuang_34517812.nutritrack.data.room.dao.NutriCoachTipDao
import com.haoshuang_34517812.nutritrack.data.room.entity.NutriCoachTipEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class NutriCoachTipRepository @Inject constructor(
    private val tipDao: NutriCoachTipDao,
    private val cloudSync: CloudSyncRepository
) {
    /**
     * Saves a new nutrition tip for a specific user
     * @param userId ID of the user receiving the tip
     * @param content The content of the tip
     * @param prompt The prompt used to generate this tip
     * @return The ID of the newly created tip, or -1 if save failed
     */
    suspend fun saveTip(userId: String, content: String, prompt: String): Long {
        // Skip saving if content is empty
        if (content.isBlank()) {
            return -1
        }

        val newTip = NutriCoachTipEntity(
            userId = userId,
            tipContent = content,
            timestamp = Date(),
            prompt = prompt
        )

        // Save to database, mirror to Firestore, and return the row ID
        val rowId = tipDao.insertTip(newTip)
        if (rowId > 0) {
            cloudSync.pushTip(newTip.copy(id = rowId))
        }
        return rowId
    }

    /**
     * Gets all tips for a specific user as a Flow
     * @param userId The ID of the user
     * @return Flow of tips ordered by timestamp (newest first)
     */
    fun getTipsForUser(userId: String): Flow<List<NutriCoachTipEntity>> {
        return tipDao.getTipsByUserId(userId)
    }

    /**
     * Deletes a specific tip by its ID
     * @param tipId The ID of the tip to delete
     */
    suspend fun deleteTipById(tipId: Long) {
        tipDao.deleteTipById(tipId)
    }

    /**
     * Deletes all tips associated with a specific user
     * @param userId The ID of the user
     */
    suspend fun deleteTipsForUser(userId: String) {
        tipDao.deleteAllTipsForUser(userId)
    }
}
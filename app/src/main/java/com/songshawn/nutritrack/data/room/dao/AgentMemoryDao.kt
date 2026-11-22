package com.songshawn.nutritrack.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.songshawn.nutritrack.data.room.entity.AgentMessageEntity
import com.songshawn.nutritrack.data.room.entity.AgentSessionSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentMemoryDao {
    @Query("SELECT * FROM agent_messages WHERE userId = :userId ORDER BY createdAt ASC, id ASC")
    fun getMessagesFlow(userId: String): Flow<List<AgentMessageEntity>>

    @Query("SELECT * FROM agent_messages WHERE userId = :userId ORDER BY createdAt ASC, id ASC")
    suspend fun getMessages(userId: String): List<AgentMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AgentMessageEntity): Long

    @Query("DELETE FROM agent_messages WHERE id IN (:ids)")
    suspend fun deleteMessagesByIds(ids: List<Long>)

    @Query("DELETE FROM agent_messages WHERE userId = :userId")
    suspend fun clearMessagesForUser(userId: String)

    @Query("SELECT * FROM agent_session_summaries WHERE userId = :userId LIMIT 1")
    fun getSummaryFlow(userId: String): Flow<AgentSessionSummaryEntity?>

    @Query("SELECT * FROM agent_session_summaries WHERE userId = :userId LIMIT 1")
    suspend fun getSummary(userId: String): AgentSessionSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: AgentSessionSummaryEntity)

    @Query("DELETE FROM agent_session_summaries WHERE userId = :userId")
    suspend fun clearSummaryForUser(userId: String)
}

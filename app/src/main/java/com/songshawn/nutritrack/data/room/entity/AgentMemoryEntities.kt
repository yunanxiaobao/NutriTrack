package com.songshawn.nutritrack.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted message in a user's NutriCoach Agent conversation history, including
 * serialized tool-call trace steps ([traceJson]) so reasoning steps survive app restarts.
 */
@Entity(
    tableName = "agent_messages",
    indices = [Index(value = ["userId", "createdAt"])],
)
data class AgentMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    /** "USER" or "ASSISTANT" */
    val role: String,
    val content: String,
    /** JSON-serialized `List<ToolCallTraceStep>` for assistant turns. */
    val traceJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Rolling summary of older conversation turns for a user, automatically updated when
 * the active message window exceeds the threshold so context stays bounded.
 */
@Entity(tableName = "agent_session_summaries")
data class AgentSessionSummaryEntity(
    @PrimaryKey
    val userId: String,
    val summaryText: String,
    val summarizedMessageCount: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)

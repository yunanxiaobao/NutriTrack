package com.songshawn.nutritrack.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.songshawn.nutritrack.data.room.dao.AgentMemoryDao
import com.songshawn.nutritrack.data.room.entity.AgentMessageEntity
import com.songshawn.nutritrack.data.room.entity.AgentSessionSummaryEntity
import com.songshawn.nutritrack.domain.agent.LlmMessage
import com.songshawn.nutritrack.domain.agent.LlmRole
import com.songshawn.nutritrack.domain.agent.ToolCallTraceStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AgentChatMessage(
    val id: Long,
    val role: LlmRole,
    val content: String,
    val traceSteps: List<ToolCallTraceStep>,
    val createdAt: Long,
)

@Singleton
class AgentConversationRepository @Inject constructor(
    private val agentMemoryDao: AgentMemoryDao,
    private val gson: Gson,
) {
    fun getChatMessagesFlow(userId: String): Flow<List<AgentChatMessage>> =
        agentMemoryDao.getMessagesFlow(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun getSessionSummaryFlow(userId: String): Flow<AgentSessionSummaryEntity?> =
        agentMemoryDao.getSummaryFlow(userId)

    suspend fun getRecentLlmHistory(userId: String): Pair<List<LlmMessage>, String?> =
        withContext(Dispatchers.IO) {
            val summary = agentMemoryDao.getSummary(userId)?.summaryText
            val recent = agentMemoryDao.getMessages(userId).takeLast(MAX_CONTEXT_MESSAGES).map { entity ->
                LlmMessage(
                    role = if (entity.role == LlmRole.USER.name) LlmRole.USER else LlmRole.ASSISTANT,
                    content = entity.content,
                )
            }
            recent to summary
        }

    suspend fun appendUserMessage(userId: String, text: String): Long = withContext(Dispatchers.IO) {
        agentMemoryDao.insertMessage(
            AgentMessageEntity(
                userId = userId,
                role = LlmRole.USER.name,
                content = text,
                traceJson = "[]",
            ),
        )
    }

    suspend fun appendAssistantMessage(
        userId: String,
        text: String,
        traceSteps: List<ToolCallTraceStep>,
    ): Long = withContext(Dispatchers.IO) {
        val id = agentMemoryDao.insertMessage(
            AgentMessageEntity(
                userId = userId,
                role = LlmRole.ASSISTANT.name,
                content = text,
                traceJson = gson.toJson(traceSteps),
            ),
        )
        compactHistoryIfNeeded(userId)
        id
    }

    suspend fun clearConversation(userId: String) = withContext(Dispatchers.IO) {
        agentMemoryDao.clearMessagesForUser(userId)
        agentMemoryDao.clearSummaryForUser(userId)
    }

    /**
     * Compresses the oldest turns into [AgentSessionSummaryEntity] when the active message count
     * exceeds [COMPACTION_THRESHOLD], keeping the prompt token window bounded.
     */
    internal suspend fun compactHistoryIfNeeded(
        userId: String,
        threshold: Int = COMPACTION_THRESHOLD,
        batchSize: Int = COMPACTION_BATCH_SIZE,
    ) {
        val allMessages = agentMemoryDao.getMessages(userId)
        if (allMessages.size <= threshold) return

        val toCompact = allMessages.take(batchSize)
        val existingSummary = agentMemoryDao.getSummary(userId)
        val newSnippet = toCompact.joinToString(" | ") { msg ->
            val prefix = if (msg.role == LlmRole.USER.name) "User asked" else "Coach advised"
            "$prefix: ${msg.content.replace("\n", " ").take(90)}"
        }
        val mergedSummary = buildString {
            if (!existingSummary?.summaryText.isNullOrBlank()) {
                append(existingSummary?.summaryText)
                append(" ; ")
            }
            append(newSnippet)
        }.takeLast(MAX_SUMMARY_CHARS)

        agentMemoryDao.upsertSummary(
            AgentSessionSummaryEntity(
                userId = userId,
                summaryText = mergedSummary,
                summarizedMessageCount = (existingSummary?.summarizedMessageCount ?: 0) + toCompact.size,
            ),
        )
        agentMemoryDao.deleteMessagesByIds(toCompact.map { it.id })
    }

    private fun AgentMessageEntity.toDomain(): AgentChatMessage {
        val steps: List<ToolCallTraceStep> = try {
            val type = object : TypeToken<List<ToolCallTraceStep>>() {}.type
            gson.fromJson<List<ToolCallTraceStep>>(traceJson, type).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        return AgentChatMessage(
            id = id,
            role = if (role == LlmRole.USER.name) LlmRole.USER else LlmRole.ASSISTANT,
            content = content,
            traceSteps = steps,
            createdAt = createdAt,
        )
    }

    companion object {
        const val MAX_CONTEXT_MESSAGES = 8
        const val COMPACTION_THRESHOLD = 10
        const val COMPACTION_BATCH_SIZE = 4
        private const val MAX_SUMMARY_CHARS = 800
    }
}

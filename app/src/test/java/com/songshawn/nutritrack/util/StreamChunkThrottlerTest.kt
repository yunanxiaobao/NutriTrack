package com.songshawn.nutritrack.util

import com.songshawn.nutritrack.data.repository.StreamResult
import com.songshawn.nutritrack.data.room.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamChunkThrottlerTest {

    @Test
    fun `throttleChunks reduces 100 rapid token emissions by over 80 percent while preserving full text`() = runBlocking {
        var virtualClockMs = 1_000L
        val rawTokens = (1..100).map { "tok$it " }

        // Simulate 100 tokens arriving every 5ms (total 500ms stream)
        val rapidStream = flow {
            for (token in rawTokens) {
                virtualClockMs += 5L
                emit(StreamResult.Chunk(token))
            }
            emit(StreamResult.Complete)
        }

        val unthrottledEvents = rapidStream.toList()
        val unthrottledChunkCount = unthrottledEvents.filterIsInstance<StreamResult.Chunk>().size
        assertEquals(100, unthrottledChunkCount)

        virtualClockMs = 1_000L
        val throttledEvents = rapidStream
            .throttleChunks(windowMs = 48L, clockMillis = { virtualClockMs })
            .toList()

        val throttledChunks = throttledEvents.filterIsInstance<StreamResult.Chunk>()
        val reconstructedText = throttledChunks.joinToString("") { it.text }
        val expectedText = rawTokens.joinToString("")

        // 1. Zero data loss: final text is identical
        assertEquals(expectedText, reconstructedText)
        assertTrue(throttledEvents.last() is StreamResult.Complete)

        // 2. Quantitative recomposition reduction: 100 emissions -> <= 12 emissions (>= 88% reduction)
        assertTrue(
            "Expected <= 12 throttled UI emissions from 100 rapid chunks, got ${throttledChunks.size}",
            throttledChunks.size <= 12,
        )
    }

    @Test
    fun `deterministicUuid generates idempotent UUIDs for identical logical outbox keys`() {
        val uuid1 = SyncOutboxEntity.deterministicUuid("tip:user_123:1700000000:42")
        val uuid2 = SyncOutboxEntity.deterministicUuid("tip:user_123:1700000000:42")
        val uuidDifferent = SyncOutboxEntity.deterministicUuid("tip:user_123:1700000001:42")

        assertEquals(uuid1, uuid2)
        assertNotEquals(uuid1, uuidDifferent)
    }
}

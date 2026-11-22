package com.songshawn.nutritrack.util

import com.songshawn.nutritrack.data.repository.StreamResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Coalesces rapid SSE [StreamResult.Chunk] emissions within [windowMs] intervals so the UI
 * layer recomposes at a smooth frame cadence (~20 fps at 48ms) instead of on every token.
 *
 * Terminal events ([StreamResult.Complete], [StreamResult.Error]) flush any buffered text
 * immediately before emitting the terminal event.
 */
fun Flow<StreamResult>.throttleChunks(
    windowMs: Long = 48L,
    clockMillis: () -> Long = { System.currentTimeMillis() },
): Flow<StreamResult> = flow {
    if (windowMs <= 0L) {
        collect { emit(it) }
        return@flow
    }

    val buffer = StringBuilder()
    var lastEmitTimeMs = clockMillis()

    collect { event ->
        when (event) {
            is StreamResult.Chunk -> {
                buffer.append(event.text)
                val now = clockMillis()
                if (now - lastEmitTimeMs >= windowMs) {
                    emit(StreamResult.Chunk(buffer.toString()))
                    buffer.clear()
                    lastEmitTimeMs = now
                }
            }

            is StreamResult.Complete -> {
                if (buffer.isNotEmpty()) {
                    emit(StreamResult.Chunk(buffer.toString()))
                    buffer.clear()
                }
                emit(StreamResult.Complete)
            }

            is StreamResult.Error -> {
                if (buffer.isNotEmpty()) {
                    emit(StreamResult.Chunk(buffer.toString()))
                    buffer.clear()
                }
                emit(event)
            }
        }
    }
}
